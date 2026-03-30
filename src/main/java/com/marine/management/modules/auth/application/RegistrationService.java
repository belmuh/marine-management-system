package com.marine.management.modules.auth.application;

import com.marine.management.modules.auth.application.EmailService;
import com.marine.management.modules.organization.domain.Organization;
import com.marine.management.modules.organization.infrastructure.OrganizationRepository;
import com.marine.management.modules.users.domain.User;
import com.marine.management.modules.users.infrastructure.UserRepository;
import com.marine.management.shared.exceptions.UserRegistrationException;
import com.marine.management.shared.security.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * Handles user registration with email verification.
 *
 * New flow:
 * 1. Register: Creates minimal org + unverified user, sends verification email
 * 2. Verify: Activates user's email via token
 * 3. Resend: Generates new token and resends verification email
 */
@Service
public class RegistrationService {

    private static final Logger logger = LoggerFactory.getLogger(RegistrationService.class);

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public RegistrationService(
            UserRepository userRepository,
            OrganizationRepository organizationRepository,
            PasswordEncoder passwordEncoder,
            EmailService emailService
    ) {
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    /**
     * Register a new user with minimal information.
     * Creates organization with defaults, creates unverified user, sends verification email.
     */
    @Transactional
    public String register(String yachtName, String companyName, String firstName, String lastName,
                           String email, String password, String phoneNumber) {
        Objects.requireNonNull(email, "Email cannot be null");
        Objects.requireNonNull(password, "Password cannot be null");
        Objects.requireNonNull(yachtName, "Yacht name cannot be null");

        logger.info("Starting registration for email: {}", email);

        // Validate uniqueness
        if (userRepository.existsByEmail(email.trim().toLowerCase())) {
            throw UserRegistrationException.emailAlreadyExists(email);
        }

        if (organizationRepository.existsByYachtName(yachtName.trim())) {
            throw new IllegalArgumentException("Yacht name '" + yachtName + "' is already registered");
        }

        // Create minimal organization (defaults: TR, EUR, Europe/Istanbul)
        Organization organization = Organization.createMinimal(yachtName.trim());
        if (companyName != null && !companyName.isBlank()) {
            // Update company name via existing method
            organization.updateDetails(companyName, null, null, null, null);
        }
        organization = organizationRepository.save(organization);

        logger.info("Created organization: {} (id: {})", organization.getYachtName(), organization.getOrganizationId());

        // Create unverified admin user
        String hashedPassword = passwordEncoder.encode(password);
        User user = User.createUnverified(
                email.trim().toLowerCase(),
                firstName,
                lastName,
                hashedPassword,
                Role.ADMIN,
                organization
        );

        user = userRepository.save(user);

        logger.info("Created unverified user: {} (id: {}) for org: {}",
                user.getEmail(), user.getUserId(), organization.getOrganizationId());

        // Send verification email (async, won't block)
        emailService.sendVerificationEmail(user.getEmail(), firstName, user.getVerificationToken());

        return user.getEmail();
    }

    /**
     * Verify user's email using the verification token.
     */
    @Transactional
    public void verifyEmail(String token) {
        Objects.requireNonNull(token, "Verification token cannot be null");

        User user = userRepository.findByVerificationToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid verification token"));

        if (!user.isVerificationTokenValid()) {
            throw new IllegalArgumentException("Verification token has expired. Please request a new one.");
        }

        user.verifyEmail();
        userRepository.save(user);

        logger.info("Email verified for user: {}", user.getEmail());
    }

    /**
     * Resend verification email with a new token.
     */
    @Transactional
    public void resendVerification(String email) {
        Objects.requireNonNull(email, "Email cannot be null");

        User user = userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("No account found with this email"));

        if (user.isEmailVerified()) {
            throw new IllegalArgumentException("Email is already verified");
        }

        String newToken = user.generateVerificationToken();
        userRepository.save(user);

        emailService.sendVerificationEmail(user.getEmail(), user.getFirstName(), newToken);

        logger.info("Resent verification email to: {}", email);
    }
}
