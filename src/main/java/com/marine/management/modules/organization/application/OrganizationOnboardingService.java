package com.marine.management.modules.organization.application;

import com.marine.management.modules.finance.application.TenantReferenceDataInitializer;
import com.marine.management.modules.organization.application.commands.OnboardingResult;
import com.marine.management.modules.organization.application.commands.RegisterYachtCommand;
import com.marine.management.modules.organization.domain.Organization;
import com.marine.management.modules.organization.infrastructure.OrganizationRepository;
import com.marine.management.modules.users.domain.User;
import com.marine.management.modules.users.infrastructure.UserRepository;
import com.marine.management.shared.exceptions.UserRegistrationException;
import com.marine.management.shared.multitenant.TenantContext;
import com.marine.management.shared.security.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * Handles the complete yacht registration onboarding flow.
 *
 * Receives all wizard data in a single API call and creates:
 * 1. Organization (tenant) with yacht details + financial settings
 * 2. Admin user (ADMIN role enforced — not selectable by frontend)
 * 3. Tenant reference data (MainCategory + WHO selections)
 *
 * This is the primary registration service for yacht onboarding.
 */
@Service
public class OrganizationOnboardingService {

    private static final Logger logger = LoggerFactory.getLogger(OrganizationOnboardingService.class);

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TenantReferenceDataInitializer tenantReferenceDataInitializer;

    public OrganizationOnboardingService(
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
     * Complete onboarding registration — single transactional operation.
     *
     * Flow:
     * 1. Validate uniqueness (yacht name, email)
     * 2. Create Organization with all wizard data
     * 3. Create Admin user (ADMIN role enforced)
     * 4. Initialize tenant reference data (categories, WHO)
     *
     * @param command all wizard step data combined
     * @return result with created IDs
     * @throws UserRegistrationException if email already exists
     * @throws YachtNameAlreadyExistsException if yacht name is taken
     */
    @Transactional
    public OnboardingResult registerYacht(RegisterYachtCommand command) {
        Objects.requireNonNull(command, "Registration command cannot be null");

        logger.info("Starting yacht onboarding for: {}", command.yachtName());

        // ── Validations ──

        validateYachtNameUnique(command.yachtName());

        if (userRepository.existsByEmail(command.email())) {
            throw UserRegistrationException.emailAlreadyExists(command.email());
        }

        // ── Create Organization ──

        Organization organization = createOrganization(command);
        organization = organizationRepository.save(organization);

        Long tenantId = organization.getOrganizationId();
        logger.info("Created organization with ID: {}", tenantId);

        // ── Set tenant context for reference data ──

        TenantContext.setCurrentTenantId(tenantId);

        try {
            // ── Create Admin User (ADMIN role enforced) ──

            User adminUser = User.createWithHashedPassword(
                    command.email(),
                    command.firstName(),
                    command.lastName(),
                    passwordEncoder.encode(command.password()),
                    Role.ADMIN,
                    organization
            );

            adminUser = userRepository.save(adminUser);

            logger.info("Created admin user with ID: {} (email: {}) for tenant: {}",
                    adminUser.getUserId(), adminUser.getEmail(), tenantId);

            // ── Initialize Tenant Reference Data (with wizard selections) ──

            tenantReferenceDataInitializer.initializeTenantReferenceData(
                    command.selectedMainCategoryIds(),
                    command.selectedWhoIds()
            );

            logger.info("Onboarding completed for yacht: {} (tenant: {})",
                    command.yachtName(), tenantId);

            return new OnboardingResult(
                    organization.getOrganizationId(),
                    adminUser.getUserId(),
                    organization.getYachtName(),
                    adminUser.getEmail()
            );

        } finally {
            TenantContext.clear();
        }
    }

    // ── Private helpers ──

    private Organization createOrganization(RegisterYachtCommand command) {
        Organization organization = Organization.create(
                command.yachtName(),
                command.companyName(),
                command.flagCountry(),
                command.baseCurrency(),
                command.timezone(),
                command.financialYearStartMonth()
        );

        organization.updateDetails(
                command.companyName(),
                command.yachtType(),
                command.yachtLength(),
                command.homeMarina(),
                null
        );

        // Set approval settings if enabled
        if (Boolean.TRUE.equals(command.managerApprovalEnabled())) {
            organization.enableManagerApproval(command.approvalLimit());
        }

        return organization;
    }

    // ── Exceptions ──

    public static class YachtNameAlreadyExistsException extends RuntimeException {
        public YachtNameAlreadyExistsException(String message) {
            super(message);
        }
    }

    private void validateYachtNameUnique(String yachtName) {
        if (organizationRepository.existsByYachtName(yachtName)) {
            throw new YachtNameAlreadyExistsException(
                    "Yacht name '" + yachtName + "' is already registered"
            );
        }
    }
}
