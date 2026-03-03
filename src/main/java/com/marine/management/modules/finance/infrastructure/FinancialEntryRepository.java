package com.marine.management.modules.finance.infrastructure;

import com.marine.management.modules.finance.domain.entities.FinancialEntry;
import com.marine.management.modules.finance.domain.entities.FinancialCategory;
import com.marine.management.modules.finance.domain.enums.EntryStatus;
import com.marine.management.modules.finance.domain.enums.RecordType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface FinancialEntryRepository
        extends JpaRepository<FinancialEntry, UUID>,
        JpaSpecificationExecutor<FinancialEntry> {  // 👈 NEW: Specification support

    // ============================================
    // SEQUENCE & UNIQUE CHECKS
    // ============================================

    @Query(value = "SELECT NEXTVAL('financial_entry_seq')", nativeQuery = true)
    int getNextSequence();

    Optional<FinancialEntry> findByEntryNumber_Value(String value);

    boolean existsByEntryNumber_Value(String value);

    // ============================================
    // 🆕 APPROVAL WORKFLOW QUERIES
    // ============================================

    /**
     * Find entries pending approval for specific user role
     * Captain sees PENDING_CAPTAIN, Manager sees PENDING_MANAGER
     */
    @Query("""
        SELECT e FROM FinancialEntry e
        WHERE e.status = :status
        ORDER BY e.entryDate DESC
    """)
    List<FinancialEntry> findByStatus(@Param("status") EntryStatus status);

    Page<FinancialEntry> findByStatus(EntryStatus status, Pageable pageable);

    /**
     * Find entries in multiple statuses (for filtering)
     */
    @Query("""
        SELECT e FROM FinancialEntry e
        WHERE e.status IN :statuses
        ORDER BY e.entryDate DESC
    """)
    List<FinancialEntry> findByStatusIn(@Param("statuses") Set<EntryStatus> statuses);

    Page<FinancialEntry> findByStatusIn(Set<EntryStatus> statuses, Pageable pageable);

    /**
     * Count pending approvals for dashboard
     */
    long countByStatus(EntryStatus status);

    /**
     * Find approved but unpaid entries
     */
    @Query("""
        SELECT e FROM FinancialEntry e
        WHERE e.status IN ('APPROVED', 'PARTIALLY_PAID')
        AND e.approvedBaseAmount.amount > e.paidBaseAmount.amount
        ORDER BY e.entryDate DESC
    """)
    List<FinancialEntry> findUnpaidEntries();

    /**
     * Find partially paid entries
     */
    @Query("""
        SELECT e FROM FinancialEntry e
        WHERE e.status = 'PARTIALLY_PAID'
        ORDER BY e.entryDate DESC
    """)
    List<FinancialEntry> findPartiallyPaidEntries();

    // ============================================
    // USER-SPECIFIC QUERIES
    // ============================================

    /**
     * Find entries created by specific user
     * (Crew sees only their own entries)
     */
    Page<FinancialEntry> findByCreatedById(UUID userId, Pageable pageable);

    List<FinancialEntry> findByCreatedByIdOrderByEntryDateDesc(UUID userId);

    /**
     * 🆕 Find entries created by user with specific status
     */
    @Query("""
        SELECT e FROM FinancialEntry e
        WHERE e.createdById = :userId
        AND e.status IN :statuses
        ORDER BY e.entryDate DESC
    """)
    List<FinancialEntry> findByCreatedByIdAndStatusIn(
            @Param("userId") UUID userId,
            @Param("statuses") Set<EntryStatus> statuses
    );

    // ============================================
    // DATE RANGE QUERIES
    // ============================================

    Page<FinancialEntry> findByEntryDateBetween(LocalDate startDate, LocalDate endDate, Pageable pageable);

    /**
     * 🆕 Date range with status filter
     */
    @Query("""
        SELECT e FROM FinancialEntry e
        WHERE e.entryDate BETWEEN :startDate AND :endDate
        AND e.status IN :statuses
        ORDER BY e.entryDate DESC
    """)
    List<FinancialEntry> findByEntryDateBetweenAndStatusIn(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("statuses") Set<EntryStatus> statuses
    );

    // ============================================
    // TYPE & CATEGORY QUERIES
    // ============================================

    List<FinancialEntry> findByEntryTypeOrderByEntryDateDesc(RecordType type);

    Page<FinancialEntry> findByEntryType(RecordType type, Pageable pageable);

    List<FinancialEntry> findByCategoryOrderByEntryDateDesc(FinancialCategory category);

    Page<FinancialEntry> findByCategory(FinancialCategory category, Pageable pageable);

    // ============================================
    // WHO & MAIN CATEGORY QUERIES
    // ============================================

    Page<FinancialEntry> findByTenantWho_Id(UUID whoId, Pageable pageable);

    List<FinancialEntry> findByTenantWho_IdOrderByEntryDateDesc(UUID whoId);

    Page<FinancialEntry> findByTenantMainCategory_Id(UUID mainCategoryId, Pageable pageable);

    List<FinancialEntry> findByTenantMainCategory_IdOrderByEntryDateDesc(UUID mainCategoryId);

    Page<FinancialEntry> findByTenantWho_IdAndTenantMainCategory_Id(
            UUID whoId,
            UUID mainCategoryId,
            Pageable pageable
    );

    // ============================================
    // 🆕 FINANCIAL SUMMARY QUERIES (with approval status)
    // ============================================

    /**
     * Total approved amount for period (not just requested)
     */
    @Query("""
        SELECT COALESCE(SUM(e.approvedBaseAmount.amount), 0)
        FROM FinancialEntry e
        WHERE e.entryType = :entryType
        AND e.status IN ('APPROVED', 'PARTIALLY_PAID', 'PAID')
        AND e.entryDate BETWEEN :startDate AND :endDate
    """)
    BigDecimal sumApprovedAmountByTypeAndDateRange(
            @Param("entryType") RecordType entryType,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * Total paid amount for period
     */
    @Query("""
        SELECT COALESCE(SUM(e.paidBaseAmount.amount), 0)
        FROM FinancialEntry e
        WHERE e.entryType = :entryType
        AND e.status IN ('PARTIALLY_PAID', 'PAID')
        AND e.entryDate BETWEEN :startDate AND :endDate
    """)
    BigDecimal sumPaidAmountByTypeAndDateRange(
            @Param("entryType") RecordType entryType,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * Outstanding (approved but unpaid) amount
     */
    @Query("""
        SELECT COALESCE(SUM(e.approvedBaseAmount.amount - e.paidBaseAmount.amount), 0)
        FROM FinancialEntry e
        WHERE e.entryType = :entryType
        AND e.status IN ('APPROVED', 'PARTIALLY_PAID')
        AND e.entryDate BETWEEN :startDate AND :endDate
    """)
    BigDecimal sumOutstandingAmountByTypeAndDateRange(
            @Param("entryType") RecordType entryType,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    // ============================================
    // COUNTS & STATISTICS
    // ============================================

    long countByEntryTypeAndEntryDateBetween(
            RecordType entryType,
            LocalDate startDate,
            LocalDate endDate
    );

    /**
     * 🆕 Count by status and date range
     */
    @Query("""
        SELECT COUNT(e)
        FROM FinancialEntry e
        WHERE e.status IN :statuses
        AND e.entryDate BETWEEN :startDate AND :endDate
    """)
    long countByStatusInAndEntryDateBetween(
            @Param("statuses") Set<EntryStatus> statuses,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    // ============================================
    // PROJECTIONS (for lightweight queries)
    // ============================================

    @Query("""
        SELECT e.entryDate as entryDate,
               e.category.name as categoryName,
               e.baseAmount.amount as baseAmount,
               e.approvedBaseAmount.amount as approvedAmount,
               e.paidBaseAmount.amount as paidAmount,
               e.status as status,
               e.entryType as entryType
        FROM FinancialEntry e
        WHERE e.entryDate BETWEEN :start AND :end
        ORDER BY e.entryDate DESC
    """)
    List<FinancialEntryProjection> findByEntryDateBetweenOrderByEntryDateDesc(
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );

    /**
     * Projection interface for lightweight queries
     */
    interface FinancialEntryProjection {
        LocalDate getEntryDate();
        String getCategoryName();
        BigDecimal getBaseAmount();
        BigDecimal getApprovedAmount();
        BigDecimal getPaidAmount();
        EntryStatus getStatus();
        RecordType getEntryType();
    }
}