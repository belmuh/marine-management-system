package com.marine.management.shared.bootstrap;


import com.marine.management.modules.finance.application.TenantReferenceDataInitializer;
import com.marine.management.modules.finance.domain.entities.*;
import com.marine.management.modules.finance.domain.enums.*;
import com.marine.management.modules.finance.domain.vo.EntryNumber;
import com.marine.management.modules.finance.domain.vo.Money;
import com.marine.management.modules.finance.infrastructure.*;
import com.marine.management.modules.organization.domain.Organization;
import com.marine.management.modules.organization.infrastructure.OrganizationRepository;
import com.marine.management.modules.users.domain.User;
import com.marine.management.modules.users.infrastructure.UserRepository;
import com.marine.management.shared.multitenant.TenantContext;
import com.marine.management.shared.security.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

/**
 * Dev-profile demo data.
 *
 * DEMO DATA PHILOSOPHY:
 * - Geçmiş aylar: temiz, kapanmış kayıtlar (APPROVED → PAID) — raporlar dolu ve doğru görünür.
 * - Güncel açık işler: AZ sayıda (2 taslak, 3 onay bekleyen, 2 ödeme bekleyen,
 *   1 kısmi ödenmiş, 1 reddedilmiş) — onay/ödeme listeleri gerçekçi kalır, şişmez.
 * - Her gider kaydına WHO + MainCategory atanır — drill-down raporlar demo'da çalışır.
 * - Tarihler bugüne göre görelidir; demo hiç eskimez.
 * - Entry numaraları DB sequence'tan alınır (W1: bellekte sayaç → duplicate çakışması).
 */
@Component
@Profile("dev")
@Order(400)
public class DemoDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataInitializer.class);

    private static final String DEMO_ORG_NAME = "DEMO-YACHT";
    private static final String DEMO_ADMIN_EMAIL = "admin@demo.com";
    private static final String DEMO_PASSWORD = "Demo123!";

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TenantReferenceDataInitializer tenantReferenceDataInitializer;
    private final TenantMainCategoryRepository mainCategoryRepository;
    private final TenantWhoSelectionRepository tenantWhoRepository;
    private final FinancialCategoryRepository financialCategoryRepository;
    private final FinancialEntryRepository entryRepository;

    private final Random random = new Random();

    private static final String[] EXPENSE_VENDORS = {"Shell Marina", "Migros", "Teknosa", "Marina Market",
            "Marmaris Diesel", "Bodrum Ship Supply", "Turgutreis Tekne", "Fethiye Marina",
            "Göcek Provisions", "Yalıkavak Port"};
    private static final String[] INCOME_VENDORS = {"Charter Client A", "Charter Client B", "Yacht Show Prize",
            "Insurance Refund", "Equipment Resale", "Marina Sublease"};
    private static final String[] CITIES = {"Istanbul", "Marmaris", "Bodrum", "Fethiye", "Göcek", "Antalya"};
    private static final String[] COUNTRIES = {"Turkey", "Greece", "Croatia", "Italy"};

    public DemoDataInitializer(
            OrganizationRepository organizationRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            TenantReferenceDataInitializer tenantReferenceDataInitializer,
            TenantMainCategoryRepository mainCategoryRepository,
            TenantWhoSelectionRepository tenantWhoRepository,
            FinancialCategoryRepository financialCategoryRepository,
            FinancialEntryRepository entryRepository
    ) {
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tenantReferenceDataInitializer = tenantReferenceDataInitializer;
        this.mainCategoryRepository = mainCategoryRepository;
        this.tenantWhoRepository = tenantWhoRepository;
        this.financialCategoryRepository = financialCategoryRepository;
        this.entryRepository = entryRepository;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("🚀 Demo Data Initialization Started");

        try {
            // Check if already exists
            if (organizationRepository.existsByYachtName(DEMO_ORG_NAME)) {
                log.info("✓ Demo organization already exists");
                return;
            }

            // ============================================
            // STEP 1: Create Organization
            // ============================================
            Organization organization = Organization.create(
                    DEMO_ORG_NAME,
                    DEMO_ORG_NAME,
                    "TR",
                    "EUR"
            );
            organization.completeOnboarding(); // Demo org için onboarding tamamlandı sayılır
            organization = organizationRepository.save(organization);
            Long tenantId = organization.getOrganizationId();

            log.info("✓ Demo organization created with ID: {}", tenantId);

            // ============================================
            // STEP 2: Set Tenant Context
            // ============================================
            TenantContext.setCurrentTenantId(tenantId);

            // ============================================
            // STEP 3: Create Admin User
            // ============================================
            User admin = User.createWithHashedPassword(
                    DEMO_ADMIN_EMAIL,
                    "Demo",
                    "Admin",
                    passwordEncoder.encode(DEMO_PASSWORD),
                    Role.ADMIN,
                    organization
            );
            admin = userRepository.save(admin);

            log.info("✓ Admin user created: {}", admin.getEmail());

            // Audit context: AuditingEntityListener created_by_id'yi SecurityContext'ten
            // okur; startup'ta authenticated user olmadığından null kalıyordu →
            // financial_entries.created_by_id NOT NULL ihlali. Demo verisi admin'e atfedilir.
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(admin, null, admin.getAuthorities()));

            // ============================================
            // STEP 4: Initialize Reference Data (+ starter categories, TR names)
            // ============================================
            tenantReferenceDataInitializer.initializeTenantReferenceData(null, null, "TR");

            log.info("✓ Reference data initialized for tenant: {}", tenantId);

            // ============================================
            // STEP 5: Verify Reference Data
            // ============================================
            List<TenantMainCategory> allMainCategories = mainCategoryRepository.findAll();
            log.info("✓ Found {} TenantMainCategory records", allMainCategories.size());

            if (allMainCategories.isEmpty()) {
                throw new IllegalStateException("No reference data found");
            }

            // ============================================
            // STEP 6: Create Demo Users
            // ============================================
            List<User> demoUsers = createDemoUsers(organization);
            log.info("✓ Created {} additional demo users", demoUsers.size());

            // ============================================
            // STEP 7: Activate Categories
            // ============================================
            activateDemoCategories(allMainCategories);
            List<TenantMainCategory> activeMainCategories = mainCategoryRepository.findAllActiveWithMainCategory();
            log.info("✓ Activated {} main categories", activeMainCategories.size());

            // ============================================
            // STEP 8: Load Starter Categories (seeded in STEP 4)
            // Tenant-explicit sorgu: CommandLineRunner'da Hibernate tenant filtresi aktif değil.
            // ============================================
            List<FinancialCategory> financialCategories =
                    financialCategoryRepository.findByTenantIdOrderByDisplayOrderAsc(tenantId);
            log.info("✓ Loaded {} starter categories", financialCategories.size());

            // ============================================
            // STEP 9: Generate Financial Entries
            // ============================================
            if (!financialCategories.isEmpty()) {
                generateFinancialEntries(financialCategories, activeMainCategories, demoUsers, admin);
            }

            log.info("🎉 Demo data initialization completed!");
            log.info("👥 Demo Users (password: {})", DEMO_PASSWORD);
            log.info("   - {} (ADMIN)", DEMO_ADMIN_EMAIL);
            log.info("   - captain@demo.com (CAPTAIN)");
            log.info("   - manager@demo.com (MANAGER)");
            log.info("   - crew1@demo.com (CREW)");
            log.info("   - crew2@demo.com (CREW)");

        } catch (Exception e) {
            log.error("❌ Demo data initialization failed", e);
            throw new RuntimeException("Failed to initialize demo data", e);
        } finally {
            // ✅ Clear tenant + security context
            TenantContext.clear();
            SecurityContextHolder.clearContext();
        }
    }

    private List<User> createDemoUsers(Organization organization) {
        List<User> users = new ArrayList<>();
        users.add(createUser("captain@demo.com", "Demo", "Captain", Role.CAPTAIN, organization));
        users.add(createUser("manager@demo.com", "Demo", "Manager", Role.MANAGER, organization));
        users.add(createUser("crew1@demo.com", "John", "Crew", Role.CREW, organization));
        users.add(createUser("crew2@demo.com", "Jane", "Crew", Role.CREW, organization));
        return users;
    }

    private User createUser(String email, String firstName, String lastName, Role role, Organization organization) {
        return userRepository.findByEmail(email).orElseGet(() -> {
            User user = User.createWithHashedPassword(email, firstName, lastName,
                    passwordEncoder.encode(DEMO_PASSWORD), role, organization);
            return userRepository.save(user);
        });
    }

    private void activateDemoCategories(List<TenantMainCategory> categories) {
        int toActivate = Math.min(10, categories.size());
        for (int i = 0; i < toActivate; i++) {
            categories.get(i).enable();
            mainCategoryRepository.save(categories.get(i));
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // ENTRY GENERATION
    // ═══════════════════════════════════════════════════════════════

    private void generateFinancialEntries(
            List<FinancialCategory> categories,
            List<TenantMainCategory> mainCategories,
            List<User> demoUsers,
            User admin
    ) {
        log.info("💰 Generating sample financial entries...");

        User captain = demoUsers.stream()
                .filter(u -> u.getRoleEnum() == Role.CAPTAIN).findFirst().orElse(admin);
        List<TenantWhoSelection> whoSelections = tenantWhoRepository.findAllActiveWithWho();

        List<FinancialCategory> expenseCategories = categories.stream()
                .filter(c -> c.getCategoryType() == RecordType.EXPENSE)
                .toList();
        if (expenseCategories.isEmpty()) {
            expenseCategories = categories; // fallback
        }

        LocalDate today = LocalDate.now();

        // ───────────────────────────────────────────────────────────
        // 1) GEÇMİŞ AYLAR — kapanmış kayıtlar (PAID)
        //    Raporların dolu görünmesi için; açık iş listelerini şişirmez.
        // ───────────────────────────────────────────────────────────
        LocalDate firstMonth = today.minusMonths(9).withDayOfMonth(1);
        LocalDate currentMonth = today.withDayOfMonth(1);

        for (LocalDate month = firstMonth; month.isBefore(currentMonth); month = month.plusMonths(1)) {
            int entryCount = 6 + random.nextInt(5); // 6–10 / ay

            for (int i = 0; i < entryCount; i++) {
                FinancialEntry entry = createDemoExpense(
                        expenseCategories, whoSelections, mainCategories, randomDateInMonth(month));
                entry.submitAndApprove();
                payFully(entry, captain);
                entryRepository.save(entry);
            }
        }

        // ───────────────────────────────────────────────────────────
        // 2) GÜNCEL AÇIK İŞLER — az sayıda, gerçekçi
        // ───────────────────────────────────────────────────────────

        // 2 taslak (henüz submit edilmemiş)
        for (int i = 0; i < 2; i++) {
            entryRepository.save(createDemoExpense(
                    expenseCategories, whoSelections, mainCategories, recentDate(today)));
        }

        // 3 kaptan onayı bekleyen
        for (int i = 0; i < 3; i++) {
            FinancialEntry entry = createDemoExpense(
                    expenseCategories, whoSelections, mainCategories, recentDate(today));
            entry.submit();
            entryRepository.save(entry);
        }

        // 2 onaylanmış, ödeme bekleyen
        for (int i = 0; i < 2; i++) {
            FinancialEntry entry = createDemoExpense(
                    expenseCategories, whoSelections, mainCategories, recentDate(today));
            entry.submitAndApprove();
            entryRepository.save(entry);
        }

        // 1 kısmi ödenmiş
        FinancialEntry partiallyPaid = createDemoExpense(
                expenseCategories, whoSelections, mainCategories, recentDate(today));
        partiallyPaid.submitAndApprove();
        payHalf(partiallyPaid, captain);
        entryRepository.save(partiallyPaid);

        // 1 reddedilmiş
        FinancialEntry rejected = createDemoExpense(
                expenseCategories, whoSelections, mainCategories, recentDate(today));
        rejected.submit();
        rejected.reject("Amount seems incorrect, please verify");
        entryRepository.save(rejected);

        // ───────────────────────────────────────────────────────────
        // 3) GELİRLER — aylık 1-2 kapanmış + 1 güncel bekleyen
        // ───────────────────────────────────────────────────────────
        log.info("💵 Generating sample income entries...");

        List<FinancialCategory> incomeCategories = categories.stream()
                .filter(c -> c.getCategoryType() == RecordType.INCOME)
                .toList();

        if (!incomeCategories.isEmpty()) {
            for (LocalDate month = firstMonth; month.isBefore(currentMonth); month = month.plusMonths(1)) {
                int incomeCount = 1 + random.nextInt(2); // ayda 1-2 gelir
                for (int i = 0; i < incomeCount; i++) {
                    FinancialEntry income = createDemoIncome(incomeCategories, randomDateInMonth(month));
                    income.submitAndApprove();
                    payFully(income, captain);
                    entryRepository.save(income);
                }
            }

            // 1 güncel, onay bekleyen gelir
            FinancialEntry pendingIncome = createDemoIncome(incomeCategories, recentDate(today));
            pendingIncome.submit();
            entryRepository.save(pendingIncome);
        }

        log.info("✓ Generated {} total entries (expenses + incomes)", entryRepository.count());
    }

    private FinancialEntry createDemoExpense(
            List<FinancialCategory> categories,
            List<TenantWhoSelection> whoSelections,
            List<TenantMainCategory> mainCategories,
            LocalDate entryDate
    ) {
        double amount = 50 + random.nextDouble() * 2950; // 50–3000 EUR
        FinancialCategory category = categories.get(random.nextInt(categories.size()));
        TenantMainCategory mainCategory = resolveMainCategory(category, null, mainCategories);
        TenantWhoSelection who = resolveWho(mainCategory, whoSelections);
        String vendor = EXPENSE_VENDORS[random.nextInt(EXPENSE_VENDORS.length)];

        return FinancialEntry.create(
                nextEntryNumber(),
                RecordType.EXPENSE,
                category,
                Money.of(formatAmount(amount), "EUR"),
                entryDate,
                randomPaymentMethod(),
                "Demo expense - " + vendor,
                who,
                mainCategory,
                vendor,
                COUNTRIES[random.nextInt(COUNTRIES.length)],
                CITIES[random.nextInt(CITIES.length)],
                null,
                vendor,
                "EUR"
        );
    }

    private FinancialEntry createDemoIncome(List<FinancialCategory> incomeCategories, LocalDate entryDate) {
        double amount = 500 + random.nextDouble() * 19500; // 500–20000 EUR
        String vendor = INCOME_VENDORS[random.nextInt(INCOME_VENDORS.length)];

        return FinancialEntry.create(
                nextEntryNumber(),
                RecordType.INCOME,
                incomeCategories.get(random.nextInt(incomeCategories.size())),
                Money.of(formatAmount(amount), "EUR"),
                entryDate,
                random.nextBoolean() ? PaymentMethod.BANK_TRANSFER : PaymentMethod.CASH,
                "Income - " + vendor,
                null,
                null,
                vendor,
                "Turkey",
                CITIES[random.nextInt(CITIES.length)],
                null,
                null,
                "EUR"
        );
    }

    /**
     * Gider kaydının ana kategorisini, satır-kalemi kategorisinin kanonik eşlemesinden
     * çözer; böylece pivot/drill-down raporlarda kategori her zaman doğru ana kategori
     * altında toplanır (rastgele eşleme demo'yu tutarsız gösteriyordu).
     *
     * Öncelik: 1) kategori → ana kategori kanonik eşleme,
     *          2) WHO'nun önerdiği ana kategori, 3) son çare rastgele.
     */
    private TenantMainCategory resolveMainCategory(
            FinancialCategory category,
            TenantWhoSelection who,
            List<TenantMainCategory> mainCategories
    ) {
        if (mainCategories.isEmpty()) {
            return null;
        }

        // 1) Kategori → ana kategori (tutarlı ağaç)
        String mainEn = TenantReferenceDataInitializer.mainCategoryNameEnFor(category.getName());
        if (mainEn != null) {
            for (TenantMainCategory mc : mainCategories) {
                if (mainEn.equals(mc.getMainCategory().getNameEn())) {
                    return mc;
                }
            }
        }

        // 2) WHO önerisi
        if (who != null && who.getWho().getSuggestedMainCategoryId() != null) {
            Long suggestedId = who.getWho().getSuggestedMainCategoryId();
            for (TenantMainCategory mc : mainCategories) {
                if (mc.getMainCategory().getId().equals(suggestedId)) {
                    return mc;
                }
            }
        }

        // 3) Son çare: rastgele
        return mainCategories.get(random.nextInt(mainCategories.size()));
    }

    /**
     * Ana kategoriyle tutarlı bir WHO seçer: WHO'nun önerdiği ana kategori
     * (suggestedMainCategoryId) kaydın ana kategorisiyle eşleşmeli. Böylece
     * "Maintenance → Jeneratör", "Crew → Kaptan", "Fuel → Tender" gibi anlamlı
     * kırılımlar çıkar; "Visas → Tesisat" gibi saçmalıklar olmaz.
     *
     * Eşleşen WHO yoksa (ör. Sigorta, İletişim) WHO atanmaz (null) — bu kategoriler
     * kişi/sisteme bağlı olmadığından drill-down'da WHO seviyesi olmaz.
     */
    private TenantWhoSelection resolveWho(
            TenantMainCategory mainCategory,
            List<TenantWhoSelection> whoSelections
    ) {
        if (whoSelections.isEmpty() || mainCategory == null) {
            return null;
        }

        Long mainCategoryId = mainCategory.getMainCategory().getId();
        List<TenantWhoSelection> matches = whoSelections.stream()
                .filter(w -> mainCategoryId.equals(w.getWho().getSuggestedMainCategoryId()))
                .toList();

        if (matches.isEmpty()) {
            return null;
        }
        return matches.get(random.nextInt(matches.size()));
    }

    /** Tam ödeme — kayıt PAID durumuna geçer. Ödeme tarihi: kayıt tarihinden 0-5 gün sonra. */
    private void payFully(FinancialEntry entry, User recordedBy) {
        Payment payment = Payment.create(
                entry,
                entry.getBaseAmount(),
                entry.getEntryDate().plusDays(random.nextInt(6)),
                null,
                randomPaymentMethod(),
                "Demo payment",
                recordedBy
        );
        entry.addPayment(payment);
    }

    /** Yarım ödeme — kayıt PARTIALLY_PAID durumunda kalır. */
    private void payHalf(FinancialEntry entry, User recordedBy) {
        BigDecimal half = entry.getBaseAmount().getAmount()
                .divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
        Payment payment = Payment.create(
                entry,
                Money.of(half, "EUR"),
                entry.getEntryDate().plusDays(random.nextInt(6)),
                null,
                randomPaymentMethod(),
                "Demo partial payment",
                recordedBy
        );
        entry.addPayment(payment);
    }

    /** DB sequence'tan numara üretir (W1: bellekteki sayaç sonradan girilen kayıtlarla çakışıyordu). */
    private EntryNumber nextEntryNumber() {
        return EntryNumber.generate(entryRepository.getNextSequence());
    }

    /** Locale'den bağımsız tutar formatı — TR locale'de "%.2f" virgül üretir ve Money parse'ı bozulur. */
    private String formatAmount(double amount) {
        return String.format(Locale.US, "%.2f", amount);
    }

    private LocalDate randomDateInMonth(LocalDate month) {
        return month.plusDays(random.nextInt(Math.min(28, month.lengthOfMonth())));
    }

    private LocalDate recentDate(LocalDate today) {
        return today.minusDays(random.nextInt(14));
    }

    private PaymentMethod randomPaymentMethod() {
        PaymentMethod[] methods = {PaymentMethod.CASH, PaymentMethod.BANK_TRANSFER, PaymentMethod.CREDIT_CARD};
        return methods[random.nextInt(methods.length)];
    }
}
