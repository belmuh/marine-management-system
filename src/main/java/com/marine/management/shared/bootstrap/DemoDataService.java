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

    private static final String[] EXPENSE_VENDORS = {
            "Shell Marina Marmaris", "Migros", "Marina Market Bodrum",
            "Marmaris Diesel", "Bodrum Ship Supply", "Turgutreis Tekne Bakım",
            "Fethiye Marina", "Göcek Provisions", "Yalıkavak Port",
            "Intercontinental Ship Stores"
    };
    private static final String[] INCOME_VENDORS = {
            "Oceanview Charters GmbH", "Blue Voyage S.A.",
            "Mediterranean Sailing Club", "Adriatic Dream Ltd.",
            "Aegean Escapes Ltd.", "Portofino Yacht Rentals"
    };
    private static final String[] CITIES = {"İstanbul", "Marmaris", "Bodrum", "Fethiye", "Göcek", "Antalya"};
    private static final String[] COUNTRIES = {"Turkey", "Greece", "Croatia", "Italy"};

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

        // Geçmiş aylar — kapanmış kayıtlar
        for (LocalDate month = firstMonth; month.isBefore(currentMonth); month = month.plusMonths(1)) {
            int entryCount = 6 + random.nextInt(5);
            for (int i = 0; i < entryCount; i++) {
                FinancialEntry entry = createDemoExpense(expenseCategories, whoSelections, mainCategories, randomDateInMonth(month));
                entry.submitAndApprove();
                payFully(entry, captain);
                entryRepository.save(entry);
            }
        }

        // Güncel açık işler
        for (int i = 0; i < 2; i++) entryRepository.save(createDemoExpense(expenseCategories, whoSelections, mainCategories, recentDate(today)));
        for (int i = 0; i < 2; i++) { FinancialEntry e = createDemoExpense(expenseCategories, whoSelections, mainCategories, recentDate(today)); e.submit(); entryRepository.save(e); }
        for (int i = 0; i < 2; i++) { FinancialEntry e = createLargeExpense(expenseCategories, whoSelections, mainCategories, recentDate(today)); e.submit(); e.approveByCaptain(true); entryRepository.save(e); }
        for (int i = 0; i < 2; i++) { FinancialEntry e = createDemoExpense(expenseCategories, whoSelections, mainCategories, recentDate(today)); e.submitAndApprove(); entryRepository.save(e); }

        FinancialEntry partial = createDemoExpense(expenseCategories, whoSelections, mainCategories, recentDate(today));
        partial.submitAndApprove(); payHalf(partial, captain); entryRepository.save(partial);

        FinancialEntry rejected = createDemoExpense(expenseCategories, whoSelections, mainCategories, recentDate(today));
        rejected.submit(); rejected.reject("Amount seems incorrect, please verify"); entryRepository.save(rejected);

        // Gelirler
        List<FinancialCategory> incomeCategories = categories.stream()
                .filter(c -> c.getCategoryType() == RecordType.INCOME).toList();
        if (!incomeCategories.isEmpty()) {
            for (LocalDate month = firstMonth; month.isBefore(currentMonth); month = month.plusMonths(1)) {
                int incomeCount = 1 + random.nextInt(2);
                for (int i = 0; i < incomeCount; i++) {
                    FinancialEntry income = createDemoIncome(incomeCategories, randomDateInMonth(month));
                    income.submitAndApprove(); payFully(income, captain); entryRepository.save(income);
                }
            }
            FinancialEntry pendingIncome = createDemoIncome(incomeCategories, recentDate(today));
            pendingIncome.submit(); entryRepository.save(pendingIncome);
        }

        log.info("✓ Toplam {} kayıt oluşturuldu", entryRepository.count());
    }

    private FinancialEntry createDemoExpense(List<FinancialCategory> categories, List<TenantWhoSelection> whoSelections, List<TenantMainCategory> mainCategories, LocalDate entryDate) {
        double amount = 50 + random.nextDouble() * 2950;
        FinancialCategory category = categories.get(random.nextInt(categories.size()));
        TenantMainCategory mainCategory = resolveMainCategory(category, mainCategories);
        TenantWhoSelection who = resolveWho(mainCategory, whoSelections);
        String vendor = EXPENSE_VENDORS[random.nextInt(EXPENSE_VENDORS.length)];
        return FinancialEntry.create(nextEntryNumber(), RecordType.EXPENSE, category, Money.of(formatAmount(amount), "EUR"), entryDate, randomPaymentMethod(), vendor, who, mainCategory, vendor, COUNTRIES[random.nextInt(COUNTRIES.length)], CITIES[random.nextInt(CITIES.length)], null, vendor, "EUR");
    }

    private FinancialEntry createLargeExpense(List<FinancialCategory> categories, List<TenantWhoSelection> whoSelections, List<TenantMainCategory> mainCategories, LocalDate entryDate) {
        double amount = 800 + random.nextDouble() * 2200;
        FinancialCategory category = categories.get(random.nextInt(categories.size()));
        TenantMainCategory mainCategory = resolveMainCategory(category, mainCategories);
        TenantWhoSelection who = resolveWho(mainCategory, whoSelections);
        String vendor = EXPENSE_VENDORS[random.nextInt(EXPENSE_VENDORS.length)];
        return FinancialEntry.create(nextEntryNumber(), RecordType.EXPENSE, category, Money.of(formatAmount(amount), "EUR"), entryDate, randomPaymentMethod(), vendor, who, mainCategory, vendor, COUNTRIES[random.nextInt(COUNTRIES.length)], CITIES[random.nextInt(CITIES.length)], null, vendor, "EUR");
    }

    private FinancialEntry createDemoIncome(List<FinancialCategory> incomeCategories, LocalDate entryDate) {
        double amount = 500 + random.nextDouble() * 19500;
        String vendor = INCOME_VENDORS[random.nextInt(INCOME_VENDORS.length)];
        return FinancialEntry.create(nextEntryNumber(), RecordType.INCOME, incomeCategories.get(random.nextInt(incomeCategories.size())), Money.of(formatAmount(amount), "EUR"), entryDate, random.nextBoolean() ? PaymentMethod.BANK_TRANSFER : PaymentMethod.CASH, vendor, null, null, vendor, "Turkey", CITIES[random.nextInt(CITIES.length)], null, null, "EUR");
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

    private TenantWhoSelection resolveWho(TenantMainCategory mainCategory, List<TenantWhoSelection> whoSelections) {
        if (whoSelections.isEmpty() || mainCategory == null) return null;
        Long mainCategoryId = mainCategory.getMainCategory().getId();
        List<TenantWhoSelection> matches = whoSelections.stream()
                .filter(w -> mainCategoryId.equals(w.getWho().getSuggestedMainCategoryId())).toList();
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
