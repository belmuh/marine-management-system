package com.marine.management.modules.organization.application;

import com.marine.management.modules.organization.application.commands.OnboardingResult;
import com.marine.management.modules.organization.application.commands.RegisterYachtCommand;
import com.marine.management.modules.organization.domain.Organization;
import com.marine.management.modules.organization.infrastructure.OrganizationRepository;
import com.marine.management.modules.users.domain.User;
import com.marine.management.modules.users.infrastructure.UserRepository;
import com.marine.management.shared.security.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrganizationOnboardingService {

    private static final Logger logger = LoggerFactory.getLogger(OrganizationOnboardingService.class);

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public OrganizationOnboardingService(
            OrganizationRepository organizationRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Registers a new yacht organization with owner user.
     *
     * CREATES:
     * 1. Organization (tenant)
     * 2. Admin user (yacht owner)
     * 3. Default tenant setup (implicit via data loaders)
     *
     * @param command registration details
     * @return registration result with IDs
     * @throws YachtNameAlreadyExistsException if yacht name taken
     * @throws EmailAlreadyExistsException if email taken
     */
    @Transactional
    public OnboardingResult registerNewYacht(RegisterYachtCommand command) {
        logger.info("Starting yacht registration for: {}", command.yachtName());

        // 1. Validate uniqueness
        validateYachtNameUnique(command.yachtName());
        validateEmailUnique(command.email());

        // 2. Create organization (tenant)
        Organization organization = createOrganization(command);
        organization = organizationRepository.save(organization);

        logger.info("Organization created: id={}, yacht={}",
                organization.getId(), organization.getYachtName());

        // 3. Create owner user
        User owner = createOwnerUser(command, organization);
        owner = userRepository.save(owner);

        logger.info("Owner user created: id={}, email={}",
                owner.getId(), owner.getEmail());

        logger.info("Yacht registration complete for: {}", command.yachtName());

        return new OnboardingResult(
                organization.getId(),
                owner.getId(),
                organization.getYachtName(),
                owner.getEmail()
        );
    }

    // ============================================
    // HELPER METHODS
    // ============================================

    private void validateYachtNameUnique(String yachtName) {
        if (organizationRepository.existsByYachtName(yachtName)) {
            throw new YachtNameAlreadyExistsException(
                    "Yacht name '" + yachtName + "' is already registered"
            );
        }
    }

    private void validateEmailUnique(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException(
                    "Email '" + email + "' is already registered"
            );
        }
    }

    private Organization createOrganization(RegisterYachtCommand command) {
        Organization organization = Organization.create(
                command.yachtName(),
                command.companyName(),
                command.flagCountry(),
                command.baseCurrency(),
                "FREE"  // Initial subscription
        );

        // Set optional yacht details
        if (command.yachtType() != null ||
                command.yachtLength() != null ||
                command.homeMarina() != null) {

            organization.updateDetails(
                    command.companyName(),
                    command.yachtType(),
                    command.yachtLength(),
                    command.homeMarina(),
                    null  // currentLocation
            );
        }

        return organization;
    }

    private User createOwnerUser(RegisterYachtCommand command, Organization organization) {
        String hashedPassword = passwordEncoder.encode(command.password());

        User owner = User.createWithHashedPassword(
                command.username(),
                command.email(),
                hashedPassword,
                Role.ADMIN,  // First user is admin/owner
                organization
        );

        // Set optional name fields
        if (command.firstName() != null && !command.firstName().trim().isEmpty()) {
            owner.setFirstName(command.firstName());
        }
        if (command.lastName() != null && !command.lastName().trim().isEmpty()) {
            owner.setLastName(command.lastName());
        }

        return owner;
    }

    // ============================================
    // CUSTOM EXCEPTIONS
    // ============================================

    public static class YachtNameAlreadyExistsException extends RuntimeException {
        public YachtNameAlreadyExistsException(String message) {
            super(message);
        }
    }

    public static class EmailAlreadyExistsException extends RuntimeException {
        public EmailAlreadyExistsException(String message) {
            super(message);
        }
    }
}