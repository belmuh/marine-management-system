package com.marine.management.modules.auth.application;

/**
 * Port for sending transactional emails.
 *
 * Two implementations exist:
 * - SmtpEmailService  : active when app.mail.enabled=true (production)
 * - NoOpEmailService  : active otherwise (dev, test)
 *
 * Callers depend only on this interface — no awareness of mail being enabled/disabled.
 */
public interface EmailService {

    /**
     * Send email verification link to newly registered user.
     * Must be called asynchronously by implementations to avoid blocking the caller.
     */
    void sendVerificationEmail(String toEmail, String firstName, String verificationToken);

    /**
     * Send password reset link to user.
     * Security: always responds the same way to the caller,
     * regardless of whether the email address exists.
     */
    void sendPasswordResetEmail(String toEmail, String firstName, String resetToken);
}
