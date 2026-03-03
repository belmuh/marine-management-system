package com.marine.management.shared.bootstrap;


import com.marine.management.modules.finance.application.ApprovalService;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Component // ← Component olarak kaydet
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
    private final FinancialCategoryRepository financialCategoryRepository;
    private final FinancialEntryRepository entryRepository;
    private final ApprovalService approvalService;

    private final Random random = new Random();

    public DemoDataInitializer(
            OrganizationRepository organizationRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            TenantReferenceDataInitializer tenantReferenceDataInitializer,
            TenantMainCategoryRepository mainCategoryRepository,
            FinancialCategoryRepository financialCategoryRepository,
            FinancialEntryRepository entryRepository,
            ApprovalService approvalService
    ) {
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tenantReferenceDataInitializer = tenantReferenceDataInitializer;
        this.mainCategoryRepository = mainCategoryRepository;
        this.financialCategoryRepository = financialCategoryRepository;
        this.entryRepository = entryRepository;
        this.approvalService = approvalService;
    }

    @Override // ← @PostConstruct yerine @Override
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

            // ============================================
            // STEP 4: Initialize Reference Data
            // ============================================
            tenantReferenceDataInitializer.initializeTenantReferenceData();

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
            // STEP 8: Create Financial Categories
            // ============================================
            List<FinancialCategory> financialCategories = createFinancialCategories(activeMainCategories);
            log.info("✓ Created {} financial categories", financialCategories.size());

            // ============================================
            // STEP 9: Generate Financial Entries
            // ============================================
            if (!financialCategories.isEmpty()) {
                generateFinancialEntries(financialCategories, demoUsers, admin);
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
            // ✅ Clear tenant context
            TenantContext.clear();
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

    private List<FinancialCategory> createFinancialCategories(List<TenantMainCategory> activeMainCategories) {
        List<FinancialCategory> categories = new ArrayList<>();
        String[][] suffixes = {{"Regular", "Special"}, {"Monthly", "Quarterly"}};

        for (TenantMainCategory mainCategory : activeMainCategories) {
            for (int i = 0; i < 2; i++) {
                String suffix = suffixes[i % 2][random.nextInt(suffixes[i % 2].length)];
                String code = mainCategory.getMainCategory().getCode() + "_" + suffix.toUpperCase();
                String name = mainCategory.getMainCategory().getNameEn() + " - " + suffix;

                Optional<FinancialCategory> existing = financialCategoryRepository.findByCode(code);
                if (existing.isEmpty()) {
                    categories.add(financialCategoryRepository.save(FinancialCategory.create(
                            code, name, RecordType.EXPENSE, "Demo: " + name, i + 1, true)));
                } else {
                    categories.add(existing.get());
                }
            }
        }
        return categories;
    }

    private void generateFinancialEntries(List<FinancialCategory> categories, List<User> demoUsers, User admin) {
        log.info("💰 Generating sample financial entries...");

        // Kullanıcıları role'e göre ayır
        User captain = demoUsers.stream().filter(u -> u.getRoleEnum() == Role.CAPTAIN).findFirst().orElse(admin);
        User manager = demoUsers.stream().filter(u -> u.getRoleEnum() == Role.MANAGER).findFirst().orElse(admin);
        List<User> crewMembers = demoUsers.stream().filter(u -> u.getRoleEnum() == Role.CREW).toList();

        LocalDate start = LocalDate.of(2025, 8, 1);
        LocalDate end = LocalDate.of(2026, 2, 28);
        int counter = 1;

        String[] expenseVendors = {"Shell Marina", "Migros", "Teknosa", "Marina Market", "Marmaris Diesel",
                "Bodrum Ship Supply", "Turgutreis Tekne", "Fethiye Marina", "Göcek Provisions", "Yalıkavak Port"};
        String[] incomeVendors = {"Charter Client A", "Charter Client B", "Yacht Show Prize",
                "Insurance Refund", "Equipment Resale", "Marina Sublease"};
        String[] cities = {"Istanbul", "Marmaris", "Bodrum", "Fethiye", "Göcek", "Antalya"};
        String[] countries = {"Turkey", "Greece", "Croatia", "Italy"};

        // ═══════════════════════════════════════════════════════════════
        // EXPENSE ENTRIES
        // ═══════════════════════════════════════════════════════════════
        List<FinancialCategory> expenseCategories = categories.stream()
                .filter(c -> c.getCategoryType() == RecordType.EXPENSE)
                .toList();

        if (expenseCategories.isEmpty()) {
            expenseCategories = categories; // fallback
        }

        for (LocalDate date = start; !date.isAfter(end); date = date.plusMonths(1)) {
            int entryCount = 5 + random.nextInt(11);

            for (int i = 0; i < entryCount; i++) {
                double amount = 50 + random.nextDouble() * 5950; // 50–6000 EUR range
                LocalDate entryDate = date.plusDays(random.nextInt(Math.min(28, date.lengthOfMonth())));
                User submitter = crewMembers.isEmpty() ? admin : crewMembers.get(random.nextInt(crewMembers.size()));
                String city = cities[random.nextInt(cities.length)];
                String country = countries[random.nextInt(countries.length)];

                FinancialEntry entry = FinancialEntry.create(
                        EntryNumber.generate(counter++),
                        RecordType.EXPENSE,
                        expenseCategories.get(random.nextInt(expenseCategories.size())),
                        Money.of(String.format("%.2f", amount), "EUR"),
                        entryDate,
                        randomPaymentMethod(),
                        "Demo expense - " + expenseVendors[random.nextInt(expenseVendors.length)],
                        null, null,
                        expenseVendors[random.nextInt(expenseVendors.length)],
                        country, city, null, null
                );
                entry = entryRepository.save(entry);

                // Gerçekçi approval senaryoları
                double scenario = random.nextDouble();

                if (scenario < 0.30) {
                    // %30 — DRAFT kalır (henüz submit edilmemiş)
                    // Do nothing

                } else if (scenario < 0.55) {
                    // PENDING_CAPTAIN
                    entry.submit();
                    entryRepository.save(entry);

                } else if (scenario < 0.80) {
                    // APPROVED (full flow)
                    entry.submit();
                    Organization tenant = captain.getOrganization();
                    boolean needsManager = isManagerApprovalRequired(entry, tenant);
                    entry.approveByCaptain(needsManager);
                    if (needsManager) {
                        entry.approveByManager();
                    }
                    entryRepository.save(entry);

                } else if (scenario < 0.90) {
                    // Captain submit — limit'e göre
                    Organization tenant = captain.getOrganization();
                    boolean needsManager = isManagerApprovalRequired(entry, tenant);
                    if (needsManager) {
                        entry.submitToManager();
                    } else {
                        entry.submitAndApprove();
                    }
                    entryRepository.save(entry);


                } else {
                    // REJECTED
                    entry.submit();
                    entry.reject("Amount seems incorrect, please verify");
                    entryRepository.save(entry);
                }
            }
        }

        // ═══════════════════════════════════════════════════════════════
        // INCOME ENTRIES
        // ═══════════════════════════════════════════════════════════════
        log.info("💵 Generating sample income entries...");

        // Income kategorileri oluştur (yoksa)
        List<FinancialCategory> incomeCategories = createIncomeCategoriesIfNeeded();

        if (!incomeCategories.isEmpty()) {
            for (LocalDate date = start; !date.isAfter(end); date = date.plusMonths(1)) {
                int incomeCount = 1 + random.nextInt(3); // Ayda 1-3 gelir

                for (int i = 0; i < incomeCount; i++) {
                    double amount = 500 + random.nextDouble() * 19500; // 500–20000 EUR
                    LocalDate entryDate = date.plusDays(random.nextInt(Math.min(28, date.lengthOfMonth())));
                    String vendor = incomeVendors[random.nextInt(incomeVendors.length)];

                    FinancialEntry income = FinancialEntry.create(
                            EntryNumber.generate(counter++),
                            RecordType.INCOME,
                            incomeCategories.get(random.nextInt(incomeCategories.size())),
                            Money.of(String.format("%.2f", amount), "EUR"),
                            entryDate,
                            random.nextBoolean() ? PaymentMethod.BANK_TRANSFER : PaymentMethod.CASH,
                            "Income - " + vendor,
                            null, null,
                            vendor,
                            "Turkey",
                            cities[random.nextInt(cities.length)],
                            null, null
                    );
                    income = entryRepository.save(income);

                    // Income'lar genelde direkt approve edilir
                    if (random.nextDouble() < 0.7) {
                        try {
                            approvalService.submit(income.getEntryId(), captain);
                        } catch (Exception e) {
                            log.warn("Income submit failed: {}", e.getMessage());
                        }
                    }
                }
            }
        }

        log.info("✓ Generated {} total entries (expenses + incomes)", entryRepository.count());
    }
    // ApprovalService'teki ile aynı logic — DemoDataInitializer'a kopyala
    private boolean isManagerApprovalRequired(FinancialEntry entry, Organization tenant) {
        if (!tenant.isManagerApprovalEnabled()) return false;
        BigDecimal limit = tenant.getApprovalLimit();
        if (limit == null) return false;
        return entry.getBaseAmount().getAmount().compareTo(limit) > 0;
    }
    /**
     * Create income categories if they don't exist.
     */
    private List<FinancialCategory> createIncomeCategoriesIfNeeded() {
        List<FinancialCategory> incomeCategories = new ArrayList<>();

        String[][] incomeCats = {
                {"CHARTER_INCOME", "Charter Income"},
                {"REFUND", "Refunds & Returns"},
                {"EQUIPMENT_SALE", "Equipment Sale"},
                {"SUBSIDY", "Subsidies & Grants"},
                {"OTHER_INCOME", "Other Income"}
        };

        for (String[] cat : incomeCats) {
            Optional<FinancialCategory> existing = financialCategoryRepository.findByCode(cat[0]);
            if (existing.isEmpty()) {
                incomeCategories.add(financialCategoryRepository.save(
                        FinancialCategory.create(cat[0], cat[1], RecordType.INCOME, "Demo: " + cat[1], 1, true)
                ));
            } else {
                incomeCategories.add(existing.get());
            }
        }

        return incomeCategories;
    }

    private PaymentMethod randomPaymentMethod() {
        PaymentMethod[] methods = {PaymentMethod.CASH, PaymentMethod.BANK_TRANSFER, PaymentMethod.CREDIT_CARD};
        return methods[random.nextInt(methods.length)];
    }
}