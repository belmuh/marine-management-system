package com.marine.management.shared.bootstrap;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Uygulama başlangıcında demo data yükler.
 *
 * Sadece app.demo.enabled=true olduğunda çalışır.
 * Asıl logic DemoDataService'te — reset endpoint'i de onu kullanır.
 */
@Component
@Profile({"dev", "prod"})
@ConditionalOnProperty(name = "app.demo.enabled", havingValue = "true", matchIfMissing = false)
@Order(400)
public class DemoDataInitializer implements CommandLineRunner {

    private final DemoDataService demoDataService;

    public DemoDataInitializer(DemoDataService demoDataService) {
        this.demoDataService = demoDataService;
    }

    @Override
    public void run(String... args) throws Exception {
        demoDataService.initialize();
    }
}
