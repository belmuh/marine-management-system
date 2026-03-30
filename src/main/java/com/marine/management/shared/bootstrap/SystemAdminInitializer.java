package com.marine.management.shared.bootstrap;

import com.marine.management.modules.organization.application.OrganizationOnboardingService;
import com.marine.management.modules.organization.application.commands.OnboardingResult;
import com.marine.management.modules.organization.application.commands.RegisterYachtCommand;
import com.marine.management.modules.organization.domain.YachtType;
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
            OrganizationOnboardingService onboardingService
    ) {
        return args -> {
            try {
                log.info("Starting SYSTEM bootstrap...");

                if (organizationRepository.existsByYachtName(systemOrgCode)) {
                    log.info("SYSTEM organization already exists, skipping initialization");
                    return;
                }

                RegisterYachtCommand command = new RegisterYachtCommand(
                        systemOrgCode,          // yachtName
                        YachtType.OTHER,        // yachtType (system org)
                        null,                   // yachtLength
                        systemOrgCountry,       // flagCountry
                        null,                   // homeMarina
                        systemOrgName,          // companyName
                        adminEmail,             // email
                        adminPassword,          // password
                        "System",               // firstName
                        "Administrator",        // lastName
                        null,                   // phoneNumber
                        "EUR",                  // baseCurrency
                        "Europe/Istanbul",      // timezone
                        1,                      // financialYearStartMonth
                        null,                   // approvalLimit
                        false,                  // managerApprovalEnabled
                        null,                   // selectedMainCategoryIds (null = enable all)
                        null                    // selectedWhoIds (null = enable all)
                );

                OnboardingResult result = onboardingService.registerYacht(command);

                log.info("SYSTEM bootstrap completed successfully");
                log.info("   Organization: {} (ID: {})", systemOrgCode, result.organizationId());
                log.info("   Admin: {}", result.email());

            } catch (Exception e) {
                log.error("SYSTEM bootstrap failed", e);
                throw new RuntimeException("Failed to initialize system admin", e);
            }
        };
    }
}
