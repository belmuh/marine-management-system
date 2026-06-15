package com.marine.management.modules.finance.application;

import com.marine.management.modules.finance.domain.entities.FinancialCategory;
import com.marine.management.modules.finance.domain.entities.MainCategory;
import com.marine.management.modules.finance.domain.entities.TenantMainCategory;
import com.marine.management.modules.finance.domain.entities.TenantWhoSelection;
import com.marine.management.modules.finance.domain.entities.Who;
import com.marine.management.modules.finance.domain.enums.RecordType;
import com.marine.management.modules.finance.infrastructure.FinancialCategoryRepository;
import com.marine.management.modules.finance.infrastructure.MainCategoryRepository;
import com.marine.management.modules.finance.infrastructure.TenantMainCategoryRepository;
import com.marine.management.modules.finance.infrastructure.TenantWhoSelectionRepository;
import com.marine.management.modules.finance.infrastructure.WhoRepository;
import com.marine.management.shared.multitenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Initializes tenant-specific reference data on new tenant registration.
 *
 * Creates:
 * - TenantMainCategory and TenantWhoSelection records from global seed data
 * - Default FinancialCategory starter set (CATEGORY_SEED.md) so a new tenant
 *   can create their first entry immediately — without this the required
 *   category dropdown is empty and the user hits a dead end.
 */
@Service
public class TenantReferenceDataInitializer {

    private static final Logger logger = LoggerFactory.getLogger(TenantReferenceDataInitializer.class);

    private final MainCategoryRepository mainCategoryRepository;
    private final WhoRepository whoRepository;
    private final TenantMainCategoryRepository tenantMainCategoryRepository;
    private final TenantWhoSelectionRepository tenantWhoSelectionRepository;
    private final FinancialCategoryRepository financialCategoryRepository;

    public TenantReferenceDataInitializer(
            MainCategoryRepository mainCategoryRepository,
            WhoRepository whoRepository,
            TenantMainCategoryRepository tenantMainCategoryRepository,
            TenantWhoSelectionRepository tenantWhoSelectionRepository,
            FinancialCategoryRepository financialCategoryRepository
    ) {
        this.mainCategoryRepository = mainCategoryRepository;
        this.whoRepository = whoRepository;
        this.tenantMainCategoryRepository = tenantMainCategoryRepository;
        this.tenantWhoSelectionRepository = tenantWhoSelectionRepository;
        this.financialCategoryRepository = financialCategoryRepository;
    }

    // ═══════════════════════════════════════════════════════════════
    // STARTER CATEGORY SET — kaynak: docs/CATEGORY_SEED.md
    // Piyasa yazılımı taksonomisinin mevcut MainCategory yapısına eşlenmiş hali.
    // mainCategoryNameEn = hangi ana kategori seçiliyse o kategorilerin oluşacağını belirler.
    // ═══════════════════════════════════════════════════════════════

    private record SeedCategory(String nameTr, String nameEn, String mainCategoryNameEn, boolean technical) {}

    // Sadeleştirme ilkesi (2026-06): WHO ekseniyle ayrıştırılabilen alt kırılımlar
    // ayrı kategori OLMAZ. Örn. tek "Yakıt" kategorisi + WHO=Tender/Jetski/Ana Makine;
    // tek "Sigorta" + WHO=Tekne Gövdesi/Personel/Misafir. Eski 22'lik listeden
    // birleştirilenler: İnternet&Uydu+Telefon, Transfer&Ulaşım+Personel Seyahat,
    // Tekne&Makine+Sağlık Sigortası, Yat+Tender&Jetski Yakıtı, Bakım&Onarım+Yedek Parça.
    private static final List<SeedCategory> EXPENSE_SEED = List.of(
            new SeedCategory("Banka Masrafları", "Bank Charges", "Administration", false),
            new SeedCategory("Yönetim Ücretleri", "Management Fees", "Administration", false),
            new SeedCategory("Acente, Vergi & Resmi İşlemler", "Agency Fees, Taxes & Formalities", "Administration", false),
            new SeedCategory("Bahşişler", "Gratuities", "Administration", false),
            new SeedCategory("Diğer Giderler", "Other Expenses", "Administration", false),
            // WHO: Personel/Misafir/Tekne Sahibi → kimin seyahati olduğunu belirler
            new SeedCategory("Seyahat & Ulaşım", "Travel & Transfers", "Administration", false),
            new SeedCategory("İnternet & Telefon", "Internet & Phones", "Communication", false),
            new SeedCategory("Personel Maaşları", "Crew Wages", "Crew Expenses", false),
            // WHO: Kaptan/Personel/Misafir → kimin sağlık gideri olduğunu belirler
            new SeedCategory("Sağlık Giderleri", "Medical Expenses", "Crew Expenses", false),
            new SeedCategory("Üniforma", "Uniforms", "Crew Expenses", false),
            new SeedCategory("Vize", "Visas", "Crew Expenses", false),
            // WHO: Tekne Gövdesi → tekne/makine; Personel/Misafir → sağlık sigortası
            new SeedCategory("Sigorta Poliçeleri", "Insurance Policies", "Insurance", false),
            new SeedCategory("Marina & Bağlama", "Dockage & Marina", "Dockage & Berths", false),
            new SeedCategory("Su & Elektrik", "Water & Electricity", "Dockage & Berths", false),
            // WHO: Ana Makine/Jeneratör → yat; Tender/Jetski → toys
            new SeedCategory("Yakıt & Yağ", "Fuel & Lubricants", "Fuel", true),
            // Yedek parça dahil — WHO teknik listesi (Jeneratör, Klima, ...) sistemi belirler
            new SeedCategory("Onarım & Yedek Parça", "Repairs & Spare Parts", "Maintenance & Repairs", true),
            // WHO: Personel/Misafir/Tekne Sahibi → kimin ikramı/kumanyası
            new SeedCategory("Yiyecek & İçecek", "Food & Beverages", "Provisions", false)
    );

    private static final List<SeedCategory> INCOME_SEED = List.of(
            new SeedCategory("Sahip Fonu", "Owner's Funds", null, false),
            new SeedCategory("Charter Geliri", "Charter Income", null, false),
            new SeedCategory("Diğer Gelir", "Other Income", null, false)
    );

    // Canonical "starter category name → main category (English name)" lookup,
    // keyed by both Turkish and English category names. Exposed so demo data and
    // any auto-classification can roll an entry's line-item category up to the
    // correct main category instead of guessing.
    private static final Map<String, String> CATEGORY_TO_MAIN_CATEGORY_EN;
    static {
        Map<String, String> m = new HashMap<>();
        for (SeedCategory s : EXPENSE_SEED) {
            if (s.mainCategoryNameEn() != null) {
                m.put(s.nameTr(), s.mainCategoryNameEn());
                m.put(s.nameEn(), s.mainCategoryNameEn());
            }
        }
        CATEGORY_TO_MAIN_CATEGORY_EN = Map.copyOf(m);
    }

    /**
     * Returns the canonical main category (English name) for a starter category
     * name (Turkish or English), or {@code null} if the category is not in the
     * starter taxonomy.
     */
    public static String mainCategoryNameEnFor(String categoryName) {
        return categoryName == null ? null : CATEGORY_TO_MAIN_CATEGORY_EN.get(categoryName);
    }

    /**
     * Initialize all reference data for current tenant.
     * Called during registration. Enables all by default, English category names.
     *
     * ⚠️ REQUIRES: TenantContext must be set before calling!
     */
    @Transactional
    public void initializeTenantReferenceData() {
        initializeTenantReferenceData(null, null, null);
    }

    /**
     * Backward-compatible overload — defaults to English category names.
     */
    @Transactional
    public void initializeTenantReferenceData(Set<Long> selectedMainCategoryIds, Set<Long> selectedWhoIds) {
        initializeTenantReferenceData(selectedMainCategoryIds, selectedWhoIds, null);
    }

    /**
     * Initialize reference data for current tenant with optional selections.
     *
     * @param selectedMainCategoryIds IDs of main categories to enable (null = enable all)
     * @param selectedWhoIds IDs of WHO entries to enable (null = enable all)
     * @param flagCountry tenant flag country (ISO 3166-1 alpha-2); "TR" → Turkish
     *                    starter category names, otherwise English
     *
     * ⚠️ REQUIRES: TenantContext must be set before calling!
     */
    @Transactional
    public void initializeTenantReferenceData(
            Set<Long> selectedMainCategoryIds,
            Set<Long> selectedWhoIds,
            String flagCountry
    ) {
        Long tenantId = TenantContext.getCurrentTenantId();

        logger.info("Initializing reference data for tenant: {}", tenantId);

        initializeMainCategories(selectedMainCategoryIds);
        initializeWhoSelections(selectedWhoIds);
        initializeStarterCategories(selectedMainCategoryIds, "TR".equalsIgnoreCase(flagCountry));

        logger.info("Reference data initialization completed for tenant: {}", tenantId);
    }

    /**
     * Copy all global MainCategory entries to tenant-specific records.
     * If selectedIds is provided, only those are enabled; others are disabled.
     * If selectedIds is null, all are enabled.
     */
    private void initializeMainCategories(Set<Long> selectedIds) {
        List<MainCategory> globalCategories = mainCategoryRepository.findAll();

        logger.info("Found {} global MainCategories for tenant: {}",
                globalCategories.size(), TenantContext.getCurrentTenantId());

        if (globalCategories.isEmpty()) {
            logger.warn("No global MainCategories found! Skipping tenant initialization.");
            return;
        }

        boolean enableAll = selectedIds == null || selectedIds.isEmpty();

        List<TenantMainCategory> tenantCategories = globalCategories.stream()
                .map(mc -> {
                    TenantMainCategory tmc = TenantMainCategory.create(mc);
                    if (!enableAll && !selectedIds.contains(mc.getId())) {
                        tmc.disable();
                    }
                    return tmc;
                })
                .toList();

        tenantMainCategoryRepository.saveAll(tenantCategories);
        tenantMainCategoryRepository.flush();

        long enabledCount = tenantCategories.stream().filter(TenantMainCategory::isEnabled).count();
        logger.info("Created {} TenantMainCategory records ({} enabled) for tenant: {}",
                tenantCategories.size(), enabledCount, TenantContext.getCurrentTenantId());
    }

    /**
     * Copy all global WHO entries to tenant-specific records.
     * If selectedIds is provided, only those are enabled; others are disabled.
     * If selectedIds is null, all are enabled.
     */
    private void initializeWhoSelections(Set<Long> selectedIds) {
        List<Who> globalWhoList = whoRepository.findAll();

        logger.info("Found {} global WHO records for tenant: {}",
                globalWhoList.size(), TenantContext.getCurrentTenantId());

        if (globalWhoList.isEmpty()) {
            logger.warn("No global WHO records found! Skipping tenant initialization.");
            return;
        }

        boolean enableAll = selectedIds == null || selectedIds.isEmpty();

        List<TenantWhoSelection> tenantWhoSelections = globalWhoList.stream()
                .map(w -> {
                    TenantWhoSelection tws = TenantWhoSelection.create(w);
                    if (!enableAll && !selectedIds.contains(w.getId())) {
                        tws.disable();
                    }
                    return tws;
                })
                .toList();

        tenantWhoSelectionRepository.saveAll(tenantWhoSelections);
        tenantWhoSelectionRepository.flush();

        long enabledCount = tenantWhoSelections.stream().filter(TenantWhoSelection::isEnabled).count();
        logger.info("Created {} TenantWhoSelection records ({} enabled) for tenant: {}",
                tenantWhoSelections.size(), enabledCount, TenantContext.getCurrentTenantId());
    }

    /**
     * Creates the starter FinancialCategory set for a new tenant (K1 fix).
     *
     * - Expense categories are created only for the main categories the tenant
     *   selected in the setup wizard (null/empty selection = all).
     * - Income categories are always created.
     * - Names are Turkish when the yacht flag is TR, English otherwise.
     * - IDEMPOTENT per tenant: skipped entirely if the tenant already has any
     *   category (guards against legacy /onboarding/register + /setup double-init).
     *   Uses tenant-EXPLICIT count — the Hibernate tenant filter is not active
     *   outside authenticated requests.
     */
    private void initializeStarterCategories(Set<Long> selectedMainCategoryIds, boolean turkish) {
        Long tenantId = TenantContext.getCurrentTenantId();

        if (financialCategoryRepository.countByTenantId(tenantId) > 0) {
            logger.info("Tenant {} already has categories, skipping starter set", tenantId);
            return;
        }

        // Determine which main categories are enabled for this tenant (by English name)
        List<MainCategory> globalMainCategories = mainCategoryRepository.findAll();
        boolean allSelected = selectedMainCategoryIds == null || selectedMainCategoryIds.isEmpty();
        Set<String> enabledMainNames = globalMainCategories.stream()
                .filter(mc -> allSelected || selectedMainCategoryIds.contains(mc.getId()))
                .map(MainCategory::getNameEn)
                .collect(Collectors.toSet());

        List<FinancialCategory> toCreate = new ArrayList<>();
        String description = turkish ? "Başlangıç seti" : "Starter set";
        int displayOrder = 1;

        for (SeedCategory seed : EXPENSE_SEED) {
            if (!enabledMainNames.contains(seed.mainCategoryNameEn())) {
                continue; // tenant bu ana kategoriyi seçmedi
            }
            toCreate.add(FinancialCategory.create(
                    turkish ? seed.nameTr() : seed.nameEn(),
                    seed.nameEn(),
                    RecordType.EXPENSE,
                    description,
                    displayOrder++,
                    seed.technical()
            ));
        }

        for (SeedCategory seed : INCOME_SEED) {
            toCreate.add(FinancialCategory.create(
                    turkish ? seed.nameTr() : seed.nameEn(),
                    seed.nameEn(),
                    RecordType.INCOME,
                    description,
                    displayOrder++,
                    seed.technical()
            ));
        }

        financialCategoryRepository.saveAll(toCreate);

        logger.info("Created {} starter categories ({} expense, {} income, lang={}) for tenant: {}",
                toCreate.size(),
                toCreate.stream().filter(c -> c.getCategoryType() == RecordType.EXPENSE).count(),
                toCreate.stream().filter(c -> c.getCategoryType() == RecordType.INCOME).count(),
                turkish ? "tr" : "en",
                tenantId);
    }
}