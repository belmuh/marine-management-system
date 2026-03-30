package com.marine.management.modules.auth.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

/**
 * Email service for sending verification and notification emails.
 *
 * When mail is disabled (dev mode), logs the email content to console.
 * Uses async execution to avoid blocking the main thread.
 */
@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromAddress;

    @Value("${app.mail.verify-url}")
    private String verifyBaseUrl;

    @Value("${app.mail.reset-password-url}")
    private String resetPasswordBaseUrl;

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Send email verification link to newly registered user.
     * Async to avoid blocking the registration response.
     */
    @Async
    public void sendVerificationEmail(String toEmail, String firstName, String verificationToken) {
        String verifyUrl = verifyBaseUrl + "?token=" + verificationToken;

        String subject = "Verify your email - Marine Management";
        String htmlContent = buildVerificationEmailHtml(firstName, verifyUrl);

        if (!mailEnabled) {
            logger.info("========================================");
            logger.info("EMAIL VERIFICATION (mail disabled - dev mode)");
            logger.info("To: {}", toEmail);
            logger.info("Subject: {}", subject);
            logger.info("Verify URL: {}", verifyUrl);
            logger.info("Token: {}", verificationToken);
            logger.info("========================================");
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromAddress);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            logger.info("Verification email sent to: {}", toEmail);
        } catch (MessagingException e) {
            logger.error("Failed to send verification email to: {}", toEmail, e);
            // Don't throw - registration should still succeed even if email fails
        }
    }

    /**
     * Send password reset link to user.
     * Async to avoid blocking the request.
     *
     * Security: always logs/sends — caller must not reveal whether the email exists.
     */
    @Async
    public void sendPasswordResetEmail(String toEmail, String firstName, String resetToken) {
        String resetUrl = resetPasswordBaseUrl + "?token=" + resetToken;

        String subject = "Reset your password - Marine Management";
        String htmlContent = buildPasswordResetEmailHtml(firstName, resetUrl);

        if (!mailEnabled) {
            logger.info("========================================");
            logger.info("PASSWORD RESET EMAIL (mail disabled - dev mode)");
            logger.info("To: {}", toEmail);
            logger.info("Subject: {}", subject);
            logger.info("Reset URL: {}", resetUrl);
            logger.info("Token: {}", resetToken);
            logger.info("========================================");
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromAddress);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            logger.info("Password reset email sent to: {}", toEmail);
        } catch (MessagingException e) {
            logger.error("Failed to send password reset email to: {}", toEmail, e);
        }
    }

    private String buildPasswordResetEmailHtml(String firstName, String resetUrl) {
        return """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"></head>
            <body style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
                <div style="text-align: center; padding: 20px 0;">
                    <h1 style="color: #4f46e5; margin: 0;">Marine Management</h1>
                </div>
                <div style="background: #f8fafc; border-radius: 12px; padding: 32px; margin: 20px 0;">
                    <h2 style="color: #1e293b; margin-top: 0;">Password Reset, %s</h2>
                    <p style="color: #475569; font-size: 16px; line-height: 1.6;">
                        We received a request to reset your password. Click the button below to choose a new one.
                    </p>
                    <div style="text-align: center; margin: 32px 0;">
                        <a href="%s"
                           style="background: #4f46e5; color: white; padding: 14px 32px; border-radius: 8px; text-decoration: none; font-weight: 600; font-size: 16px; display: inline-block;">
                            Reset Password
                        </a>
                    </div>
                    <p style="color: #94a3b8; font-size: 14px;">
                        This link expires in <strong>1 hour</strong>. If you did not request a password reset, you can safely ignore this email.
                    </p>
                </div>
                <div style="text-align: center; padding: 20px 0; color: #94a3b8; font-size: 12px;">
                    <p>Marine Management System</p>
                </div>
            </body>
            </html>
            """.formatted(firstName != null ? firstName : "Captain", resetUrl);
    }

    private String buildVerificationEmailHtml(String firstName, String verifyUrl) {
        return """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"></head>
            <body style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
                <div style="text-align: center; padding: 20px 0;">
                    <h1 style="color: #4f46e5; margin: 0;">Marine Management</h1>
                </div>
                <div style="background: #f8fafc; border-radius: 12px; padding: 32px; margin: 20px 0;">
                    <h2 style="color: #1e293b; margin-top: 0;">Welcome, %s!</h2>
                    <p style="color: #475569; font-size: 16px; line-height: 1.6;">
                        Thank you for registering. Please verify your email address by clicking the button below.
                    </p>
                    <div style="text-align: center; margin: 32px 0;">
                        <a href="%s"
                           style="background: #4f46e5; color: white; padding: 14px 32px; border-radius: 8px; text-decoration: none; font-weight: 600; font-size: 16px; display: inline-block;">
                            Verify Email Address
                        </a>
                    </div>
                    <p style="color: #94a3b8; font-size: 14px;">
                        This link expires in 24 hours. If you didn't create an account, please ignore this email.
                    </p>
                </div>
                <div style="text-align: center; padding: 20px 0; color: #94a3b8; font-size: 12px;">
                    <p>Marine Management System</p>
                </div>
            </body>
            </html>
            """.formatted(firstName != null ? firstName : "Captain", verifyUrl);
    }
}
