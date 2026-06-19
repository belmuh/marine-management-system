package com.marine.management.smoke;

import com.marine.management.TestcontainersConfiguration;
import com.marine.management.modules.organization.application.OrganizationOnboardingService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import com.marine.management.modules.organization.application.commands.OnboardingResult;
import com.marine.management.modules.organization.application.commands.RegisterYachtCommand;
import com.marine.management.modules.organization.domain.Organization;
import com.marine.management.modules.organization.domain.YachtType;
import com.marine.management.modules.organization.infrastructure.OrganizationRepository;
import com.marine.management.modules.users.domain.User;
import com.marine.management.modules.users.infrastructure.UserRepository;
import com.marine.management.shared.multitenant.TenantContext;
import com.marine.management.shared.security.Role;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tüm smoke/entegrasyon testlerinin ortak base sınıfı.
 *
 * - Testcontainers PostgreSQL (gerçek DB, Flyway migration)
 * - RANDOM_PORT: HTTP-level testler (login, refresh) için gerekli
 * - Her test sınıfı bu class'ı extend eder
 *
 * Helper'lar:
 * - registerOrg()  → yeni Organization + Admin kullanıcı oluşturur
 * - addUser()      → mevcut Org'a CREW/CAPTAIN/MANAGER kullanıcı ekler
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
public abstract class IntegrationTestBase {

    // ── Test email counter — her çağrıda benzersiz email üretir ──────────────
    private static final AtomicInteger counter = new AtomicInteger(0);

    // ── Core services ─────────────────────────────────────────────────────────

    @Autowired
    protected OrganizationOnboardingService onboardingService;

    @Autowired
    protected OrganizationRepository organizationRepository;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @Autowired
    protected TestRestTemplate restTemplate;

    // ── Cleanup ───────────────────────────────────────────────────────────────

    @AfterEach
    void clearContexts() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    /**
     * SecurityContext'e user set eder — AuditingEntityListener created_by_id için buna bakar.
     * Her createEntry/approve/payment çağrısından önce çağrılmalı.
     */
    protected void authenticateAs(User user) {
        var auth = new UsernamePasswordAuthenticationToken(
                user, null, user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Yeni bir Organization + ADMIN kullanıcı oluşturur.
     * managerApprovalEnabled=false (varsayılan): CREW → PENDING_CAPTAIN → APPROVED.
     */
    protected OnboardingResult registerOrg(String prefix) {
        int n = counter.incrementAndGet();
        return onboardingService.registerYacht(new RegisterYachtCommand(
                prefix + "-Yacht-" + n,  // yachtName
                YachtType.SAILING_YACHT,  // yachtType
                15,                        // yachtLength
                "TR",                      // flagCountry
                null,                      // homeMarina
                prefix + " Ltd " + n,     // companyName
                prefix.toLowerCase() + n + "@test.com", // email
                "TestPass123!",            // password
                "Test",                    // firstName
                "Admin",                   // lastName
                null,                      // phoneNumber
                "EUR",                     // baseCurrency
                "Europe/Istanbul",         // timezone
                1,                         // financialYearStartMonth
                null,                      // approvalLimit
                false,                     // managerApprovalEnabled
                null,                      // selectedMainCategoryIds
                null                       // selectedWhoIds
        ));
    }

    /**
     * Manager approval aktif Organization oluşturur.
     * CREW → PENDING_CAPTAIN → PENDING_MANAGER → APPROVED.
     */
    protected OnboardingResult registerOrgWithManagerApproval(String prefix, java.math.BigDecimal limit) {
        int n = counter.incrementAndGet();
        return onboardingService.registerYacht(new RegisterYachtCommand(
                prefix + "-Yacht-" + n,  // yachtName
                YachtType.SAILING_YACHT,  // yachtType
                15,                        // yachtLength
                "TR",                      // flagCountry
                null,                      // homeMarina
                prefix + " Ltd " + n,     // companyName
                prefix.toLowerCase() + n + "@test.com", // email
                "TestPass123!",            // password
                "Test",                    // firstName
                "Admin",                   // lastName
                null,                      // phoneNumber
                "EUR",                     // baseCurrency
                "Europe/Istanbul",         // timezone
                1,                         // financialYearStartMonth
                limit,                     // approvalLimit
                true,                      // managerApprovalEnabled
                null,                      // selectedMainCategoryIds
                null                       // selectedWhoIds
        ));
    }

    /**
     * Mevcut organization'a belirtilen role'de kullanıcı ekler.
     * Kullanıcı doğrulanmış (emailVerified=true) olarak oluşturulur.
     */
    protected User addUser(Organization org, Role role, String prefix) {
        int n = counter.incrementAndGet();
        String email = prefix.toLowerCase() + n + "@test.com";
        User user = User.createWithHashedPassword(
                email,
                prefix, "User",
                passwordEncoder.encode("TestPass123!"),
                role,
                org
        );
        return userRepository.save(user);
    }

    /**
     * Organization entity'sini ID ile çeker.
     */
    protected Organization findOrg(Long orgId) {
        return organizationRepository.findById(orgId)
                .orElseThrow(() -> new IllegalStateException("Org not found: " + orgId));
    }
}
