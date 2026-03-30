package com.marine.management.modules.auth.application;

import com.marine.management.modules.users.infrastructure.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service responsible for the password reset flow.
 *
 * <p>Encapsulates all business logic for:
 * <ul>
 *   <li>Generating and sending one-time reset tokens</li>
 *   <li>Validating tokens and updating passwords</li>
 * </ul>
 *
 * <p>Security considerations:
 * <ul>
 *   <li>Always returns successfully on reset requests (prevents user enumeration)</li>
 *   <li>Tokens are UUID-based, single-use, and expire in 1 hour</li>
 * </ul>
 */
@Service
@Transactional
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
    private static final int MIN_PASSWORD_LENGTH = 8;

    private final UserRepository userRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    public PasswordResetService(
            UserRepository userRepository,
            EmailService emailService,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Initiates a password reset for the given email address.
     *
     * <p>Generates a one-time token, persists it, and sends a reset email.
     * Intentionally does nothing (and does not throw) if the email is not found,
     * to prevent user enumeration attacks.
     *
     * @param email the email address of the account to reset
     */
    public void requestPasswordReset(String email) {
        userRepository.findByEmail(email.trim().toLowerCase()).ifPresent(user -> {
            String token = user.generatePasswordResetToken();
            userRepository.save(user);
            emailService.sendPasswordResetEmail(user.getEmail(), user.getFirstName(), token);
            log.info("Password reset token generated for user: {}", user.getId());
        });
    }

    /**
     * Resets the password for the account associated with the given token.
     *
     * @param token       the one-time reset token
     * @param newPassword the new plain-text password (will be encoded)
     * @throws InvalidPasswordResetTokenException if the token is missing, invalid, or expired
     * @throws IllegalArgumentException           if the password is too short
     */
    public void resetPassword(String token, String newPassword) {
        if (newPassword == null || newPassword.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException(
                    "Password must be at least " + MIN_PASSWORD_LENGTH + " characters"
            );
        }

        var user = userRepository.findByPasswordResetToken(token)
                .filter(u -> u.isPasswordResetTokenValid())
                .orElseThrow(InvalidPasswordResetTokenException::new);

        user.resetPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        log.info("Password successfully reset for user: {}", user.getId());
    }

    // -------------------------------------------------------
    // Exception — kept as inner class to co-locate with service
    // -------------------------------------------------------

    public static class InvalidPasswordResetTokenException extends RuntimeException {
        public InvalidPasswordResetTokenException() {
            super("Reset link is invalid or has expired. Please request a new one.");
        }
    }
}
