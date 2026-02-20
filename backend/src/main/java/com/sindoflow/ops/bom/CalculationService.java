package com.sindoflow.ops.bom;

import com.sindoflow.ops.inventory.ArticleEntity;
import com.sindoflow.ops.inventory.ArticleRepository;
import com.sindoflow.ops.inventory.SupplierArticleRepository;
import com.sindoflow.ops.inventory.SupplierPriceListRepository;
import com.sindoflow.ops.inventory.SupplierArticleEntity;
import com.sindoflow.ops.inventory.SupplierPriceListEntity;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class CalculationService {

    private static final Logger log = LoggerFactory.getLogger(CalculationService.class);
    private static final BigDecimal OVERHEAD_PERCENTAGE = new BigDecimal("0.15"); // 15% overhead

    private final CalculationRepository calculationRepository;
    private final BomVersionRepository bomVersionRepository;
    private final BomItemRepository bomItemRepository;
    private final ProcessPlanRepository processPlanRepository;
    private final ProcessStepRepository processStepRepository;
    private final CostRateRepository costRateRepository;
    private final PartRepository partRepository;
    private final ArticleRepository articleRepository;
    private final SupplierArticleRepository supplierArticleRepository;
    private final SupplierPriceListRepository supplierPriceListRepository;

    public CalculationService(CalculationRepository calculationRepository,
                              BomVersionRepository bomVersionRepository,
                              BomItemRepository bomItemRepository,
                              ProcessPlanRepository processPlanRepository,
                              ProcessStepRepository processStepRepository,
                              CostRateRepository costRateRepository,
                              PartRepository partRepository,
                              ArticleRepository articleRepository,
                              SupplierArticleRepository supplierArticleRepository,
                              SupplierPriceListRepository supplierPriceListRepository) {
        this.calculationRepository = calculationRepository;
        this.bomVersionRepository = bomVersionRepository;
        this.bomItemRepository = bomItemRepository;
        this.processPlanRepository = processPlanRepository;
        this.processStepRepository = processStepRepository;
        this.costRateRepository = costRateRepository;
        this.partRepository = partRepository;
        this.articleRepository = articleRepository;
        this.supplierArticleRepository = supplierArticleRepository;
        this.supplierPriceListRepository = supplierPriceListRepository;
    }

    @Transactional
    public CalculationEntity calculate(UUID partId, UUID bomVersionId, UUID processPlanId,
                                        int quantity, UUID calculatedBy) {
        if (!partRepository.existsById(partId)) {
            throw new EntityNotFoundException("Part not found: " + partId);
        }

        // Calculate material costs from BOM
        BigDecimal materialCost = calculateMaterialCost(bomVersionId, quantity);

        // Calculate labor costs from process steps
        BigDecimal laborCost = calculateLaborCost(processPlanId, quantity);

        // Overhead = configurable percentage of (material + labor)
        BigDecimal overheadCost = materialCost.add(laborCost)
                .multiply(OVERHEAD_PERCENTAGE)
                .setScale(4, RoundingMode.HALF_UP);

        BigDecimal totalCost = materialCost.add(laborCost).add(overheadCost);

        CalculationEntity entity = new CalculationEntity();
        entity.setPartId(partId);
        entity.setBomVersionId(bomVersionId);
        entity.setProcessPlanId(processPlanId);
        entity.setQuantity(quantity);
        entity.setMaterialCost(materialCost);
        entity.setLaborCost(laborCost);
        entity.setOverheadCost(overheadCost);
        entity.setTotalCost(totalCost);
        entity.setCalculatedAt(Instant.now());
        entity.setCalculatedBy(calculatedBy);
        return calculationRepository.save(entity);
    }

    @Transactional(readOnly = true)
    public CalculationEntity getById(UUID id) {
        return calculationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Calculation not found: " + id));
    }

    private BigDecimal calculateMaterialCost(UUID bomVersionId, int quantity) {
        List<BomItemEntity> items = bomItemRepository.findByBomVersionIdOrderByPositionAsc(bomVersionId);
        BigDecimal totalMaterialCost = BigDecimal.ZERO;
        LocalDate today = LocalDate.now();

        for (BomItemEntity item : items) {
            BigDecimal itemQuantity = item.getQuantity().multiply(BigDecimal.valueOf(quantity));

            // Try to find article price via supplier price lists
            // Look up by part number -> article number mapping
            PartEntity componentPart = partRepository.findById(item.getComponentPartId()).orElse(null);
            if (componentPart != null) {
                Optional<ArticleEntity> article = articleRepository.findByArticleNumber(componentPart.getPartNumber());
                if (article.isPresent()) {
                    List<SupplierArticleEntity> supplierArticles =
                            supplierArticleRepository.findByArticleId(article.get().getId());
                    for (SupplierArticleEntity sa : supplierArticles) {
                        Optional<SupplierPriceListEntity> price =
                                supplierPriceListRepository.findCurrentPrice(sa.getId(), today);
                        if (price.isPresent()) {
                            totalMaterialCost = totalMaterialCost.add(
                                    price.get().getPrice().multiply(itemQuantity));
                            break; // Use first available price
                        }
                    }
                }
            }
        }
        return totalMaterialCost.setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateLaborCost(UUID processPlanId, int quantity) {
        List<ProcessStepEntity> steps = processStepRepository
                .findByProcessPlanIdOrderByStepNumberAsc(processPlanId);
        BigDecimal totalLaborCost = BigDecimal.ZERO;
        LocalDate today = LocalDate.now();

        for (ProcessStepEntity step : steps) {
            int totalMinutes = step.getSetupTimeMinutes() + (step.getProcessingTimeMinutes() * quantity);
            BigDecimal hours = BigDecimal.valueOf(totalMinutes)
                    .divide(BigDecimal.valueOf(60), 4, RoundingMode.HALF_UP);

            // Look up labor rate for the station
            if (step.getStationId() != null) {
                Optional<CostRateEntity> laborRate =
                        costRateRepository.findCurrentRate("LABOR", step.getStationId(), today);
                if (laborRate.isPresent()) {
                    totalLaborCost = totalLaborCost.add(hours.multiply(laborRate.get().getRatePerHour()));
                }
            }

            // Add machine cost if applicable
            if (step.getMachineId() != null) {
                Optional<CostRateEntity> machineRate =
                        costRateRepository.findCurrentRate("MACHINE", step.getMachineId(), today);
                if (machineRate.isPresent()) {
                    totalLaborCost = totalLaborCost.add(hours.multiply(machineRate.get().getRatePerHour()));
                }
            }
        }
        return totalLaborCost.setScale(4, RoundingMode.HALF_UP);
    }
}
