// modules/auth/application/RegistrationService.java
package com.marine.management.modules.auth.application;

import com.marine.management.modules.auth.presentation.dto.RegisterOrganizationRequest;
import com.marine.management.modules.auth.presentation.dto.RegisterOrganizationResponse;
import com.marine.management.modules.finance.application.TenantReferenceDataInitializer;
import com.marine.management.modules.organization.domain.Organization;
import com.marine.management.modules.organization.infrastructure.OrganizationRepository;
import com.marine.management.modules.users.domain.User;
import com.marine.management.shared.multitenant.TenantContext;
import com.marine.management.shared.security.Role;
import com.marine.management.shared.exceptions.UserRegistrationException;
import com.marine.management.modules.users.infrastructure.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
public class RegistrationService {

    private static final Logger logger = LoggerFactory.getLogger(RegistrationService.class);

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TenantReferenceDataInitializer tenantReferenceDataInitializer;

    public RegistrationService(
            OrganizationRepository organizationRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            TenantReferenceDataInitializer tenantReferenceDataInitializer
    ) {
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tenantReferenceDataInitializer = tenantReferenceDataInitializer;
    }

    /**
     * Register new organization with admin user.
     *
     * CRITICAL FLOW:
     * 1. Create Organization (tenant)
     * 2. Create Admin User
     * 3. Set TenantContext for reference data initialization
     * 4. Initialize tenant-specific reference data (categories, WHO data)
     * 5. Clear TenantContext
     *
     * @param request Registration request with organization and admin details
     * @return Registration response with organization and user IDs
     * @throws UserRegistrationException if email or organization name already exists
     */
    @Transactional
    public RegisterOrganizationResponse registerNewOrganization(RegisterOrganizationRequest request) {
        Objects.requireNonNull(request, "Registration request cannot be null");

        logger.info("Starting registration for organization: {}", request.organizationName());

        // ============================================
        // VALIDATIONS
        // ============================================

        //  Check email uniqueness (global)
        if (userRepository.existsByEmail(request.adminEmail())) {
            throw UserRegistrationException.emailAlreadyExists(request.adminEmail());
        }

        // Check organization name uniqueness
        if (organizationRepository.existsByYachtName(request.organizationName())) {
            throw UserRegistrationException.organizationNameAlreadyExists(request.organizationName());
        }

        // ============================================
        // CREATE ORGANIZATION
        // ============================================

        String flagCountry = request.country() != null && request.country().length() == 2
                ? request.country() : "TR";
        String baseCurrency = "EUR";

        Organization organization = Organization.create(
                request.organizationName(),
                request.organizationName(),  // yachtName = organizationName
                flagCountry,
                baseCurrency
        );
        organization = organizationRepository.save(organization);

        logger.info(" Created organization with ID: {}", organization.getOrganizationId());

        // ============================================
        // CREATE ADMIN USER
        // ============================================

        //  Create user with email (no username field)
        User adminUser = User.createWithHashedPassword(
                request.adminEmail(),
                request.adminFirstName(),
                request.adminLastName(),// email
                passwordEncoder.encode(request.password()), // hashedPassword
                Role.ADMIN,                                // role
                organization                               // organization
        );

        adminUser = userRepository.save(adminUser);

        logger.info(" Created admin user with ID: {} (email: {}) for organization: {}",
                adminUser.getUserId(),
                adminUser.getEmail(),
                organization.getOrganizationId());

        // ============================================
        // INITIALIZE TENANT REFERENCE DATA
        // ============================================

        //  Set tenant context for reference data initialization
        TenantContext.setCurrentTenantId(organization.getOrganizationId());

        try {
            // Initialize tenant-specific reference data (MainCategory, WHO data, etc.)
            tenantReferenceDataInitializer.initializeTenantReferenceData();

            logger.info(" Initialized reference data for tenant: {}", organization.getOrganizationId());

        } finally {
            //  CRITICAL: Always clear tenant context
            TenantContext.clear();
        }

        // ============================================
        // RETURN RESPONSE
        // ============================================

        return new RegisterOrganizationResponse(
                organization.getOrganizationId(),
                organization.getYachtName(),
                adminUser.getUserId(),
                adminUser.getEmail(),
                "Organization registered successfully. Please complete the onboarding setup."
        );
    }
}