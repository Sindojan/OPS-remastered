package com.owlsburg.ops.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class ModuleService {

    private static final Logger log = LoggerFactory.getLogger(ModuleService.class);

    private final ModuleRepository moduleRepository;
    private final TenantModuleRepository tenantModuleRepository;

    private final ConcurrentHashMap<UUID, Set<String>> cache = new ConcurrentHashMap<>();

    public ModuleService(ModuleRepository moduleRepository,
                         TenantModuleRepository tenantModuleRepository) {
        this.moduleRepository = moduleRepository;
        this.tenantModuleRepository = tenantModuleRepository;
    }

    public Set<String> getEnabledModules(UUID tenantId) {
        return cache.computeIfAbsent(tenantId, this::loadEnabledModules);
    }

    public boolean isModuleEnabled(UUID tenantId, String moduleId) {
        if ("core".equals(moduleId)) {
            return true;
        }
        return getEnabledModules(tenantId).contains(moduleId);
    }

    @Transactional
    public void toggleModule(UUID tenantId, String moduleId, boolean enabled) {
        ModuleEntity module = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new IllegalArgumentException("Unbekanntes Modul: " + moduleId));

        if (module.isCore()) {
            throw new IllegalArgumentException("Kern-Modul kann nicht deaktiviert werden");
        }

        TenantModuleEntity tenantModule = tenantModuleRepository
                .findByTenantIdAndModuleId(tenantId, moduleId)
                .orElseGet(() -> {
                    TenantModuleEntity tm = new TenantModuleEntity();
                    tm.setTenantId(tenantId);
                    tm.setModuleId(moduleId);
                    return tm;
                });

        tenantModule.setEnabled(enabled);
        tenantModule.setEnabledAt(Instant.now());
        tenantModuleRepository.save(tenantModule);

        evictCache(tenantId);
        log.info("Module '{}' {} for tenant {}", moduleId, enabled ? "enabled" : "disabled", tenantId);
    }

    public List<ModuleEntity> getAllModules() {
        return moduleRepository.findAllByOrderByDisplayOrderAsc();
    }

    public List<TenantModuleEntity> getTenantModules(UUID tenantId) {
        return tenantModuleRepository.findByTenantId(tenantId);
    }

    public void evictCache(UUID tenantId) {
        cache.remove(tenantId);
    }

    private Set<String> loadEnabledModules(UUID tenantId) {
        Set<String> enabled = tenantModuleRepository.findByTenantIdAndEnabledTrue(tenantId)
                .stream()
                .map(TenantModuleEntity::getModuleId)
                .collect(Collectors.toSet());
        // core is always enabled
        enabled.add("core");
        return enabled;
    }
}
