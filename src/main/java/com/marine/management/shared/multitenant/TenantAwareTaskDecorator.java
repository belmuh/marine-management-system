package com.marine.management.shared.multitenant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.TaskDecorator;
import org.springframework.lang.NonNull;

public class TenantAwareTaskDecorator implements TaskDecorator {

    private static final Logger log = LoggerFactory.getLogger(TenantAwareTaskDecorator.class);

    @Override
    @NonNull
    public Runnable decorate(@NonNull Runnable runnable) {
        // Capture tenant ID from parent thread (primitive, safe)
        Long tenantId = captureTenantIdFromParentThread();

        return () -> {
            boolean tenantWasSet = false;

            try {
                if (tenantId != null) {
                    TenantContext.setCurrentTenantId(tenantId);
                    tenantWasSet = true;
                    log.debug("Tenant context propagated to async thread: tenantId={}", tenantId);
                }

                runnable.run();

            } finally {
                if (tenantWasSet) {
                    TenantContext.clear();
                }
            }
        };
    }

    private Long captureTenantIdFromParentThread() {
        if (!TenantContext.hasTenantContext()) {
            return null;
        }
        return TenantContext.getCurrentTenantId();
    }
}