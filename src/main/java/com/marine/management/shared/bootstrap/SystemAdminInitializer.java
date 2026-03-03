package com.marine.management.shared.bootstrap;

import com.marine.management.modules.auth.application.RegistrationService;
import com.marine.management.modules.auth.presentation.dto.RegisterOrganizationRequest;
import com.marine.management.modules.auth.presentation.dto.RegisterOrganizationResponse;
import com.marine.management.modules.organization.infrastructure.OrganizationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Configuration
@Order(300)
public class SystemAdminInitializer {

    private static final Logger log = LoggerFactory.getLogger(SystemAdminInitializer.class);

    @Value("${system.admin.username}")
    private String adminUsername;

    @Value("${system.admin.email}")
    private String adminEmail;

    @Value("${system.admin.password}")
    private String adminPassword;

    @Value("${system.organization.code}")
    private String systemOrgCode;

    @Value("${system.organization.country}")
    private String systemOrgCountry;

    @Value("${system.organization.company-name}")
    private String systemOrgName;

    @Bean
    public CommandLineRunner initializeSystemAdmin(
            OrganizationRepository organizationRepository,
            RegistrationService registrationService
    ) {
        return args -> {
            try {
                log.info("🔧 Starting SYSTEM bootstrap...");

                // Check if SYSTEM organization already exists
                if (organizationRepository.existsByYachtName(systemOrgCode)) {
                    log.info("✓ SYSTEM organization already exists, skipping initialization");
                    return;
                }

                // Use RegistrationService - creates org + user + reference data automatically
                RegisterOrganizationRequest request = new RegisterOrganizationRequest(
                        systemOrgCode,          // organizationName (yacht name)
                        adminEmail,             // adminEmail
                        adminPassword,          // password
                        "System",               // adminFirstName
                        "Administrator",        // adminLastName
                        null,                   // phone (optional)
                        systemOrgCountry,       // country
                        null                    // address (optional)
                );

                RegisterOrganizationResponse response = registrationService.registerNewOrganization(request);

                log.info("✅ SYSTEM bootstrap completed successfully");
                log.info("   Organization: {} (ID: {})", systemOrgCode, response.organizationId());
                log.info("   SUPER_ADMIN: {}", response.adminEmail());

            } catch (Exception e) {
                log.error("❌ SYSTEM bootstrap failed", e);
                throw new RuntimeException("Failed to initialize system admin", e);
            }
        };
    }
}