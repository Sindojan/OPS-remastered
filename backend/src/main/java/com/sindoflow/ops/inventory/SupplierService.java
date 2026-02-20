package com.sindoflow.ops.inventory;

import com.sindoflow.ops.inventory.dto.*;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class SupplierService {

    private static final Logger log = LoggerFactory.getLogger(SupplierService.class);

    private final SupplierRepository supplierRepository;
    private final SupplierArticleRepository supplierArticleRepository;
    private final SupplierPriceListRepository priceListRepository;

    public SupplierService(SupplierRepository supplierRepository,
                           SupplierArticleRepository supplierArticleRepository,
                           SupplierPriceListRepository priceListRepository) {
        this.supplierRepository = supplierRepository;
        this.supplierArticleRepository = supplierArticleRepository;
        this.priceListRepository = priceListRepository;
    }

    // --- Suppliers ---

    @Transactional
    public SupplierEntity createSupplier(CreateSupplierRequest request) {
        SupplierEntity entity = new SupplierEntity();
        entity.setName(request.name());
        entity.setContactName(request.contactName());
        entity.setEmail(request.email());
        entity.setPhone(request.phone());
        entity.setAddress(request.address());
        entity.setTaxId(request.taxId());
        return supplierRepository.save(entity);
    }

    @Transactional(readOnly = true)
    public SupplierEntity getSupplierById(UUID id) {
        return supplierRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Supplier not found: " + id));
    }

    @Transactional(readOnly = true)
    public Page<SupplierEntity> findAllSuppliers(Pageable pageable) {
        return supplierRepository.findAll(pageable);
    }

    @Transactional
    public SupplierEntity updateSupplier(UUID id, UpdateSupplierRequest request) {
        SupplierEntity entity = getSupplierById(id);
        if (request.name() != null) entity.setName(request.name());
        if (request.contactName() != null) entity.setContactName(request.contactName());
        if (request.email() != null) entity.setEmail(request.email());
        if (request.phone() != null) entity.setPhone(request.phone());
        if (request.address() != null) entity.setAddress(request.address());
        if (request.taxId() != null) entity.setTaxId(request.taxId());
        if (request.status() != null) entity.setStatus(request.status());
        return supplierRepository.save(entity);
    }

    @Transactional
    public void deleteSupplier(UUID id) {
        if (!supplierRepository.existsById(id)) {
            throw new EntityNotFoundException("Supplier not found: " + id);
        }
        supplierRepository.deleteById(id);
    }

    // --- Supplier Articles ---

    @Transactional
    public SupplierArticleEntity addSupplierArticle(UUID supplierId, SupplierArticleRequest request) {
        getSupplierById(supplierId); // verify exists
        SupplierArticleEntity entity = new SupplierArticleEntity();
        entity.setSupplierId(supplierId);
        entity.setArticleId(request.articleId());
        entity.setSupplierArticleNumber(request.supplierArticleNumber());
        entity.setLeadTimeDays(request.leadTimeDays());
        entity.setMinOrderQuantity(request.minOrderQuantity());
        return supplierArticleRepository.save(entity);
    }

    @Transactional(readOnly = true)
    public List<SupplierArticleEntity> getSupplierArticles(UUID supplierId) {
        return supplierArticleRepository.findBySupplierId(supplierId);
    }

    // --- Price Lists ---

    @Transactional
    public SupplierPriceListEntity addPriceList(UUID supplierArticleId, PriceListRequest request) {
        if (!supplierArticleRepository.existsById(supplierArticleId)) {
            throw new EntityNotFoundException("Supplier article not found: " + supplierArticleId);
        }
        SupplierPriceListEntity entity = new SupplierPriceListEntity();
        entity.setSupplierArticleId(supplierArticleId);
        entity.setPrice(request.price());
        if (request.currency() != null) entity.setCurrency(request.currency());
        entity.setValidFrom(request.validFrom());
        entity.setValidUntil(request.validUntil());
        return priceListRepository.save(entity);
    }

    @Transactional(readOnly = true)
    public List<SupplierPriceListEntity> getPriceLists(UUID supplierArticleId) {
        return priceListRepository.findBySupplierArticleId(supplierArticleId);
    }
}
