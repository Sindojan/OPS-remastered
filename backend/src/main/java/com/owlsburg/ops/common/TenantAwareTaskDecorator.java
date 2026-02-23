package com.owlsburg.ops.common;

import org.springframework.core.task.TaskDecorator;

public class TenantAwareTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        String tenantId = TenantContext.getCurrentTenant();
        return () -> {
            try {
                if (tenantId != null) {
                    TenantContext.setCurrentTenant(tenantId);
                }
                runnable.run();
            } finally {
                TenantContext.clear();
            }
        };
    }
}
