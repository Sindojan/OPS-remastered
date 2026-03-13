package com.owlsburg.ops.common;

/**
 * Static utility for running tasks in Virtual Threads with TenantContext propagation.
 * Replaces the manual capture/set/clear pattern used in CeoAgent and other async code.
 */
public final class TenantAwareExecutor {

    private TenantAwareExecutor() {}

    /**
     * Starts a Virtual Thread with the given tenantId set in TenantContext.
     * TenantContext is always cleared in finally to prevent leaks.
     */
    public static Thread runVirtual(String tenantId, Runnable task) {
        return Thread.startVirtualThread(() -> {
            TenantContext.setCurrentTenant(tenantId);
            try {
                task.run();
            } finally {
                TenantContext.clear();
            }
        });
    }
}
