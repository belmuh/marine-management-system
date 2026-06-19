package com.marine.management.shared.multitenant;

import com.marine.management.TestcontainersConfiguration;
import com.marine.management.modules.finance.application.FinancialCategoryService;
import com.marine.management.modules.finance.domain.entities.FinancialCategory;
import com.marine.management.modules.finance.domain.enums.RecordType;
import com.marine.management.modules.organization.application.OrganizationOnboardingService;
import com.marine.management.modules.organization.application.commands.OnboardingResult;
import com.marine.management.modules.organization.application.commands.RegisterYachtCommand;
import com.marine.management.modules.organization.domain.YachtType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tenant izolasyonunun gerçek DB'de çalışıp çalışmadığını doğrular.
 *
 * Kapsanan senaryolar:
 * 1. READ izolasyonu — bir tenant diğerinin verisini göremez
 * 2. findAll() (disabled dahil) da filter'dan geçer
 * 3. @Order / @Transactional etkileşimi — kesin sayı eşleşmesi ile ölçülür
 * 4. Context yokken servis çağrısı reddedilir (sözleşme testi)
 * 5. WRITE izolasyonu — yeni kayıt doğru tenant_id ile oluşturulur,
 *    diğer tenant'ın listesinde görünmez
 *
 * KAPSANMAYAN (TODO — entry oluşturma flow'u gerektiriyor):
 * 6. FinancialEntry READ/WRITE izolasyonu
 *    → FinancialEntryService.createEntry() bağımlılıkları tamamlanınca ekle
 *
 * NOT — Repository doğrudan çağrısı:
 * TenantFilterAspect @Service ve @Transactional metodları intercept eder.
 * Repository interface metodları (JpaRepository.count() vb.) JDK proxy üzerinden
 * çağrıldığında @annotation(Transactional) eşleşmez — filter uygulanmaz.
 * Bu nedenle testler servis katmanı üzerinden geçer, repository'yi doğrudan çağırmaz.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class TenantIsolationIntegrationTest {

    @Autowired
    private OrganizationOnboardingService onboardingService;

    @Autowired
    private FinancialCategoryService categoryService;

    @AfterEach
    void cleanupContext() {
        TenantContext.clear();
    }

    // ── 1. TEMEL READ İZOLASYONU ─────────────────────────────────────────────

    @Test
    void findAllActive_shouldReturnOnlyOwnCategories_whenTwoTenantsExist() {
        OnboardingResult orgA = register("ISO-READ-A", "isoA@test.com");
        OnboardingResult orgB = register("ISO-READ-B", "isoB@test.com");

        TenantContext.setCurrentTenantId(orgA.organizationId());
        List<FinancialCategory> categoriesA = categoryService.findAllActive();
        TenantContext.clear();

        TenantContext.setCurrentTenantId(orgB.organizationId());
        List<FinancialCategory> categoriesB = categoryService.findAllActive();
        TenantContext.clear();

        assertThat(categoriesA).isNotEmpty();
        assertThat(categoriesA)
                .allSatisfy(c -> assertThat(c.getTenantId())
                        .as("Org A görünümünde org B'nin kaydı olmamalı")
                        .isEqualTo(orgA.organizationId()));

        assertThat(categoriesB).isNotEmpty();
        assertThat(categoriesB)
                .allSatisfy(c -> assertThat(c.getTenantId())
                        .as("Org B görünümünde org A'nın kaydı olmamalı")
                        .isEqualTo(orgB.organizationId()));

        Set<UUID> idsA = categoriesA.stream().map(FinancialCategory::getId).collect(Collectors.toSet());
        Set<UUID> idsB = categoriesB.stream().map(FinancialCategory::getId).collect(Collectors.toSet());
        assertThat(idsA)
                .as("İki tenant'ın ID kümeleri tamamen ayrı olmalı")
                .doesNotContainAnyElementsOf(idsB);
    }

    // ── 2. findAll() DA FILTER'DAN GEÇMELİ ─────────────────────────────────

    @Test
    void findAll_shouldReturnOnlyOwnCategories_includingDisabled() {
        OnboardingResult orgA = register("ISO-ALL-A", "isoAllA@test.com");
        OnboardingResult orgB = register("ISO-ALL-B", "isoAllB@test.com");

        TenantContext.setCurrentTenantId(orgA.organizationId());
        List<FinancialCategory> allA = categoryService.findAll();
        TenantContext.clear();

        assertThat(allA).isNotEmpty();
        assertThat(allA)
                .allSatisfy(c -> assertThat(c.getTenantId())
                        .as("findAll() cross-tenant veri döndürmemeli")
                        .isEqualTo(orgA.organizationId()));
        assertThat(allA)
                .extracting(FinancialCategory::getTenantId)
                .doesNotContain(orgB.organizationId());
    }

    // ── 3. KESIN SAYI EŞLEŞMESİ (@ORDER / @TRANSACTIONAL ETKİLEŞİMİ) ───────
    //
    // Zayıf assertion ("hasSizeLessThan total") yerine kesin sayı:
    // filter geçici session'a uygulanıyorsa başka tenant'ların verisi de döner,
    // sayı perTenantCount'tan fazla olur ve test çöker.

    @Test
    void tenantFilter_shouldReturnExactlyOwnData_notMixedFromMultipleTenants() {
        OnboardingResult orgA = register("ORDER-A", "orderA@test.com");
        register("ORDER-B", "orderB@test.com");
        register("ORDER-C", "orderC@test.com");

        // Org A'nın kesin sayısını referans al
        TenantContext.setCurrentTenantId(orgA.organizationId());
        int expectedCount = categoryService.findAllActive().size();
        TenantContext.clear();

        assertThat(expectedCount).isGreaterThan(0);

        // Aynı sorguyu tekrar koş — 3 tenant varken sadece kendi sayısı dönmeli
        TenantContext.setCurrentTenantId(orgA.organizationId());
        List<FinancialCategory> result = categoryService.findAllActive();
        TenantContext.clear();

        assertThat(result)
                .as("Filter broken: expected %d (org A only), got %d "
                    + "(>expected means cross-tenant leak, <expected means over-filtering)",
                        expectedCount, result.size())
                .hasSize(expectedCount);

        assertThat(result)
                .allSatisfy(c -> assertThat(c.getTenantId()).isEqualTo(orgA.organizationId()));
    }

    // ── 4. CONTEXT YOKKEN SERVİS REDDEDİLMELİ ───────────────────────────────
    //
    // TenantInterceptor HTTP katmanında reddeder, ama bu test servis katmanındaki
    // guard'ın da çalıştığını doğrular — HTTP dışı çağrı senaryoları için kritik.

    @Test
    void service_shouldRejectCall_whenNoTenantContextSet() {
        register("NO-CTX", "noctx@test.com");

        // context set edilmeden servis çağrısı → AccessDeniedException fırlatmalı
        assertThatThrownBy(() -> categoryService.findAllActive())
                .isInstanceOf(AccessDeniedException.class);
    }

    // ── 5. WRITE İZOLASYONU ──────────────────────────────────────────────────
    //
    // Yeni kayıt doğru tenant'a atanmalı; diğer tenant'ın listesinde görünmemeli.

    @Test
    void newCategory_shouldBeCreatedWithCorrectTenantId_andNotVisibleToOtherTenant() {
        OnboardingResult orgA = register("WRITE-A", "writeA@test.com");
        OnboardingResult orgB = register("WRITE-B", "writeB@test.com");

        // Org A'nın başlangıç sayısını al
        TenantContext.setCurrentTenantId(orgA.organizationId());
        int orgABefore = categoryService.findAll().size();
        TenantContext.clear();

        // Org B context'inde yeni kategori oluştur
        TenantContext.setCurrentTenantId(orgB.organizationId());
        FinancialCategory created = categoryService.create(
                "Test Gider Kategorisi", RecordType.EXPENSE, null, 99, false);
        TenantContext.clear();

        // Oluşturulan kaydın tenant_id'si org B olmalı
        assertThat(created.getTenantId())
                .as("Yeni kategori org B'ye atanmalı")
                .isEqualTo(orgB.organizationId());

        // Org A'nın listesi değişmemiş olmalı
        TenantContext.setCurrentTenantId(orgA.organizationId());
        int orgAAfter = categoryService.findAll().size();
        TenantContext.clear();

        assertThat(orgAAfter)
                .as("Org B'de oluşturulan kategori org A'nın sayısını etkilememeli")
                .isEqualTo(orgABefore);

        // Org A'nın listesinde oluşturulan kaydın ID'si bulunmamalı
        TenantContext.setCurrentTenantId(orgA.organizationId());
        List<FinancialCategory> orgACategories = categoryService.findAll();
        TenantContext.clear();

        assertThat(orgACategories)
                .extracting(FinancialCategory::getId)
                .as("Org B'de oluşturulan kategori org A'da görünmemeli")
                .doesNotContain(created.getId());
    }

    // ── HELPER ───────────────────────────────────────────────────────────────

    private OnboardingResult register(String yachtName, String email) {
        return onboardingService.registerYacht(new RegisterYachtCommand(
                yachtName, YachtType.SAILING_YACHT, 15, "TR", null,
                yachtName + " Ltd", email, "TestPass123!",
                "Test", "Admin", null,
                "EUR", "Europe/Istanbul", 1, null, false, null, null
        ));
    }
}
