package com.marine.management.shared.bootstrap;

import com.marine.management.modules.organization.application.OrganizationOnboardingService;
import com.marine.management.modules.organization.application.commands.OnboardingResult;
import com.marine.management.modules.organization.application.commands.RegisterYachtCommand;
import com.marine.management.modules.organization.domain.YachtType;
import com.marine.management.modules.organization.infrastructure.OrganizationRepository;
import com.marine.management.modules.users.infrastructure.UserRepository;
import com.marine.management.shared.security.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;

@Configuration
@Order(300)
@Profile("!test")  // Test profilinde çalıştırma — testler kendi verisini yönetir
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
            OrganizationOnboardingService onboardingService,
            UserRepository userRepository
    ) {
        return args -> {
            try {
                log.info("Starting SYSTEM bootstrap...");

                if (adminPassword == null || adminPassword.isBlank()) {
                    throw new IllegalStateException(
                        "SYSTEM_ADMIN_PASSWORD env var is not set.");
                }
                if (adminEmail == null || adminEmail.isBlank()) {
                    throw new IllegalStateException(
                        "SYSTEM_ADMIN_EMAIL env var is not set.");
                }

                if (organizationRepository.existsByYachtName(systemOrgCode)) {
                    // Mevcut kullanıcının rolü SUPER_ADMIN değilse yükselt
                    userRepository.findByEmail(adminEmail).ifPresent(user -> {
                        if (!user.getRoleEnum().isSuperAdmin()) {
                            user.changeRole(Role.SUPER_ADMIN);
                            userRepository.save(user);
                            log.info("SYSTEM admin role SUPER_ADMIN'e yükseltildi: {}", adminEmail);
                        }
                    });
                    log.info("SYSTEM organization already exists, skipping initialization");
                    return;
                }

                RegisterYachtCommand command = new RegisterYachtCommand(
                        systemOrgCode,
                        YachtType.OTHER,
                        null,
                        systemOrgCountry,
                        null,
                        systemOrgName,
                        adminEmail,
                        adminPassword,
                        "System",
                        "Administrator",
                        null,
                        "EUR",
                        "Europe/Istanbul",
                        1,
                        null,
                        false,
                        null,
                        null
                );

                OnboardingResult result = onboardingService.registerYacht(command);

                // Onboarding CAPTAIN rolüyle oluşturur — SUPER_ADMIN'e yükselt
                userRepository.findByEmail(adminEmail).ifPresent(user -> {
                    user.changeRole(Role.SUPER_ADMIN);
                    userRepository.save(user);
                    log.info("SYSTEM admin SUPER_ADMIN rolüne atandı: {}", adminEmail);
                });

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
