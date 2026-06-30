package com.marine.management.shared.bootstrap;

import com.marine.management.modules.finance.application.TenantReferenceDataInitializer;
import com.marine.management.modules.finance.domain.entities.*;
import com.marine.management.modules.finance.domain.enums.*;
import com.marine.management.modules.finance.domain.vo.EntryNumber;
import com.marine.management.modules.finance.domain.vo.Money;
import com.marine.management.modules.finance.infrastructure.*;
import com.marine.management.modules.finance.infrastructure.TenantEntryCounterRepository;
import com.marine.management.modules.organization.domain.Organization;
import com.marine.management.modules.organization.infrastructure.OrganizationRepository;
import com.marine.management.modules.users.domain.User;
import com.marine.management.modules.users.infrastructure.UserRepository;
import com.marine.management.shared.multitenant.TenantContext;
import com.marine.management.shared.security.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

/**
 * Demo verisi oluşturma ve sıfırlama servisi.
 *
 * DemoDataInitializer (CommandLineRunner) bu servisi kullanarak başlangıçta veri yükler.
 * DemoAdminController ise reset() endpoint'i aracılığıyla mevcut demo datasını silip
 * yeniden oluşturur — sadece SUPER_ADMIN erişebilir.
 */
@Service
public class DemoDataService {

    private static final Logger log = LoggerFactory.getLogger(DemoDataService.class);

    public static final String DEMO_ORG_NAME = "S/Y Maritar";
    public static final String DEMO_ADMIN_EMAIL = "admin@maritar.demo";
    public static final String DEMO_PASSWORD = "Demo123!";

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TenantReferenceDataInitializer tenantReferenceDataInitializer;
    private final TenantMainCategoryRepository mainCategoryRepository;
    private final TenantWhoSelectionRepository tenantWhoRepository;
    private final FinancialCategoryRepository financialCategoryRepository;
    private final FinancialEntryRepository entryRepository;
    private final TenantEntryCounterRepository entryCounterRepository;
    private final JdbcTemplate jdbcTemplate;

    private final Random random = new Random();

    private static final String[] CITIES = {"Marmaris", "Bodrum", "Fethiye", "Göcek", "Yalıkavak", "Antalya"};
    private static final String[] CREW_NAMES = {"Luca Romano", "Marco Rossi", "Sophie Martin", "Claire Dubois"};

    private static final PaymentMethod[] BANK = {PaymentMethod.BANK_TRANSFER};
    private static final PaymentMethod[] CASH = {PaymentMethod.CASH};
    private static final PaymentMethod[] CARD_BANK = {PaymentMethod.CREDIT_CARD, PaymentMethod.BANK_TRANSFER};
    private static final PaymentMethod[] CASH_CARD = {PaymentMethod.CASH, PaymentMethod.CREDIT_CARD};
    private static final PaymentMethod[] ALL_METHODS = {PaymentMethod.CASH, PaymentMethod.BANK_TRANSFER, PaymentMethod.CREDIT_CARD};

    /**
     * Bir gider kategorisinin gerçekçi profili — kategorinin İngilizce adına (getNameEn) göre eşlenir.
     * Böylece tedarikçi, açıklama, WHO ekseni, tutar ve ödeme yöntemi birbiriyle tutarlı olur.
     */
    private record ExpenseProfile(
            String[] vendors,
            String[] descriptions,
            String[] whoEn,
            int minAmount,
            int maxAmount,
            PaymentMethod[] payments
    ) {}

    private static final ExpenseProfile FALLBACK_PROFILE = new ExpenseProfile(
            new String[]{"General Supplier", "Ship Chandler"},
            new String[]{"Miscellaneous expense", "General supplies"},
            new String[]{"Office"}, 50, 1500, ALL_METHODS);

    // Kategori (İngilizce ad) → gerçekçi profil. Özel yat (S/Y Maritar) işletme akışına göre.
    private static final Map<String, ExpenseProfile> EXPENSE_PROFILES = Map.ofEntries(
            Map.entry("Bank Charges", new ExpenseProfile(
                    new String[]{"Garanti BBVA", "İş Bankası", "Wise Business"},
                    new String[]{"Wire transfer fee", "Monthly account maintenance", "FX conversion fee"},
                    new String[]{"Office"}, 15, 250, BANK)),
            Map.entry("Management Fees", new ExpenseProfile(
                    new String[]{"Bluewater Yacht Management", "Maritar Holding Ltd."},
                    new String[]{"Monthly management fee", "Accounting & payroll service"},
                    new String[]{"Office"}, 1500, 5000, BANK)),
            Map.entry("Agency Fees, Taxes & Formalities", new ExpenseProfile(
                    new String[]{"Marmaris Yacht Agency", "Bodrum Port Agency", "Setur Marinas"},
                    new String[]{"Port clearance & formalities", "Transit log renewal", "Cruising tax payment"},
                    new String[]{"Office"}, 150, 2500, CARD_BANK)),
            Map.entry("Gratuities", new ExpenseProfile(
                    new String[]{"Marina Staff", "Fuel Dock Crew", "Dockhands"},
                    new String[]{"Dock staff gratuity", "Fuel dock tip", "Service gratuity"},
                    new String[]{"Office", "Marina"}, 50, 800, CASH)),
            Map.entry("Other Expenses", new ExpenseProfile(
                    new String[]{"Amazon", "Local Hardware Store", "Ship Chandler"},
                    new String[]{"Miscellaneous supplies", "Courier & shipping", "Office consumables"},
                    new String[]{"Office"}, 50, 1500, ALL_METHODS)),
            Map.entry("Travel & Transfers", new ExpenseProfile(
                    new String[]{"Turkish Airlines", "BizJet Transfer", "Marmaris VIP Transfer"},
                    new String[]{"Crew flight", "Owner airport transfer", "Guest transfer"},
                    new String[]{"Crew", "Owner", "Guest"}, 100, 3000, CARD_BANK)),
            Map.entry("Internet & Phones", new ExpenseProfile(
                    new String[]{"Starlink Maritime", "Turkcell", "Vodafone TR"},
                    new String[]{"Starlink maritime subscription", "Crew SIM & data", "Satellite airtime"},
                    new String[]{"Office"}, 80, 1200, CARD_BANK)),
            Map.entry("Crew Wages", new ExpenseProfile(
                    new String[]{"Payroll"},
                    new String[]{"Monthly salary"},
                    new String[]{"Crew", "Captain"}, 1800, 4500, BANK)),
            Map.entry("Medical Expenses", new ExpenseProfile(
                    new String[]{"Marmaris Pharmacy", "Bodrum Private Hospital", "Medline Clinic"},
                    new String[]{"Crew medical check-up", "First-aid supplies restock", "Guest medical assistance"},
                    new String[]{"Captain", "Crew", "Guest"}, 50, 1500, CARD_BANK)),
            Map.entry("Uniforms", new ExpenseProfile(
                    new String[]{"Marinepool", "Helly Hansen Store", "Crew Outfitters"},
                    new String[]{"Crew polo shirts & shorts", "Foul-weather gear", "Deck shoes"},
                    new String[]{"Crew"}, 100, 1500, CARD_BANK)),
            Map.entry("Visas", new ExpenseProfile(
                    new String[]{"Schengen Visa Service", "Marmaris Yacht Agency"},
                    new String[]{"Crew Schengen visa", "Visa renewal & courier"},
                    new String[]{"Crew", "Captain"}, 80, 700, CARD_BANK)),
            Map.entry("Insurance Policies", new ExpenseProfile(
                    new String[]{"Pantaenius Yacht Insurance", "MS Amlin", "Allianz Marine"},
                    new String[]{"Hull & machinery premium", "Crew medical insurance", "P&I liability premium"},
                    new String[]{"Hull", "Crew"}, 1200, 7000, BANK)),
            Map.entry("Dockage & Marina", new ExpenseProfile(
                    new String[]{"D-Marin Göcek", "Yalıkavak Marina", "Netsel Marmaris Marina", "Setur Kuşadası"},
                    new String[]{"Monthly berth fee", "Overnight mooring", "Winter berth contract"},
                    new String[]{"Marina"}, 400, 3500, CARD_BANK)),
            Map.entry("Water & Electricity", new ExpenseProfile(
                    new String[]{"D-Marin Göcek", "Yalıkavak Marina", "Netsel Marmaris Marina"},
                    new String[]{"Shore power & water", "Marina utilities"},
                    new String[]{"Marina"}, 80, 700, CARD_BANK)),
            Map.entry("Fuel & Lubricants", new ExpenseProfile(
                    new String[]{"Shell Marina Marmaris", "Opet Marina Bodrum", "Marmaris Diesel", "BP Marine"},
                    new String[]{"Main engine diesel bunkering", "Generator fuel top-up", "Tender petrol refill", "Lubricant oil change"},
                    new String[]{"Main Engine", "Generator", "Tender", "Jetski"}, 800, 6000, CARD_BANK)),
            Map.entry("Repairs & Spare Parts", new ExpenseProfile(
                    new String[]{"Bodrum Ship Supply", "Turgutreis Tekne Bakım", "Intercontinental Ship Stores", "Marmaris Marine Service"},
                    new String[]{"Service & spare parts", "Scheduled maintenance", "Equipment repair"},
                    new String[]{"Generator", "AC System", "Watermaker", "Electrical", "Hull", "Main Engine", "Electronics", "Plumbing"}, 300, 5000, CARD_BANK)),
            Map.entry("Food & Beverages", new ExpenseProfile(
                    new String[]{"Migros", "Marina Market Bodrum", "Göcek Provisions", "Macrocenter"},
                    new String[]{"Weekly provisions", "Guest catering supplies", "Fresh produce & beverages", "Owner cabin stocking"},
                    new String[]{"Crew", "Guest", "Owner"}, 150, 1500, CASH_CARD))
    );

    public DemoDataService(
            OrganizationRepository organizationRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            TenantReferenceDataInitializer tenantReferenceDataInitializer,
            TenantMainCategoryRepository mainCategoryRepository,
            TenantWhoSelectionRepository tenantWhoRepository,
            FinancialCategoryRepository financialCategoryRepository,
            FinancialEntryRepository entryRepository,
            TenantEntryCounterRepository entryCounterRepository,
            JdbcTemplate jdbcTemplate
    ) {
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tenantReferenceDataInitializer = tenantReferenceDataInitializer;
        this.mainCategoryRepository = mainCategoryRepository;
        this.tenantWhoRepository = tenantWhoRepository;
        this.financialCategoryRepository = financialCategoryRepository;
        this.entryRepository = entryRepository;
        this.entryCounterRepository = entryCounterRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    // ═══════════════════════════════════════════════════════════════════
    // PUBLIC API
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Demo data varsa atlar (idempotent). DemoDataInitializer tarafından çağrılır.
     */
    @Transactional
    public void initialize() {
        if (organizationRepository.existsByYachtName(DEMO_ORG_NAME)) {
            log.info("✓ Demo organization already exists, skipping");
            return;
        }
        doCreate();
    }

    /**
     * Mevcut demo datasını tamamen siler ve yeniden oluşturur.
     * Sadece SUPER_ADMIN tarafından tetiklenir (DemoAdminController).
     */
    @Transactional
    public void reset() {
        log.info("🔄 Demo data reset başlatıldı...");
        deleteDemoTenant();
        doCreate();
        log.info("✅ Demo data reset tamamlandı.");
    }

    // ═══════════════════════════════════════════════════════════════════
    // PRIVATE: DELETE
    // ═══════════════════════════════════════════════════════════════════

    private void deleteDemoTenant() {
        organizationRepository.findByYachtName(DEMO_ORG_NAME).ifPresent(org -> {
            Long tenantId = org.getOrganizationId();
            log.info("🗑️ Demo tenant siliniyor (id={})", tenantId);

            // FK sırasına göre sil
            jdbcTemplate.update("DELETE FROM financial_entries_aud WHERE id IN (SELECT id FROM financial_entries WHERE tenant_id = ?)", tenantId);
            jdbcTemplate.update("DELETE FROM entry_approvals WHERE tenant_id = ?", tenantId);
            jdbcTemplate.update("DELETE FROM payments WHERE tenant_id = ?", tenantId);
            jdbcTemplate.update("DELETE FROM financial_entry_attachments WHERE entry_id IN (SELECT id FROM financial_entries WHERE tenant_id = ?)", tenantId);
            jdbcTemplate.update("DELETE FROM financial_entries WHERE tenant_id = ?", tenantId);
            jdbcTemplate.update("DELETE FROM financial_categories WHERE tenant_id = ?", tenantId);
            jdbcTemplate.update("DELETE FROM tenant_main_categories WHERE tenant_id = ?", tenantId);
            jdbcTemplate.update("DELETE FROM tenant_who_selections WHERE tenant_id = ?", tenantId);
            jdbcTemplate.update("DELETE FROM tenant_entry_counter WHERE tenant_id = ?", tenantId);
            jdbcTemplate.update("DELETE FROM refresh_tokens WHERE user_id IN (SELECT id FROM users WHERE organization_id = ?)", tenantId);
            jdbcTemplate.update("DELETE FROM users WHERE organization_id = ?", tenantId);
            jdbcTemplate.update("DELETE FROM organizations WHERE id = ?", tenantId);

            log.info("✓ Demo tenant silindi");
        });
    }

    // ═══════════════════════════════════════════════════════════════════
    // PRIVATE: CREATE
    // ═══════════════════════════════════════════════════════════════════

    private void doCreate() {
        try {
            // STEP 1: Organization
            Organization organization = Organization.create(DEMO_ORG_NAME, DEMO_ORG_NAME, "TR", "EUR");
            organization.completeOnboarding();
            organization.enableManagerApproval(new BigDecimal("500.00"));
            organization = organizationRepository.save(organization);
            Long tenantId = organization.getOrganizationId();
            log.info("✓ Demo organization oluşturuldu (id={})", tenantId);

            // STEP 2: Tenant context
            TenantContext.setCurrentTenantId(tenantId);

            // STEP 3: Admin user
            User admin = User.createWithHashedPassword(
                    DEMO_ADMIN_EMAIL, "James", "Harrison",
                    passwordEncoder.encode(DEMO_PASSWORD), Role.ADMIN, organization
            );
            admin = userRepository.save(admin);
            log.info("✓ Admin user oluşturuldu: {}", admin.getEmail());

            // Security context — audit created_by_id için gerekli
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(admin, null, admin.getAuthorities()));

            // STEP 4: Reference data
            tenantReferenceDataInitializer.initializeTenantReferenceData(null, null, "TR");
            log.info("✓ Reference data hazır");

            // STEP 5: Demo users
            List<User> demoUsers = createDemoUsers(organization);
            log.info("✓ {} demo kullanıcı oluşturuldu", demoUsers.size());

            // STEP 6: Categories
            List<TenantMainCategory> allMainCategories = mainCategoryRepository.findAll();
            if (allMainCategories.isEmpty()) throw new IllegalStateException("No reference data found");
            activateDemoCategories(allMainCategories);
            List<TenantMainCategory> activeMainCategories = mainCategoryRepository.findAllActiveWithMainCategory();
            log.info("✓ {} ana kategori aktif", activeMainCategories.size());

            // STEP 7: Financial entries
            List<FinancialCategory> financialCategories =
                    financialCategoryRepository.findByTenantIdOrderByDisplayOrderAsc(tenantId);
            log.info("✓ {} starter kategori yüklendi", financialCategories.size());

            if (!financialCategories.isEmpty()) {
                generateFinancialEntries(financialCategories, activeMainCategories, demoUsers, admin);
            }

            log.info("🎉 Demo data hazır!");
            log.info("👥 Demo Users (password: {})", DEMO_PASSWORD);
            log.info("   - {} (ADMIN / James Harrison)", DEMO_ADMIN_EMAIL);
            log.info("   - captain@maritar.demo (CAPTAIN / Luca Romano)");
            log.info("   - manager@maritar.demo (MANAGER / Claire Dubois)");
            log.info("   - crew1@maritar.demo (CREW / Marco Rossi)");
            log.info("   - crew2@maritar.demo (CREW / Sophie Martin)");

        } catch (Exception e) {
            log.error("❌ Demo data oluşturma başarısız", e);
            throw new RuntimeException("Demo data oluşturma başarısız", e);
        } finally {
            TenantContext.clear();
            SecurityContextHolder.clearContext();
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // PRIVATE: HELPERS
    // ═══════════════════════════════════════════════════════════════════

    private List<User> createDemoUsers(Organization organization) {
        List<User> users = new ArrayList<>();
        users.add(createUser("captain@maritar.demo", "Luca", "Romano", Role.CAPTAIN, organization));
        users.add(createUser("manager@maritar.demo", "Claire", "Dubois", Role.MANAGER, organization));
        users.add(createUser("crew1@maritar.demo", "Marco", "Rossi", Role.CREW, organization));
        users.add(createUser("crew2@maritar.demo", "Sophie", "Martin", Role.CREW, organization));
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

    private void generateFinancialEntries(
            List<FinancialCategory> categories,
            List<TenantMainCategory> mainCategories,
            List<User> demoUsers,
            User admin
    ) {
        log.info("💰 Finansal kayıtlar oluşturuluyor...");

        User captain = demoUsers.stream()
                .filter(u -> u.getRoleEnum() == Role.CAPTAIN).findFirst().orElse(admin);
        List<TenantWhoSelection> whoSelections = tenantWhoRepository.findAllActiveWithWho();

        List<FinancialCategory> expenseCategories = categories.stream()
                .filter(c -> c.getCategoryType() == RecordType.EXPENSE).toList();
        if (expenseCategories.isEmpty()) expenseCategories = categories;

        LocalDate today = LocalDate.now();
        LocalDate firstMonth = today.minusMonths(9).withDayOfMonth(1);
        LocalDate currentMonth = today.withDayOfMonth(1);
        int insuranceMonth = firstMonth.getMonthValue() % 3; // kabaca üç ayda bir sigorta

        // ── Geçmiş aylar: bir kaptanın normal işletme akışı (tekrarlayan demirler + ekstralar) ──
        for (LocalDate month = firstMonth; month.isBefore(currentMonth); month = month.plusMonths(1)) {
            // Marina bağlama — her ay
            closeAndPay(expenseFor("Dockage & Marina", expenseCategories, whoSelections, mainCategories, randomDateInMonth(month)), captain);
            closeAndPay(expenseFor("Water & Electricity", expenseCategories, whoSelections, mainCategories, randomDateInMonth(month)), captain);
            // Mürettebat maaşları — her ay, mürettebat başına
            for (int i = 0; i < 2 + random.nextInt(2); i++) {
                closeAndPay(expenseFor("Crew Wages", expenseCategories, whoSelections, mainCategories, month.withDayOfMonth(Math.min(28, 1 + random.nextInt(3)))), captain);
            }
            // Yakıt — her ay 1-2 kez
            for (int i = 0; i < 1 + random.nextInt(2); i++) {
                closeAndPay(expenseFor("Fuel & Lubricants", expenseCategories, whoSelections, mainCategories, randomDateInMonth(month)), captain);
            }
            // Erzak — her ay 1-2 kez
            for (int i = 0; i < 1 + random.nextInt(2); i++) {
                closeAndPay(expenseFor("Food & Beverages", expenseCategories, whoSelections, mainCategories, randomDateInMonth(month)), captain);
            }
            // İletişim — her ay
            closeAndPay(expenseFor("Internet & Phones", expenseCategories, whoSelections, mainCategories, randomDateInMonth(month)), captain);
            // Sigorta — üç ayda bir
            if (month.getMonthValue() % 3 == insuranceMonth) {
                closeAndPay(expenseFor("Insurance Policies", expenseCategories, whoSelections, mainCategories, randomDateInMonth(month)), captain);
            }
            // Rastgele ekstralar (bakım, bahşiş, sağlık, üniforma, acente vb.)
            for (int i = 0; i < 2 + random.nextInt(3); i++) {
                closeAndPay(createDemoExpense(expenseCategories, whoSelections, mainCategories, randomDateInMonth(month)), captain);
            }
        }

        // ── Güncel açık işler (çeşitli durumlar) ──
        for (int i = 0; i < 2; i++) entryRepository.save(createDemoExpense(expenseCategories, whoSelections, mainCategories, recentDate(today)));
        for (int i = 0; i < 2; i++) { FinancialEntry e = createDemoExpense(expenseCategories, whoSelections, mainCategories, recentDate(today)); e.submit(); entryRepository.save(e); }
        for (int i = 0; i < 2; i++) { FinancialEntry e = createLargeExpense(expenseCategories, whoSelections, mainCategories, recentDate(today)); e.submit(); e.approveByCaptain(true); entryRepository.save(e); }
        for (int i = 0; i < 2; i++) { FinancialEntry e = createDemoExpense(expenseCategories, whoSelections, mainCategories, recentDate(today)); e.submitAndApprove(); entryRepository.save(e); }

        FinancialEntry partial = createDemoExpense(expenseCategories, whoSelections, mainCategories, recentDate(today));
        partial.submitAndApprove(); payHalf(partial, captain); entryRepository.save(partial);

        FinancialEntry rejected = createDemoExpense(expenseCategories, whoSelections, mainCategories, recentDate(today));
        rejected.submit(); rejected.reject("Amount seems incorrect, please verify"); entryRepository.save(rejected);

        // ── Gelirler: özel yat → ağırlıklı Sahip Fonu, ara sıra Diğer Gelir ──
        List<FinancialCategory> incomeCategories = categories.stream()
                .filter(c -> c.getCategoryType() == RecordType.INCOME).toList();
        if (!incomeCategories.isEmpty()) {
            for (LocalDate month = firstMonth; month.isBefore(currentMonth); month = month.plusMonths(1)) {
                FinancialEntry income = createOwnerFunds(incomeCategories, month.withDayOfMonth(Math.min(28, 1 + random.nextInt(5))));
                income.submitAndApprove(); payFully(income, captain); entryRepository.save(income);
            }
            for (int i = 0; i < 3; i++) {
                FinancialEntry other = createOtherIncome(incomeCategories, randomDateInMonth(firstMonth.plusMonths(random.nextInt(9))));
                other.submitAndApprove(); payFully(other, captain); entryRepository.save(other);
            }
            FinancialEntry pendingIncome = createOwnerFunds(incomeCategories, recentDate(today));
            pendingIncome.submit(); entryRepository.save(pendingIncome);
        }

        log.info("✓ Toplam {} kayıt oluşturuldu", entryRepository.count());
    }

    /** Kapanmış (onaylı + tam ödenmiş) gider kaydını kaydeder. */
    private void closeAndPay(FinancialEntry entry, User captain) {
        entry.submitAndApprove();
        payFully(entry, captain);
        entryRepository.save(entry);
    }

    /** Belirli bir kategori için profiline göre gider üretir; kategori yoksa rastgele. */
    private FinancialEntry expenseFor(String categoryNameEn, List<FinancialCategory> categories, List<TenantWhoSelection> whoSelections, List<TenantMainCategory> mainCategories, LocalDate entryDate) {
        FinancialCategory category = categories.stream()
                .filter(c -> categoryNameEn.equals(c.getNameEn())).findFirst().orElse(null);
        if (category == null) return createDemoExpense(categories, whoSelections, mainCategories, entryDate);
        ExpenseProfile profile = EXPENSE_PROFILES.getOrDefault(categoryNameEn, FALLBACK_PROFILE);
        return buildExpense(category, profile, whoSelections, mainCategories, entryDate, profile.minAmount(), profile.maxAmount());
    }

    private FinancialEntry createDemoExpense(List<FinancialCategory> categories, List<TenantWhoSelection> whoSelections, List<TenantMainCategory> mainCategories, LocalDate entryDate) {
        FinancialCategory category = categories.get(random.nextInt(categories.size()));
        ExpenseProfile profile = EXPENSE_PROFILES.getOrDefault(category.getNameEn(), FALLBACK_PROFILE);
        return buildExpense(category, profile, whoSelections, mainCategories, entryDate, profile.minAmount(), profile.maxAmount());
    }

    private FinancialEntry createLargeExpense(List<FinancialCategory> categories, List<TenantWhoSelection> whoSelections, List<TenantMainCategory> mainCategories, LocalDate entryDate) {
        // Yönetici onayı eşiğini (500 EUR) aşan yüksek tutarlı kategorilerden seç
        String[] highValue = {"Fuel & Lubricants", "Repairs & Spare Parts", "Insurance Policies", "Dockage & Marina"};
        String pick = highValue[random.nextInt(highValue.length)];
        FinancialCategory category = categories.stream()
                .filter(c -> pick.equals(c.getNameEn())).findFirst()
                .orElse(categories.get(random.nextInt(categories.size())));
        ExpenseProfile profile = EXPENSE_PROFILES.getOrDefault(category.getNameEn(), FALLBACK_PROFILE);
        int min = Math.max(800, profile.minAmount());
        int max = Math.max(min + 500, profile.maxAmount());
        return buildExpense(category, profile, whoSelections, mainCategories, entryDate, min, max);
    }

    private FinancialEntry buildExpense(FinancialCategory category, ExpenseProfile profile, List<TenantWhoSelection> whoSelections, List<TenantMainCategory> mainCategories, LocalDate entryDate, int min, int max) {
        double amount = min + random.nextDouble() * Math.max(1, max - min);
        TenantMainCategory mainCategory = resolveMainCategory(category, mainCategories);
        TenantWhoSelection who = resolveWhoByName(profile.whoEn(), whoSelections);
        String whoEn = (who != null && who.getWho() != null) ? who.getWho().getNameEn() : null;
        String vendor = pick(profile.vendors());
        String description = describe(category.getNameEn(), profile, whoEn);
        String recipient = vendor;

        if ("Crew Wages".equals(category.getNameEn())) {
            recipient = pick(CREW_NAMES);
            description = "Monthly salary – " + recipient;
            vendor = "Payroll";
        }

        return FinancialEntry.create(nextEntryNumber(), RecordType.EXPENSE, category,
                Money.of(formatAmount(amount), "EUR"), entryDate, pick(profile.payments()),
                description, who, mainCategory, recipient, "Turkey", pick(CITIES), null, vendor, "EUR");
    }

    /** Yakıt/onarım kategorilerinde açıklamayı seçilen WHO ile hizalar; aksi halde profil açıklamasından seçer. */
    private String describe(String categoryNameEn, ExpenseProfile profile, String whoEn) {
        if ("Fuel & Lubricants".equals(categoryNameEn) && whoEn != null) {
            return switch (whoEn) {
                case "Main Engine" -> "Main engine diesel bunkering – " + (300 + random.nextInt(1700)) + " L";
                case "Generator" -> "Generator fuel top-up";
                case "Tender" -> "Tender petrol refill";
                case "Jetski" -> "Jetski fuel";
                default -> pick(profile.descriptions());
            };
        }
        if ("Repairs & Spare Parts".equals(categoryNameEn) && whoEn != null) {
            return switch (whoEn) {
                case "Generator" -> "Generator service & spares";
                case "AC System" -> "AC compressor repair";
                case "Watermaker" -> "Watermaker membrane replacement";
                case "Electrical" -> "Electrical system repair";
                case "Hull" -> "Antifouling & hull cleaning";
                case "Main Engine" -> "Main engine overhaul";
                case "Electronics" -> "Navigation electronics calibration";
                case "Plumbing" -> "Plumbing repair";
                default -> pick(profile.descriptions());
            };
        }
        return pick(profile.descriptions());
    }

    private FinancialEntry createOwnerFunds(List<FinancialCategory> incomeCategories, LocalDate entryDate) {
        FinancialCategory category = findIncome(incomeCategories, "Owner's Funds");
        double amount = 15000 + random.nextDouble() * 30000;
        return FinancialEntry.create(nextEntryNumber(), RecordType.INCOME, category,
                Money.of(formatAmount(amount), "EUR"), entryDate, PaymentMethod.BANK_TRANSFER,
                "Owner capital transfer", null, null, "Maritar Holding Ltd.", "Turkey", "İstanbul", null, "Maritar Holding Ltd.", "EUR");
    }

    private FinancialEntry createOtherIncome(List<FinancialCategory> incomeCategories, LocalDate entryDate) {
        FinancialCategory category = findIncome(incomeCategories, "Other Income");
        double amount = 500 + random.nextDouble() * 4500;
        String[] descs = {"Insurance claim reimbursement", "Fuel deposit refund", "Returned guest deposit"};
        String desc = pick(descs);
        return FinancialEntry.create(nextEntryNumber(), RecordType.INCOME, category,
                Money.of(formatAmount(amount), "EUR"), entryDate, PaymentMethod.BANK_TRANSFER,
                desc, null, null, desc, "Turkey", pick(CITIES), null, desc, "EUR");
    }

    private FinancialCategory findIncome(List<FinancialCategory> incomeCategories, String nameEn) {
        return incomeCategories.stream()
                .filter(c -> nameEn.equals(c.getNameEn())).findFirst()
                .orElse(incomeCategories.get(0));
    }

    private <T> T pick(T[] array) {
        return array[random.nextInt(array.length)];
    }

    private TenantMainCategory resolveMainCategory(FinancialCategory category, List<TenantMainCategory> mainCategories) {
        if (mainCategories.isEmpty()) return null;
        String mainEn = TenantReferenceDataInitializer.mainCategoryNameEnFor(category.getName());
        if (mainEn != null) {
            for (TenantMainCategory mc : mainCategories) {
                if (mainEn.equals(mc.getMainCategory().getNameEn())) return mc;
            }
        }
        return mainCategories.get(random.nextInt(mainCategories.size()));
    }

    /** Profilin belirttiği WHO İngilizce adlarından, tenant'ta aktif olanlar arasından birini seçer. */
    private TenantWhoSelection resolveWhoByName(String[] whoEn, List<TenantWhoSelection> whoSelections) {
        if (whoSelections.isEmpty() || whoEn == null || whoEn.length == 0) return null;
        List<String> wanted = Arrays.asList(whoEn);
        List<TenantWhoSelection> matches = whoSelections.stream()
                .filter(w -> w.getWho() != null && wanted.contains(w.getWho().getNameEn())).toList();
        return matches.isEmpty() ? null : matches.get(random.nextInt(matches.size()));
    }

    private void payFully(FinancialEntry entry, User recordedBy) {
        Payment payment = Payment.create(entry, entry.getBaseAmount(), entry.getEntryDate().plusDays(random.nextInt(6)), null, randomPaymentMethod(), null, recordedBy);
        entry.addPayment(payment);
    }

    private void payHalf(FinancialEntry entry, User recordedBy) {
        BigDecimal half = entry.getBaseAmount().getAmount().divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
        Payment payment = Payment.create(entry, Money.of(half, "EUR"), entry.getEntryDate().plusDays(random.nextInt(6)), null, randomPaymentMethod(), null, recordedBy);
        entry.addPayment(payment);
    }

    private EntryNumber nextEntryNumber() {
        Long tenantId = TenantContext.getCurrentTenantId();
        int year = java.time.Year.now().getValue();
        return EntryNumber.generate(entryCounterRepository.nextSequence(tenantId, year));
    }

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
