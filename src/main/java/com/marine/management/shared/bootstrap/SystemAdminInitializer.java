package com.marine.management.shared.bootstrap;

import com.marine.management.modules.organization.domain.Organization;
import com.marine.management.modules.organization.infrastructure.OrganizationRepository;
import com.marine.management.modules.users.domain.User;
import com.marine.management.modules.users.infrastructure.UserRepository;
import com.marine.management.shared.security.Role;
import com.marine.management.shared.multitenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@Configuration
@Transactional
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

    @Value("${system.organization.currency}")
    private String systemOrgCurrency;

    @Value("${system.organization.company-name}")
    private String systemOrgName;

    @Bean
    public CommandLineRunner initializeSystemAdmin(
            OrganizationRepository organizationRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        return args -> {
            try {
                log.info("Starting system bootstrap...");

                Organization systemOrg = organizationRepository
                        .findByYachtName(systemOrgCode)
                        .orElseGet(() -> {
                            log.info("Creating SYSTEM organization...");

                            Organization org = Organization.create(
                                    systemOrgCode,
                                    systemOrgName,
                                    systemOrgCountry,
                                    systemOrgCurrency
                            );

                            Organization saved = organizationRepository.save(org);
                            log.info(" SYSTEM organization created: id={}", saved.getOrganizationId());
                            return saved;
                        });

                TenantContext.setCurrentTenantId(systemOrg.getOrganizationId());

                try {
                    if (userRepository.findByEmail(adminUsername).isEmpty()) {
                        log.info("Creating SUPER_ADMIN user...");

                        String hashedPassword = passwordEncoder.encode(adminPassword);

                        User superAdmin = User.createWithHashedPassword(
                                adminEmail,
                                "System",
                                "Administrator",
                                hashedPassword,
                                Role.SUPER_ADMIN,
                                systemOrg
                        );

                        superAdmin.setFirstName("System");
                        superAdmin.setLastName("Administrator");

                        userRepository.save(superAdmin);

                        log.info(" SUPER_ADMIN user created: username={}, email={}",
                                adminUsername, adminEmail);
                    } else {
                        log.info("SUPER_ADMIN user already exists: username={}", adminUsername);
                    }

                } finally {
                    TenantContext.clear();
                    log.debug("Tenant context cleared after bootstrap");
                }

                log.info(" System bootstrap completed successfully");

            } catch (Exception e) {
                log.error("❌ System bootstrap failed", e);
                throw new RuntimeException("Failed to initialize system admin", e);
            }
        };
    }
}