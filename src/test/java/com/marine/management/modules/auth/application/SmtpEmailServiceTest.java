package com.marine.management.modules.auth.application;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * SmtpEmailService unit testleri — Spring context yok, Testcontainers yok.
 *
 * @Async bu testte aktif değil: Spring AOP proxy olmadan düz metod çağrısı
 * yapılır, testler senkron çalışır. Async davranışı (thread izolasyonu vb.)
 * integration test kapsamında.
 *
 * MimeMessage: gerçek Session ile oluşturulur (SMTP bağlantısı açılmaz),
 * böylece MimeMessageHelper'ın iç çağrıları (setFrom, setTo vb.) sorunsuz çalışır.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SmtpEmailService")
class SmtpEmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    private SmtpEmailService service;

    private static final String FROM    = "noreply@maritar.com";
    private static final String VERIFY  = "https://app.maritar.com/verify-email";
    private static final String RESET   = "https://app.maritar.com/reset-password";

    @BeforeEach
    void setUp() {
        // Constructor injection — Spring olmadan direkt nesne
        service = new SmtpEmailService(mailSender, FROM, VERIFY, RESET);

        // Her testte createMimeMessage() gerçek MimeMessage dönsün
        // (SMTP bağlantısı açılmaz; sadece nesne oluşturulur)
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
    }

    // ═══════════════════════════════════════════════════════════════════
    // sendVerificationEmail
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("sendVerificationEmail")
    class SendVerificationEmail {

        @Test
        @DisplayName("token ve firstName verildiğinde mailSender.send() çağrılır")
        void shouldCallSend_WhenValidInputGiven() {
            // When
            service.sendVerificationEmail("user@test.com", "Ahmet", "token-abc");

            // Then
            verify(mailSender).send(any(MimeMessage.class));
        }

        @Test
        @DisplayName("firstName null ise 'Captain' fallback kullanılır ve send çağrılır")
        void shouldUseCaptainFallback_WhenFirstNameIsNull() {
            // When
            service.sendVerificationEmail("user@test.com", null, "token-abc");

            // Then — exception fırlatmadan tamamlanmalı, send çağrılmalı
            verify(mailSender).send(any(MimeMessage.class));
        }

        @Test
        @DisplayName("SMTP hatası fırlatılırsa exception dışarı sızmaz (registration etkilenmez)")
        void shouldSwallowException_WhenMailSenderThrows() {
            // Given
            doThrow(new MailSendException("SMTP connection refused"))
                    .when(mailSender).send(any(MimeMessage.class));

            // When / Then — exception dışarı çıkmamalı
            assertThatNoException()
                    .isThrownBy(() -> service.sendVerificationEmail("user@test.com", "Ahmet", "token-abc"));
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // sendPasswordResetEmail
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("sendPasswordResetEmail")
    class SendPasswordResetEmail {

        @Test
        @DisplayName("token ve firstName verildiğinde mailSender.send() çağrılır")
        void shouldCallSend_WhenValidInputGiven() {
            // When
            service.sendPasswordResetEmail("user@test.com", "Belma", "reset-xyz");

            // Then
            verify(mailSender).send(any(MimeMessage.class));
        }

        @Test
        @DisplayName("firstName null ise 'Captain' fallback kullanılır ve send çağrılır")
        void shouldUseCaptainFallback_WhenFirstNameIsNull() {
            // When
            service.sendPasswordResetEmail("user@test.com", null, "reset-xyz");

            // Then
            verify(mailSender).send(any(MimeMessage.class));
        }

        @Test
        @DisplayName("SMTP hatası fırlatılırsa exception dışarı sızmaz")
        void shouldSwallowException_WhenMailSenderThrows() {
            // Given
            doThrow(new MailSendException("SMTP timeout"))
                    .when(mailSender).send(any(MimeMessage.class));

            // When / Then
            assertThatNoException()
                    .isThrownBy(() -> service.sendPasswordResetEmail("user@test.com", "Belma", "reset-xyz"));
        }
    }
}
