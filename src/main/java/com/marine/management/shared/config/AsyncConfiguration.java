package com.marine.management.shared.config;


import com.marine.management.shared.multitenant.TenantAwareTaskDecorator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configuration for @Async method execution with tenant context propagation.
 *
 * This configuration ensures that when methods annotated with @Async are called,
 * the tenant context from the calling thread is propagated to the new thread.
 *
 * Thread Pool Configuration:
 * - Core pool size: 5 threads
 * - Max pool size: 10 threads
 * - Queue capacity: 25 tasks
 *
 * @see TenantAwareTaskDecorator
 */
@Configuration
@EnableAsync
public class AsyncConfiguration implements AsyncConfigurer {

    private static final Logger log = LoggerFactory.getLogger(AsyncConfiguration.class);

    private static final int CORE_POOL_SIZE = 5;
    private static final int MAX_POOL_SIZE = 10;
    private static final int QUEUE_CAPACITY = 25;

    @Override
    public Executor getAsyncExecutor() {
        log.info("Configuring async executor with tenant-aware task decorator");

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(CORE_POOL_SIZE);
        executor.setMaxPoolSize(MAX_POOL_SIZE);
        executor.setQueueCapacity(QUEUE_CAPACITY);
        executor.setThreadNamePrefix("async-tenant-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);

        // CRITICAL: Set task decorator to propagate tenant context
        executor.setTaskDecorator(new TenantAwareTaskDecorator());

        executor.initialize();

        log.info("Async executor configured: corePoolSize={}, maxPoolSize={}, queueCapacity={}",
                CORE_POOL_SIZE, MAX_POOL_SIZE, QUEUE_CAPACITY);

        return executor;
    }
}