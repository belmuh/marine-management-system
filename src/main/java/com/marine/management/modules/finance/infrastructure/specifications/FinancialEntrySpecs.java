package com.marine.management.modules.finance.infrastructure.specifications;

import com.marine.management.modules.finance.domain.entities.FinancialEntry;
import com.marine.management.modules.finance.domain.enums.EntryStatus;
import com.marine.management.modules.finance.domain.enums.RecordType;
import com.marine.management.modules.finance.infrastructure.query.EntrySearchCriteria;
import com.marine.management.modules.users.domain.User;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * JPA Specifications for FinancialEntry dynamic queries.
 * Supports role-based filtering, date ranges, status filtering, sorting, etc.
 */
public class FinancialEntrySpecs {

    private FinancialEntrySpecs() {}

    // ============================================
    // TENANT ISOLATION (Always applied via Hibernate Filter)
    // ============================================
    // Note: tenant_id filtering is automatic via Hibernate Filter

    // ============================================
    // ROLE-BASED FILTERS (mevcut - değişiklik yok)
    // ============================================

    public static Specification<FinancialEntry> forUser(User user) {
        if (user == null) {
            return null;
        }
        return switch (user.getRoleEnum()) {
            case CREW -> createdBy(user.getUserId());
            case CAPTAIN -> pendingForCaptain().or(createdBy(user.getUserId()));
            case MANAGER -> pendingForManager();
            case ADMIN, SUPER_ADMIN -> null;
        };
    }

    /**
     * Role-based visibility filter for financial reports and dashboard.
     *
     * Unlike forUser() which controls the approval queue,
     * this controls what financial data a user can see in reports:
     *
     *   CREW         → only their own entries
     *   CAPTAIN      → all entries (vessel overview)
     *   MANAGER      → all entries (financial control)
     *   ADMIN        → all entries
     *   SUPER_ADMIN  → all entries
     *
     * Note: tenant isolation is always enforced by Hibernate @Filter
     * regardless of what this method returns.
     */
    public static Specification<FinancialEntry> forFinancialReports(User user) {
        if (user == null) {
            return null;
        }
        return switch (user.getRoleEnum()) {
            case CREW                                      -> createdBy(user.getUserId());
            case CAPTAIN, MANAGER, ADMIN, SUPER_ADMIN     -> null;
        };
    }

    public static Specification<FinancialEntry> createdBy(UUID userId) {
        return (root, query, cb) -> {
            if (userId == null) return null;
            return cb.equal(root.get("createdById"), userId);
        };
    }

    public static Specification<FinancialEntry> pendingForCaptain() {
        return (root, query, cb) ->
                cb.equal(root.get("status"), EntryStatus.PENDING_CAPTAIN);
    }

    public static Specification<FinancialEntry> pendingForManager() {
        return (root, query, cb) ->
                cb.equal(root.get("status"), EntryStatus.PENDING_MANAGER);
    }

    // ============================================
    // STATUS FILTERS (mevcut - değişiklik yok)
    // ============================================

    public static Specification<FinancialEntry> hasStatus(EntryStatus status) {
        return (root, query, cb) -> {
            if (status == null) return null;
            return cb.equal(root.get("status"), status);
        };
    }

    public static Specification<FinancialEntry> statusIn(Set<EntryStatus> statuses) {
        return (root, query, cb) -> {
            if (statuses == null || statuses.isEmpty()) return null;
            return root.get("status").in(statuses);
        };
    }

    public static Specification<FinancialEntry> actualEntries() {
        return statusIn(EntryStatus.ACTUAL_STATUSES);
    }

    public static Specification<FinancialEntry> committedEntries() {
        return statusIn(EntryStatus.COMMITTED_STATUSES);
    }

    public static Specification<FinancialEntry> needsPayment() {
        return statusIn(Set.of(EntryStatus.APPROVED, EntryStatus.PARTIALLY_PAID));
    }

    // ============================================
    // DATE RANGE FILTERS (mevcut - değişiklik yok)
    // ============================================

    public static Specification<FinancialEntry> dateRange(LocalDate startDate, LocalDate endDate) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("entryDate"), startDate));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("entryDate"), endDate));
            }
            return predicates.isEmpty() ? null : cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<FinancialEntry> afterDate(LocalDate date) {
        return (root, query, cb) -> {
            if (date == null) return null;
            return cb.greaterThanOrEqualTo(root.get("entryDate"), date);
        };
    }

    public static Specification<FinancialEntry> beforeDate(LocalDate date) {
        return (root, query, cb) -> {
            if (date == null) return null;
            return cb.lessThanOrEqualTo(root.get("entryDate"), date);
        };
    }

    // ============================================
    // TYPE & CATEGORY FILTERS
    // ============================================

    public static Specification<FinancialEntry> entryType(RecordType type) {
        return (root, query, cb) -> {
            if (type == null) return null;
            return cb.equal(root.get("entryType"), type);
        };
    }

    public static Specification<FinancialEntry> categoryEquals(UUID categoryId) {
        return (root, query, cb) -> {
            if (categoryId == null) return null;
            return cb.equal(root.get("category").get("id"), categoryId);
        };
    }

    //  YENİ: WHO filter - Long id ile (entity'deki Who.id Long)
    public static Specification<FinancialEntry> whoEquals(Long whoId) {
        return (root, query, cb) -> {
            if (whoId == null) return null;
            // JOIN path: FinancialEntry → tenantWho → who → id
            Join<?, ?> tenantWho = root.join("tenantWho", JoinType.LEFT);
            Join<?, ?> who = tenantWho.join("who", JoinType.LEFT);
            return cb.equal(who.get("id"), whoId);
        };
    }

    // Mevcut UUID versiyonu - backward compatibility için kalsın
    public static Specification<FinancialEntry> whoEqualsUuid(UUID tenantWhoId) {
        return (root, query, cb) -> {
            if (tenantWhoId == null) return null;
            return cb.equal(root.get("tenantWho").get("id"), tenantWhoId);
        };
    }

    //  YENİ: MainCategory filter - Long id ile
    public static Specification<FinancialEntry> mainCategoryEquals(Long mainCategoryId) {
        return (root, query, cb) -> {
            if (mainCategoryId == null) return null;
            // JOIN path: FinancialEntry → tenantMainCategory → mainCategory → id
            Join<?, ?> tenantMainCat = root.join("tenantMainCategory", JoinType.LEFT);
            Join<?, ?> mainCat = tenantMainCat.join("mainCategory", JoinType.LEFT);
            return cb.equal(mainCat.get("id"), mainCategoryId);
        };
    }

    // Mevcut UUID versiyonu - backward compatibility
    public static Specification<FinancialEntry> mainCategoryEqualsUuid(UUID tenantMainCategoryId) {
        return (root, query, cb) -> {
            if (tenantMainCategoryId == null) return null;
            return cb.equal(root.get("tenantMainCategory").get("id"), tenantMainCategoryId);
        };
    }

    // ============================================
    // TEXT SEARCH FILTERS (mevcut - değişiklik yok)
    // ============================================

    public static Specification<FinancialEntry> searchText(String searchTerm) {
        return (root, query, cb) -> {
            if (searchTerm == null || searchTerm.isBlank()) return null;
            String pattern = "%" + searchTerm.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("description")), pattern),
                    cb.like(cb.lower(root.get("receiptNumber")), pattern),
                    cb.like(cb.lower(root.get("vendor")), pattern),
                    cb.like(cb.lower(root.get("recipient")), pattern)
            );
        };
    }

    // ============================================
    // ✅ YENİ: CRITERIA-BASED SEARCH (Dinamik Sort için)
    // ============================================

    /**
     * EntrySearchCriteria ile dinamik search.
     *
     * Bu method:
     * 1. DISTINCT ekler - JOIN duplicate'larını önler
     * 2. Category'yi FETCH eder - N+1 önler
     * 3. Tüm filtreleri AND ile birleştirir
     *
     * Sort işlemi bu method dışında Pageable ile yapılır.
     */
    public static Specification<FinancialEntry> fromCriteria(EntrySearchCriteria criteria) {
        return (root, query, cb) -> {
            // DISTINCT: LEFT JOIN'ler duplicate row üretebilir
            query.distinct(true);

            // FETCH JOIN: N+1 önleme (count query hariç)
            if (!isCountQuery(query)) {
                root.fetch("category", JoinType.LEFT);
            }

            List<Predicate> predicates = new ArrayList<>();

            // Category filter
            if (criteria.categoryId() != null) {
                predicates.add(cb.equal(root.get("category").get("id"), criteria.categoryId()));
            }

            // Entry type filter
            if (criteria.entryType() != null) {
                predicates.add(cb.equal(root.get("entryType"), criteria.entryType()));
            }

            // WHO filter (Long id)
            if (criteria.whoId() != null) {
                Join<?, ?> tenantWho = root.join("tenantWho", JoinType.LEFT);
                Join<?, ?> who = tenantWho.join("who", JoinType.LEFT);
                predicates.add(cb.equal(who.get("id"), criteria.whoId()));
            }

            // MainCategory filter (Long id)
            if (criteria.mainCategoryId() != null) {
                Join<?, ?> tenantMainCat = root.join("tenantMainCategory", JoinType.LEFT);
                Join<?, ?> mainCat = tenantMainCat.join("mainCategory", JoinType.LEFT);
                predicates.add(cb.equal(mainCat.get("id"), criteria.mainCategoryId()));
            }

            if (criteria.status() != null && !criteria.status().isEmpty()) {
                predicates.add(root.get("status").in(criteria.status()));
            }

            // Date range filter
            if (criteria.startDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("entryDate"), criteria.startDate()));
            }
            if (criteria.endDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("entryDate"), criteria.endDate()));
            }

            // Text search filter
            if (criteria.hasTextSearch()) {
                String pattern = "%" + criteria.searchTerm().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("description")), pattern),
                        cb.like(cb.lower(root.get("receiptNumber")), pattern),
                        cb.like(cb.lower(root.get("vendor")), pattern),
                        cb.like(cb.lower(root.get("recipient")), pattern)
                ));
            }

            return predicates.isEmpty()
                    ? cb.conjunction()
                    : cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    // ============================================
    // COMPOSITE FILTERS (mevcut - değişiklik yok)
    // ============================================

    public static Specification<FinancialEntry> buildSearchSpec(
            User currentUser,
            LocalDate startDate,
            LocalDate endDate,
            Set<EntryStatus> statuses,
            RecordType entryType,
            UUID categoryId,
            UUID whoId,
            UUID mainCategoryId,
            String searchTerm
    ) {
        return (root, query, cb) -> {
            //  Duplicate önleme
            query.distinct(true);

            //  N+1 önleme
            if (!isCountQuery(query)) {
                root.fetch("category", JoinType.LEFT);
            }
            List<Predicate> predicates = new ArrayList<>();
            addPredicate(predicates, forUser(currentUser), root, query, cb);
            addPredicate(predicates, dateRange(startDate, endDate), root, query, cb);
            addPredicate(predicates, statusIn(statuses), root, query, cb);
            addPredicate(predicates, entryType(entryType), root, query, cb);
            addPredicate(predicates, categoryEquals(categoryId), root, query, cb);
            addPredicate(predicates, whoEqualsUuid(whoId), root, query, cb);
            addPredicate(predicates, mainCategoryEqualsUuid(mainCategoryId), root, query, cb);
            addPredicate(predicates, searchText(searchTerm), root, query, cb);
            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<FinancialEntry> captainApprovalDashboard(User captain) {
        return (root, query, cb) -> {
            query.distinct(true);

            if (!isCountQuery(query)) {
                root.fetch("category", JoinType.LEFT);
            }
            List<Predicate> predicates = new ArrayList<>();
            addPredicate(predicates, pendingForCaptain(), root, query, cb);
            addPredicate(predicates, dateRange(LocalDate.now().minusMonths(3), null), root, query, cb);
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<FinancialEntry> managerApprovalDashboard(User manager) {
        return (root, query, cb) -> {
            query.distinct(true);

            if (!isCountQuery(query)) {
                root.fetch("category", JoinType.LEFT);
            }
            List<Predicate> predicates = new ArrayList<>();
            addPredicate(predicates, pendingForManager(), root, query, cb);
            addPredicate(predicates, dateRange(LocalDate.now().minusMonths(3), null), root, query, cb);
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<FinancialEntry> unpaidEntries() {
        return (root, query, cb) -> {
            query.distinct(true);

            if (!isCountQuery(query)) {
                root.fetch("category", JoinType.LEFT);
            }
            List<Predicate> predicates = new ArrayList<>();
            addPredicate(predicates, needsPayment(), root, query, cb);
            addPredicate(predicates, dateRange(LocalDate.now().minusYears(1), null), root, query, cb);
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    // ============================================
    // FINANCIAL FILTERS (mevcut - değişiklik yok)
    // ============================================

    public static Specification<FinancialEntry> approvedAmountGreaterThan(BigDecimal amount) {
        return (root, query, cb) -> {
            if (amount == null) return null;
            return cb.greaterThan(root.get("approvedBaseAmount").get("amount"), amount);
        };
    }

    public static Specification<FinancialEntry> hasUnpaidBalance() {
        return (root, query, cb) -> cb.greaterThan(
                cb.diff(
                        root.get("approvedBaseAmount").get("amount"),
                        root.get("paidBaseAmount").get("amount")
                ),
                BigDecimal.ZERO
        );
    }

    // ============================================
    // HELPER METHODS
    // ============================================

    private static void addPredicate(
            List<Predicate> predicates,
            Specification<FinancialEntry> spec,
            Root<FinancialEntry> root,
            CriteriaQuery<?> query,
            CriteriaBuilder cb
    ) {
        if (spec != null) {
            Predicate predicate = spec.toPredicate(root, query, cb);
            if (predicate != null) {
                predicates.add(predicate);
            }
        }
    }

    /**
     * ✅ YENİ: Count query kontrolü.
     *
     * Spring Data Page query'leri iki query çalıştırır:
     * 1. Data query (SELECT e FROM ...)
     * 2. Count query (SELECT COUNT(e) FROM ...)
     *
     * Count query'de FETCH JOIN yapılmamalı - hata verir.
     */
    private static boolean isCountQuery(CriteriaQuery<?> query) {
        Class<?> resultType = query.getResultType();
        return Long.class.equals(resultType) || long.class.equals(resultType);
    }
}