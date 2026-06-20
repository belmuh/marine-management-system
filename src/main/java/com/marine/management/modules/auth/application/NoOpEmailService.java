package com.marine.management.modules.auth.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * No-op implementation of EmailService — active when app.mail.enabled is not true
 * (i.e., local dev and test environments).
 *
 * Logs what would have been sent so developers can copy verification/reset links
 * from the console without needing a real mail server.
 *
 * Uses an explicit @ConditionalOnProperty (inverse of SmtpEmailService) instead of
 * @ConditionalOnMissingBean to avoid bean-ordering issues that cause
 * NoSuchBeanDefinitionException during Spring Boot test context startup.
 */
@Service
@ConditionalOnProperty(name = "app.mail.enabled", havingValue = "false", matchIfMissing = true)
public class NoOpEmailService implements EmailService {

    private static final Logger logger = LoggerFactory.getLogger(NoOpEmailService.class);

    @Override
    @Async
    public void sendVerificationEmail(String toEmail, String firstName, String verificationToken) {
        logger.info("========================================");
        logger.info("[MAIL DISABLED] Verification email");
        logger.info("To: {}", toEmail);
        logger.info("Token: {}", verificationToken);
        logger.info("========================================");
    }

    @Override
    @Async
    public void sendPasswordResetEmail(String toEmail, String firstName, String resetToken) {
        logger.info("========================================");
        logger.info("[MAIL DISABLED] Password reset email");
        logger.info("To: {}", toEmail);
        logger.info("Token: {}", resetToken);
        logger.info("========================================");
    }
}
