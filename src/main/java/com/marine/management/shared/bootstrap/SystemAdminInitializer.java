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

/**
 * Initializes SYSTEM organization and SUPER_ADMIN user on application startup.
 *
 * CRITICAL BOOTSTRAP CONSIDERATIONS:
 * 1. Runs BEFORE TenantFilter is active
 * 2. Must manually set TenantContext for SYSTEM organization
 * 3. BaseTenantEntity requires tenant context during @PrePersist
 * 4. Idempotent - safe to run multiple times
 *
 * Bootstrap Order:
 * 1. Create SYSTEM organization (using factory method)
 * 2. Set TenantContext to SYSTEM
 * 3. Create SUPER_ADMIN user (TenantEntityListener works)
 * 4. Clear TenantContext
 */
@Configuration
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
    @Transactional
    public CommandLineRunner initializeSystemAdmin(
            OrganizationRepository organizationRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        return args -> {
            try {
                log.info("Starting system bootstrap...");

                // 1. Create or get SYSTEM organization
                Organization systemOrg = organizationRepository
                        .findByYachtName(systemOrgCode)
                        .orElseGet(() -> {
                            log.info("Creating SYSTEM organization...");

                            // ✅ FIXED: Use factory method instead of new Organization()
                            Organization org = Organization.create(
                                    systemOrgCode,        // yachtName (used as code)
                                    systemOrgName,        // companyName
                                    systemOrgCountry,     // flagCountry
                                    systemOrgCurrency,    // baseCurrency
                                    "FREE"                // subscriptionStatus
                            );

                            // Save without tenant_id (SYSTEM has null tenant_id)
                            Organization saved = organizationRepository.save(org);

                            log.info("✅ SYSTEM organization created: id={}", saved.getId());
                            return saved;
                        });

                // 2. Set SYSTEM as current tenant for admin user creation
                // ✅ FIXED: setCurrentTenant(org) → setCurrentTenantId(org.getId())
                TenantContext.setCurrentTenantId(systemOrg.getId());

                try {
                    // 3. Create SUPER_ADMIN user
                    if (userRepository.findByUsername(adminUsername).isEmpty()) {
                        log.info("Creating SUPER_ADMIN user...");

                        String hashedPassword = passwordEncoder.encode(adminPassword);

                        User superAdmin = User.createWithHashedPassword(
                                adminUsername,
                                adminEmail,
                                hashedPassword,
                                Role.SUPER_ADMIN,
                                systemOrg
                        );

                        userRepository.save(superAdmin);

                        log.info("✅ SUPER_ADMIN user created: username={}, email={}",
                                adminUsername, adminEmail);
                    } else {
                        log.info("SUPER_ADMIN user already exists: username={}", adminUsername);
                    }

                } finally {
                    // 4. CRITICAL: Always clear tenant context after bootstrap
                    TenantContext.clear();
                    log.debug("Tenant context cleared after bootstrap");
                }

                log.info("✅ System bootstrap completed successfully");

            } catch (Exception e) {
                log.error("❌ System bootstrap failed", e);
                throw new RuntimeException("Failed to initialize system admin", e);
            }
        };
    }
}