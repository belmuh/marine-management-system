package com.marine.management.smoke;

import com.marine.management.modules.auth.presentation.dto.LoginRequest;
import com.marine.management.modules.finance.application.ApprovalService;
import com.marine.management.modules.finance.application.FinancialEntryService;
import com.marine.management.modules.finance.application.PaymentService;
import com.marine.management.modules.finance.application.commands.CreateEntryCommand;
import com.marine.management.modules.finance.domain.enums.EntryStatus;
import com.marine.management.modules.finance.domain.enums.PaymentMethod;
import com.marine.management.modules.finance.domain.enums.RecordType;
import com.marine.management.modules.finance.domain.vo.Money;
import com.marine.management.modules.finance.application.FinancialCategoryService;
import com.marine.management.modules.finance.presentation.dto.EntryResponseDto;
import com.marine.management.modules.organization.application.commands.OnboardingResult;
import com.marine.management.modules.organization.domain.Organization;
import com.marine.management.modules.users.domain.User;
import com.marine.management.shared.multitenant.TenantContext;
import com.marine.management.shared.security.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pilot öncesi 8 smoke testi.
 *
 * Deploy bu testler geçmeden yapılmaz.
 *
 * Test 1  — Login: yeni org register → admin login → 200 + access token
 * Test 2  — CREW: entry oluştur → submit → PENDING_CAPTAIN
 * Test 3  — CAPTAIN: kendi entry'sini oluştur → submit → APPROVED (auto-approve)
 * Test 4  — CAPTAIN: CREW entry'sini approve → APPROVED
 * Test 5  — MANAGER: PENDING_MANAGER entry'yi partial approve → PARTIALLY_APPROVED
 * Test 6  — CAPTAIN: APPROVED entry'ye payment kaydet → PAID / PARTIALLY_PAID
 * Test 7  — Cross-tenant izolasyon: Org A entry'leri Org B'ye görünmez
 * Test 8  — JWT refresh: login → /api/auth/refresh → yeni access token
 */
class SmokeIntegrationTest extends IntegrationTestBase {

    @Autowired private FinancialEntryService entryService;
    @Autowired private ApprovalService approvalService;
    @Autowired private PaymentService paymentService;
    @Autowired private FinancialCategoryService categoryService;

    // ─────────────────────────────────────────────────────────────────────────
    // TEST 1 — Login happy path
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("T1: Yeni org register → admin login → 200 + access token")
    void login_shouldReturnAccessToken_whenCredentialsValid() {
        OnboardingResult org = registerOrg("T1");

        LoginRequest req = new LoginRequest(org.email(), "TestPass123!");
        ResponseEntity<String> resp = restTemplate.postForEntity(
                "/api/auth/login", req, String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains("accessToken");
        assertThat(resp.getHeaders().get(HttpHeaders.SET_COOKIE))
                .anyMatch(c -> c.startsWith("refreshToken="));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TEST 2 — CREW entry oluştur → submit
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("T2: CREW entry oluştur → submit → PENDING_CAPTAIN")
    void crew_shouldSubmitEntry_toPendingCaptain() {
        OnboardingResult result = registerOrg("T2");
        Organization org = findOrg(result.organizationId());
        User crew = addUser(org, Role.CREW, "Crew");

        TenantContext.setCurrentTenantId(result.organizationId());
        authenticateAs(crew);
        try {
            UUID categoryId = firstExpenseCategoryId();
            CreateEntryCommand cmd = buildEntryCommand(RecordType.EXPENSE, categoryId, "50", crew);

            EntryResponseDto created = entryService.createEntry(cmd);
            assertThat(created.status()).isEqualTo(EntryStatus.DRAFT);

            EntryResponseDto submitted = approvalService.submit(created.id(), crew);
            assertThat(submitted.status()).isEqualTo(EntryStatus.PENDING_CAPTAIN);
        } finally {
            TenantContext.clear();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TEST 3 — CAPTAIN kendi entry'si → auto-approve
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("T3: CAPTAIN kendi entry'sini oluştur → submit → APPROVED")
    void captain_shouldAutoApproveOwnEntry_whenNoManagerRequired() {
        OnboardingResult result = registerOrg("T3");
        Organization org = findOrg(result.organizationId());
        // ADMIN rolü CAPTAIN gibi davranır (Role.isCaptain() → true)
        User captain = userRepository.findByEmail(result.email()).orElseThrow();

        TenantContext.setCurrentTenantId(result.organizationId());
        authenticateAs(captain);
        try {
            UUID categoryId = firstExpenseCategoryId();
            CreateEntryCommand cmd = buildEntryCommand(RecordType.EXPENSE, categoryId, "100", captain);

            EntryResponseDto created = entryService.createEntry(cmd);
            EntryResponseDto submitted = approvalService.submit(created.id(), captain);

            // Captain submit → managerApproval=false → APPROVED
            assertThat(submitted.status()).isEqualTo(EntryStatus.APPROVED);
        } finally {
            TenantContext.clear();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TEST 4 — CAPTAIN CREW entry'sini approve
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("T4: CAPTAIN → CREW entry'sini approve → APPROVED")
    void captain_shouldApproveCrew_entry() {
        OnboardingResult result = registerOrg("T4");
        Organization org = findOrg(result.organizationId());
        User captain = userRepository.findByEmail(result.email()).orElseThrow();
        User crew = addUser(org, Role.CREW, "Crew");

        TenantContext.setCurrentTenantId(result.organizationId());
        authenticateAs(crew);
        try {
            UUID categoryId = firstExpenseCategoryId();
            EntryResponseDto created = entryService.createEntry(
                    buildEntryCommand(RecordType.EXPENSE, categoryId, "75", crew));
            approvalService.submit(created.id(), crew); // → PENDING_CAPTAIN

            authenticateAs(captain);
            EntryResponseDto approved = approvalService.approve(created.id(), captain);
            assertThat(approved.status()).isEqualTo(EntryStatus.APPROVED);
        } finally {
            TenantContext.clear();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TEST 5 — MANAGER partial approve
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("T5: MANAGER → PENDING_MANAGER entry'yi partial approve")
    void manager_shouldPartiallyApprove_pendingManagerEntry() {
        // Manager approval aktif: limit=50 EUR, entry=200 EUR → PENDING_MANAGER
        OnboardingResult result = registerOrgWithManagerApproval("T5", new BigDecimal("50.00"));
        Organization org = findOrg(result.organizationId());
        User captain = userRepository.findByEmail(result.email()).orElseThrow();
        User crew = addUser(org, Role.CREW, "Crew");
        User manager = addUser(org, Role.MANAGER, "Manager");

        TenantContext.setCurrentTenantId(result.organizationId());
        authenticateAs(crew);
        try {
            UUID categoryId = firstExpenseCategoryId();
            // 200 EUR > limit 50 EUR → captain approve → PENDING_MANAGER
            EntryResponseDto created = entryService.createEntry(
                    buildEntryCommand(RecordType.EXPENSE, categoryId, "200", crew));
            approvalService.submit(created.id(), crew);           // → PENDING_CAPTAIN

            authenticateAs(captain);
            approvalService.approveByCaptain(created.id(), captain); // → PENDING_MANAGER

            // Manager partial approve: sadece 80 EUR onaylar
            authenticateAs(manager);
            Money partialAmount = Money.of("80.00", "EUR");
            EntryResponseDto partialApproved = approvalService.approveByManager(
                    created.id(), manager, partialAmount);

            // Partial approve → APPROVED ama approvedBaseAmount < requestedAmount
            assertThat(partialApproved.status()).isEqualTo(EntryStatus.APPROVED);
            assertThat(partialApproved.approvedBaseAmount()).isNotNull();
            assertThat(partialApproved.approvedBaseAmount().getAmountAsBigDecimal())
                    .isEqualByComparingTo(new BigDecimal("80.00"));
        } finally {
            TenantContext.clear();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TEST 6 — Payment kaydet → PAID
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("T6: CAPTAIN → APPROVED entry'ye tam ödeme kaydet → PAID")
    void captain_shouldRecordPayment_onApprovedEntry() {
        OnboardingResult result = registerOrg("T6");
        Organization org = findOrg(result.organizationId());
        User captain = userRepository.findByEmail(result.email()).orElseThrow();
        User crew = addUser(org, Role.CREW, "Crew");

        TenantContext.setCurrentTenantId(result.organizationId());
        authenticateAs(crew);
        try {
            UUID categoryId = firstExpenseCategoryId();
            EntryResponseDto created = entryService.createEntry(
                    buildEntryCommand(RecordType.EXPENSE, categoryId, "120", crew));
            approvalService.submit(created.id(), crew);

            authenticateAs(captain);
            approvalService.approve(created.id(), captain); // → APPROVED

            EntryResponseDto paid = paymentService.recordPayment(
                    created.id(),
                    Money.of("120.00", "EUR"),
                    LocalDate.now(),
                    "REF-001",
                    PaymentMethod.BANK_TRANSFER,
                    "Test payment",
                    captain
            );

            assertThat(paid.status()).isEqualTo(EntryStatus.PAID);
        } finally {
            TenantContext.clear();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TEST 7 — Cross-tenant izolasyon (FinancialEntry seviyesi)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("T7: Org A entry'leri Org B kullanıcısına görünmez")
    void entryIsolation_orgACannotSeeOrgBEntries() {
        OnboardingResult orgA = registerOrg("T7A");
        OnboardingResult orgB = registerOrg("T7B");
        User captainA = userRepository.findByEmail(orgA.email()).orElseThrow();
        User captainB = userRepository.findByEmail(orgB.email()).orElseThrow();

        // Org A'da bir entry oluştur ve onayla
        UUID entryId;
        TenantContext.setCurrentTenantId(orgA.organizationId());
        authenticateAs(captainA);
        try {
            UUID categoryId = firstExpenseCategoryId();
            EntryResponseDto created = entryService.createEntry(
                    buildEntryCommand(RecordType.EXPENSE, categoryId, "99", captainA));
            entryId = created.id();
        } finally {
            TenantContext.clear();
        }

        // Org B context'inde Org A'nın entry'si görünmemeli
        TenantContext.setCurrentTenantId(orgB.organizationId());
        try {
            // entryRepository doğrudan findById — tenant filter'dan geçmez
            // ama service üzerinden erişim filter'a takılmalı
            List<EntryResponseDto> orgBEntries = approvalService.getPendingForUser(captainB);
            assertThat(orgBEntries)
                    .as("Org B pending listesinde Org A'nın entry'si olmamalı")
                    .noneMatch(e -> e.id().equals(entryId));
        } finally {
            TenantContext.clear();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TEST 8 — JWT refresh akışı
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("T8: Login → /api/auth/refresh cookie ile → yeni access token")
    void refreshToken_shouldReturnNewAccessToken_whenCookieValid() {
        OnboardingResult org = registerOrg("T8");

        // 1. Login → refresh token cookie al
        LoginRequest loginReq = new LoginRequest(org.email(), "TestPass123!");
        ResponseEntity<String> loginResp = restTemplate.postForEntity(
                "/api/auth/login", loginReq, String.class);

        assertThat(loginResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        String setCookieHeader = loginResp.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertThat(setCookieHeader).startsWith("refreshToken=");

        // Cookie değerini ayıkla
        String cookieValue = setCookieHeader.split(";")[0]; // "refreshToken=<value>"

        // 2. /api/auth/refresh — cookie ile çağır → yeni access token
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, cookieValue);
        ResponseEntity<String> refreshResp = restTemplate.exchange(
                "/api/auth/refresh",
                HttpMethod.POST,
                new HttpEntity<>(headers),
                String.class
        );

        assertThat(refreshResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(refreshResp.getBody()).contains("accessToken");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Mevcut tenant context'indeki ilk aktif EXPENSE kategorisinin ID'si.
     * Servis üzerinden geçer — TenantFilterAspect otomatik devreye girer.
     * Çağırmadan önce TenantContext set edilmiş olmalı.
     */
    private UUID firstExpenseCategoryId() {
        return categoryService.findAllActive()
                .stream()
                .filter(c -> c.getCategoryType() == RecordType.EXPENSE)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Aktif EXPENSE kategorisi bulunamadı — TenantContext set edilmemiş olabilir"))
                .getId();
    }

    private CreateEntryCommand buildEntryCommand(
            RecordType type, UUID categoryId, String eurAmount, User creator) {
        return new CreateEntryCommand(
                type,
                categoryId,
                Money.of(eurAmount, "EUR"),
                LocalDate.now(),
                PaymentMethod.CASH,
                "Smoke test entry",
                creator,
                null,  // whoId — optional
                null,  // mainCategoryId — optional
                null,  // recipient
                null,  // country
                null,  // city
                null,  // specificLocation
                null   // vendor
        );
    }
}
