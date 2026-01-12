package com.marine.management.shared.multitenant;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Metrics for tenant filter operations.
 *
 * PRODUCTION OBSERVABILITY:
 * - tenant.filter.enable.success: Filter successfully enabled
 * - tenant.filter.enable.failure: Filter failed to enable (CRITICAL ALERT)
 * - tenant.context.missing: Protected endpoint accessed without context
 * - tenant.filter.skip: Public endpoint (no filter needed)
 */
@Component
public class TenantFilterMetrics {

    private final Counter filterEnableSuccess;
    private final Counter filterEnableFailure;
    private final Counter contextMissing;
    private final Counter filterSkip;

    public TenantFilterMetrics(MeterRegistry meterRegistry) {
        this.filterEnableSuccess = Counter.builder("tenant.filter.enable.success")
                .description("Tenant filter successfully enabled")
                .register(meterRegistry);

        this.filterEnableFailure = Counter.builder("tenant.filter.enable.failure")
                .description("CRITICAL: Tenant filter failed to enable")
                .tag("severity", "critical")
                .register(meterRegistry);

        this.contextMissing = Counter.builder("tenant.context.missing")
                .description("Protected endpoint accessed without tenant context")
                .tag("severity", "high")
                .register(meterRegistry);

        this.filterSkip = Counter.builder("tenant.filter.skip")
                .description("Public endpoint (filter skipped)")
                .register(meterRegistry);
    }

    public void recordFilterEnableSuccess() {
        filterEnableSuccess.increment();
    }

    public void recordFilterEnableFailure() {
        filterEnableFailure.increment();
    }

    public void recordContextMissing() {
        contextMissing.increment();
    }

    public void recordFilterSkip() {
        filterSkip.increment();
    }
}