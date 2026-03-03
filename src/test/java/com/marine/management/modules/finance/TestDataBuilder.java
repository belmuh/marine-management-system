package com.marine.management.modules.finance;

import com.marine.management.modules.finance.domain.entities.FinancialCategory;
import com.marine.management.modules.finance.domain.entities.FinancialEntry;
import com.marine.management.modules.finance.domain.enums.*;
import com.marine.management.modules.finance.domain.vo.EntryNumber;
import com.marine.management.modules.finance.domain.vo.Money;
import com.marine.management.modules.organization.domain.Organization;
import com.marine.management.modules.users.domain.User;
import com.marine.management.shared.domain.BaseTenantEntity;
import com.marine.management.shared.security.Role;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Test data builder utility for creating test objects
 * Centralizes test data creation to avoid duplication
 */
public class TestDataBuilder {

    // ============================================
    // USER BUILDERS
    // ============================================

    public static User createCrew(Long tenantId) {
        Organization org = createOrganization(tenantId);
        User user = User.createWithHashedPassword(
                "crew@test.com",
                "Test",
                "Crew",
                "$2a$10$hashedPassword",
                Role.CREW,
                org
        );
        setUserId(user, UUID.randomUUID());
        return user;
    }

    public static User createCaptain(Long tenantId) {
        Organization org = createOrganization(tenantId);
        User user = User.createWithHashedPassword(
                "captain@test.com",
                "Test",
                "Captain",
                "$2a$10$hashedPassword",
                Role.CAPTAIN,
                org
        );
        setUserId(user, UUID.randomUUID());
        return user;
    }

    public static User createManager(Long tenantId) {
        Organization org = createOrganization(tenantId);
        User user = User.createWithHashedPassword(
                "manager@test.com",
                "Test",
                "Manager",
                "$2a$10$hashedPassword",
                Role.MANAGER,
                org
        );
        setUserId(user, UUID.randomUUID());
        return user;
    }

    public static User createAdmin(Long tenantId) {
        Organization org = createOrganization(tenantId);
        User user = User.createWithHashedPassword(
                "admin@test.com",
                "Test",
                "Admin",
                "$2a$10$hashedPassword",
                Role.ADMIN,
                org
        );
        setUserId(user, UUID.randomUUID());
        return user;
    }

    // TestDataBuilder.java — createCaptain metodunun yanına ekle

    public static User createCaptainWithOrganization(Organization organization) {
        User user = User.createWithHashedPassword(
                "captain@test.com",
                "Test",
                "Captain",
                "$2a$10$hashedPassword",
                Role.CAPTAIN,
                organization
        );
        setUserId(user, UUID.randomUUID());
        return user;
    }

    // ============================================
    // ORGANIZATION BUILDER
    // ============================================

    public static Organization createOrganization(Long tenantId) {
        Organization org = Organization.create(
                "Test Yacht " + tenantId,
                "Test Marine Org " + tenantId,
                "TR",
                "EUR"
        );
        setOrganizationId(org, tenantId);
        return org;
    }

    // ============================================
    // CATEGORY BUILDER
    // ============================================

    public static FinancialCategory createCategory(Long tenantId) {
        FinancialCategory category = FinancialCategory.create(
                "TESTCAT",
                "Test Category",
                RecordType.EXPENSE,
                "Test category for unit tests",
                1,
                true
        );
        setTenantId(category, tenantId);
        setCategoryId(category, UUID.randomUUID());
        return category;
    }

    // ============================================
    // FINANCIAL ENTRY BUILDERS
    // ============================================

    public static FinancialEntry createDraftEntry(User creator) {
        return createEntryWithStatus(
                creator,
                EntryStatus.DRAFT,
                Money.of("500.00", "EUR")
        );
    }

    public static FinancialEntry createPendingCaptainEntry(User creator) {
        FinancialEntry entry = createEntryWithStatus(
                creator,
                EntryStatus.DRAFT,
                Money.of("500.00", "EUR")
        );
        // Submit to move to PENDING_CAPTAIN
        entry.submit(); // ✅ User parameter removed
        return entry;
    }

    public static FinancialEntry createPendingCaptainEntryWithAmount(User creator, Money amount) {
        FinancialEntry entry = createEntryWithStatus(
                creator,
                EntryStatus.DRAFT,
                amount
        );
        // Submit to move to PENDING_CAPTAIN
        entry.submit();
        return entry;
    }

    public static FinancialEntry createPendingManagerEntry(User creator) {
        FinancialEntry entry = createEntryWithStatus(
                creator,
                EntryStatus.DRAFT,
                Money.of("600.00", "EUR")
        );
        // Submit and approve by captain with manager approval needed
        entry.submit(); // ✅ User parameter removed
        entry.approveByCaptain(true); // ✅ User parameter removed
        return entry;
    }

    public static FinancialEntry createPendingManagerEntryWithAmount(User creator, Money amount) {
        FinancialEntry entry = createEntryWithStatus(
                creator,
                EntryStatus.DRAFT,
                amount
        );
        entry.submit();
        entry.approveByCaptain(true);
        return entry;
    }

    public static FinancialEntry createApprovedEntry(User creator) {
        FinancialEntry entry = createEntryWithStatus(
                creator,
                EntryStatus.DRAFT,
                Money.of("500.00", "EUR")
        );
        // Submit and approve by captain (no manager approval needed)
        entry.submit(); // ✅ User parameter removed
        entry.approveByCaptain(false); // ✅ User parameter removed
        return entry;
    }

    public static FinancialEntry createApprovedEntryWithAmount(User creator, Money amount) {
        FinancialEntry entry = createEntryWithStatus(
                creator,
                EntryStatus.DRAFT,
                amount
        );
        entry.submit();
        entry.approveByCaptain(false);
        return entry;
    }

    public static FinancialEntry createPartiallyPaidEntry(User creator, Money approvedAmount, Money paidAmount) {
        FinancialEntry entry = createApprovedEntryWithAmount(creator, approvedAmount);
        entry.recordPayment(paidAmount); // Records payment and updates status
        return entry;
    }

    public static FinancialEntry createFullyPaidEntry(User creator, Money amount) {
        FinancialEntry entry = createApprovedEntryWithAmount(creator, amount);
        entry.recordPayment(amount); // Full payment -> status becomes PAID
        return entry;
    }

    private static FinancialEntry createEntryWithStatus(User creator, EntryStatus status, Money amount) {
        FinancialCategory category = createCategory(creator.getOrganizationId());

        FinancialEntry entry = FinancialEntry.create(
                EntryNumber.generate(1),
                RecordType.EXPENSE,
                category,
                amount,
                LocalDate.now(),
                PaymentMethod.CASH,
                "Test entry for unit tests",
                null,
                null,
                "Test Recipient",
                "Turkey",
                "Istanbul",
                "Test Location",
                "Test Vendor"
        );

        // Set IDs
        setTenantId(entry, creator.getOrganizationId());
        setEntryId(entry, UUID.randomUUID());
        setEntryCreatedBy(entry, creator.getUserId());

        // Force status using reflection ONLY if not DRAFT
        // (Because DRAFT is the default status from factory method)
        if (status != EntryStatus.DRAFT) {
            setEntryStatus(entry, status);
        }

        return entry;
    }

    // ============================================
    // MONEY BUILDERS
    // ============================================

    public static Money euro(String amount) {
        return Money.of(amount, "EUR");
    }

    public static Money usd(String amount) {
        return Money.of(amount, "USD");
    }

    public static Money tryLira(String amount) {
        return Money.of(amount, "TRY");
    }

    // ============================================
    // REFLECTION UTILITIES
    // ============================================

    private static void setTenantId(BaseTenantEntity entity, Long tenantId) {
        setField(entity, BaseTenantEntity.class, "tenantId", tenantId);
    }

    private static void setOrganizationId(Organization organization, Long organizationId) {
        setField(organization, Organization.class, "id", organizationId);
    }

    private static void setUserId(User user, UUID userId) {
        setField(user, User.class, "id", userId);
    }

    private static void setCategoryId(FinancialCategory category, UUID categoryId) {
        setField(category, FinancialCategory.class, "id", categoryId);
    }

    private static void setEntryId(FinancialEntry entry, UUID entryId) {
        setField(entry, FinancialEntry.class, "id", entryId);
    }

    private static void setEntryCreatedBy(FinancialEntry entry, UUID createdById) {
        setField(entry, FinancialEntry.class, "createdById", createdById);
    }

    private static void setEntryStatus(FinancialEntry entry, EntryStatus status) {
        setField(entry, FinancialEntry.class, "status", status);
    }

    /**
     * Generic reflection utility to set private fields
     * Searches in class hierarchy (parent classes) if not found in target class
     */
    private static void setField(Object target, Class<?> clazz, String fieldName, Object value) {
        try {
            Field field = findField(clazz, fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(
                    String.format("Failed to set field '%s' on class '%s'", fieldName, clazz.getSimpleName()),
                    e
            );
        }
    }

    /**
     * Find field in class hierarchy (including parent classes)
     */
    private static Field findField(Class<?> clazz, String fieldName) {
        Class<?> currentClass = clazz;

        while (currentClass != null) {
            try {
                return currentClass.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                // Field not found in current class, try parent
                currentClass = currentClass.getSuperclass();
            }
        }

        throw new RuntimeException(
                String.format("Field '%s' not found in class '%s' or its parent classes",
                        fieldName, clazz.getSimpleName())
        );
    }

    // ============================================
    // BUILDER PATTERN
    // ============================================

    public static class FinancialEntryBuilder {
        private User creator;
        private EntryStatus status = EntryStatus.DRAFT;
        private Money amount = Money.of("500.00", "EUR");
        private RecordType type = RecordType.EXPENSE;
        private String description = "Test entry";
        private LocalDate entryDate = LocalDate.now();
        private PaymentMethod paymentMethod = PaymentMethod.CASH;
        private boolean shouldApprove = false;
        private boolean needsManagerApproval = false;
        private Money paidAmount;

        public FinancialEntryBuilder creator(User creator) {
            this.creator = creator;
            return this;
        }

        public FinancialEntryBuilder status(EntryStatus status) {
            this.status = status;
            return this;
        }

        public FinancialEntryBuilder amount(Money amount) {
            this.amount = amount;
            return this;
        }

        public FinancialEntryBuilder type(RecordType type) {
            this.type = type;
            return this;
        }

        public FinancialEntryBuilder description(String description) {
            this.description = description;
            return this;
        }

        public FinancialEntryBuilder entryDate(LocalDate entryDate) {
            this.entryDate = entryDate;
            return this;
        }

        public FinancialEntryBuilder paymentMethod(PaymentMethod paymentMethod) {
            this.paymentMethod = paymentMethod;
            return this;
        }

        public FinancialEntryBuilder approved() {
            this.shouldApprove = true;
            return this;
        }

        public FinancialEntryBuilder needsManagerApproval() {
            this.needsManagerApproval = true;
            return this;
        }

        public FinancialEntryBuilder paid(Money paidAmount) {
            this.paidAmount = paidAmount;
            return this;
        }

        public FinancialEntry build() {
            if (creator == null) {
                throw new IllegalStateException("Creator is required for FinancialEntry");
            }

            FinancialCategory category = createCategory(creator.getOrganizationId());

            FinancialEntry entry = FinancialEntry.create(
                    EntryNumber.generate(1),
                    type,
                    category,
                    amount,
                    entryDate,
                    paymentMethod,
                    description,
                    null,
                    null,
                    "Test Recipient",
                    "Turkey",
                    "Istanbul",
                    null,
                    null
            );

            // Set IDs
            setTenantId(entry, creator.getOrganizationId());
            setEntryId(entry, UUID.randomUUID());
            setEntryCreatedBy(entry, creator.getUserId());

            // Apply workflow transitions based on desired status
            if (status == EntryStatus.PENDING_CAPTAIN) {
                entry.submit();
            } else if (status == EntryStatus.PENDING_MANAGER) {
                entry.submit();
                entry.approveByCaptain(true);
            } else if (status == EntryStatus.APPROVED || shouldApprove) {
                entry.submit();
                entry.approveByCaptain(needsManagerApproval);
                if (needsManagerApproval) {
                    entry.approveByManager();
                }
            } else if (status == EntryStatus.REJECTED) {
                entry.submit();
                entry.reject("Rejected for testing");
            } else if (status != EntryStatus.DRAFT) {
                // For other statuses, force with reflection
                setEntryStatus(entry, status);
            }

            // Apply payment if specified
            if (paidAmount != null && entry.getStatus() == EntryStatus.APPROVED) {
                entry.recordPayment(paidAmount);
            }

            return entry;
        }
    }

    public static FinancialEntryBuilder entry() {
        return new FinancialEntryBuilder();
    }
}