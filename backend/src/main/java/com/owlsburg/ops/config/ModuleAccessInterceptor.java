package com.owlsburg.ops.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.owlsburg.ops.common.ApiResponse;
import com.owlsburg.ops.common.ModuleService;
import com.owlsburg.ops.common.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;
import java.util.UUID;

@Component
public class ModuleAccessInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(ModuleAccessInterceptor.class);

    private static final Map<String, String> PATH_TO_MODULE = Map.ofEntries(
            Map.entry("/api/jobs", "production"),
            Map.entry("/api/stations", "production"),
            Map.entry("/api/shifts", "production"),
            Map.entry("/api/quality-checks", "production"),
            Map.entry("/api/machines", "machines"),
            Map.entry("/api/maintenance", "machines"),
            Map.entry("/api/articles", "inventory"),
            Map.entry("/api/stock", "inventory"),
            Map.entry("/api/suppliers", "inventory"),
            Map.entry("/api/employees", "people"),
            Map.entry("/api/time-entries", "people"),
            Map.entry("/api/absences", "people"),
            Map.entry("/api/customers", "customers"),
            Map.entry("/api/conversations", "inbox"),
            Map.entry("/api/parts", "bom"),
            Map.entry("/api/bom", "bom"),
            Map.entry("/api/process-plans", "bom"),
            Map.entry("/api/calculations", "bom"),
            Map.entry("/api/knowledge", "knowledge")
    );

    private final ModuleService moduleService;
    private final ObjectMapper objectMapper;

    public ModuleAccessInterceptor(ModuleService moduleService, ObjectMapper objectMapper) {
        this.moduleService = moduleService;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String tenantId = TenantContext.getCurrentTenant();
        if (tenantId == null) {
            return true; // No tenant context = public endpoint or pre-auth
        }

        String path = request.getRequestURI();
        String moduleId = resolveModule(path);
        if (moduleId == null) {
            return true; // Core path or unmapped
        }

        UUID tenantUuid = UUID.fromString(tenantId);
        if (!moduleService.isModuleEnabled(tenantUuid, moduleId)) {
            log.debug("Blocked access to {} - module '{}' disabled for tenant {}", path, moduleId, tenantId);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            ApiResponse<Void> body = ApiResponse.error("Modul '" + moduleId + "' ist nicht aktiviert");
            response.getWriter().write(objectMapper.writeValueAsString(body));
            return false;
        }

        return true;
    }

    private String resolveModule(String path) {
        for (Map.Entry<String, String> entry : PATH_TO_MODULE.entrySet()) {
            if (path.startsWith(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }
}
