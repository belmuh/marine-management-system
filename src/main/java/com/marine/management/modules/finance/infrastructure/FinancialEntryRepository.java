package com.marine.management.modules.finance.infrastructure;

import com.marine.management.modules.finance.domain.enums.RecordType;
import com.marine.management.modules.finance.domain.entity.FinancialCategory;
import com.marine.management.modules.finance.domain.entity.FinancialEntry;
import com.marine.management.modules.users.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FinancialEntryRepository extends JpaRepository<FinancialEntry, UUID> {

    @Query(value = "SELECT NEXTVAL('financial_entry_seq')", nativeQuery = true)
    int getNextSequence();

    Optional<FinancialEntry> findByEntryNumber_Value(String value);
    boolean existsByEntryNumber_Value(String value);

    Page<FinancialEntry> findByCreatedBy(User user, Pageable pageable);
    List<FinancialEntry> findByCreatedByOrderByEntryDateDesc(User user);


    // DATE RANGE QUERIES
    Page<FinancialEntry> findByEntryDateBetween(
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable
    );

    List<FinancialEntry> findByEntryDateBetweenOrderByEntryDateDesc(
            LocalDate startDate,
            LocalDate endDate
    );


    // TYPE AND CATEGORY QUERIES
    List<FinancialEntry> findByEntryTypeOrderByEntryDateDesc(RecordType type);
    Page<FinancialEntry> findByEntryType(RecordType type, Pageable pageable);

    List<FinancialEntry> findByCategoryOrderByEntryDateDesc(FinancialCategory category);
    Page<FinancialEntry> findByCategory(FinancialCategory category, Pageable pageable);

    // WHO AND MAIN CATEGORY QUERIES
    Page<FinancialEntry> findByWhoId(Long whoId, Pageable pageable);
    List<FinancialEntry> findByWhoIdOrderByEntryDateDesc(Long whoId);

    Page<FinancialEntry> findByMainCategoryId(Long mainCategoryId, Pageable pageable);
    List<FinancialEntry> findByMainCategoryIdOrderByEntryDateDesc(Long mainCategoryId);

    // Combined who + main category
    Page<FinancialEntry> findByWhoIdAndMainCategoryId(Long whoId, Long mainCategoryId, Pageable pageable);


    // DASHBOARD METRICS

    // Period totals by entry type
    @Query("SELECT e.entryType as entryType, SUM(e.baseAmount.amount) as total " +
            "FROM FinancialEntry e WHERE e.entryDate BETWEEN :start AND :end " +
            "GROUP BY e.entryType")
    List<PeriodTotalProjection> findPeriodTotals(
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );

    // Category-based totals (flexible - works for both income and expense)
    @Query("SELECT e.category.name as categoryName, " +
            "SUM(e.baseAmount.amount) as total, " +
            "COUNT(e.id) as entryCount " +
            "FROM FinancialEntry e " +
            "WHERE e.entryType = :entryType AND e.entryDate BETWEEN :start AND :end " +
            "GROUP BY e.category.name " +
            "ORDER BY total DESC")
    List<CategoryTotalProjection> findCategoryTotals(
            @Param("entryType") RecordType entryType,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );

    // Helper methods for type-safe category totals
    default List<CategoryTotalProjection> findExpenseTotals(LocalDate start, LocalDate end) {
        return findCategoryTotals(RecordType.EXPENSE, start, end);
    }

    default List<CategoryTotalProjection> findIncomeTotals(LocalDate start, LocalDate end) {
        return findCategoryTotals(RecordType.INCOME, start, end);
    }

    // Who-based totals (for yacht-specific analytics)
    @Query("SELECT e.whoId as whoId, " +
            "SUM(e.baseAmount.amount) as total, " +
            "COUNT(e.id) as entryCount " +
            "FROM FinancialEntry e " +
            "WHERE e.entryType = :entryType " +
            "AND e.whoId IS NOT NULL " +
            "AND e.entryDate BETWEEN :start AND :end " +
            "GROUP BY e.whoId " +
            "ORDER BY total DESC")
    List<WhoTotalProjection> findWhoTotals(
            @Param("entryType") RecordType entryType,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );

    // Main Category totals (high-level grouping)
    @Query("SELECT e.mainCategoryId as mainCategoryId, " +
            "SUM(e.baseAmount.amount) as total, " +
            "COUNT(e.id) as entryCount " +
            "FROM FinancialEntry e " +
            "WHERE e.entryType = :entryType " +
            "AND e.mainCategoryId IS NOT NULL " +
            "AND e.entryDate BETWEEN :start AND :end " +
            "GROUP BY e.mainCategoryId " +
            "ORDER BY total DESC")
    List<MainCategoryTotalProjection> findMainCategoryTotals(
            @Param("entryType") RecordType entryType,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );

    // Monthly totals for charts
    @Query("SELECT EXTRACT(YEAR FROM e.entryDate) as year, " +
            "EXTRACT(MONTH FROM e.entryDate) as month, " +
            "e.entryType as entryType, " +
            "SUM(e.baseAmount.amount) as total, " +
            "COUNT(e.id) as entryCount " +
            "FROM FinancialEntry e " +
            "WHERE e.entryDate BETWEEN :start AND :end " +
            "GROUP BY EXTRACT(YEAR FROM e.entryDate), EXTRACT(MONTH FROM e.entryDate), e.entryType " +
            "ORDER BY year, month")
    List<MonthlyTotalProjection> findMonthlyTotals(
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );


    // ADVANCED SEARCH (Updated with who and mainCategory)
    @Query("SELECT e FROM FinancialEntry e WHERE " +
            "(:categoryId IS NULL OR e.category.id = :categoryId) AND " +
            "(:entryType IS NULL OR e.entryType = :entryType) AND " +
            "(:whoId IS NULL OR e.whoId = :whoId) AND " +
            "(:mainCategoryId IS NULL OR e.mainCategoryId = :mainCategoryId) AND " +
            "e.entryDate >= COALESCE(:startDate, {d '1900-01-01'}) AND " +
            "e.entryDate <= COALESCE(:endDate, {d '2100-12-31'}) " +
            "ORDER BY e.entryDate DESC")
    Page<FinancialEntry> search(
            @Param("categoryId") UUID categoryId,
            @Param("entryType") RecordType entryType,
            @Param("whoId") Long whoId,
            @Param("mainCategoryId") Long mainCategoryId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable
    );

    // Text search with optional filters
    @Query("SELECT e FROM FinancialEntry e WHERE " +
            "(LOWER(e.description) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(e.receiptNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) AND " +
            "(:entryType IS NULL OR e.entryType = :entryType) AND " +
            "e.entryDate >= COALESCE(:startDate, {d '1900-01-01'}) AND " +
            "e.entryDate <= COALESCE(:endDate, {d '2100-12-31'}) "   )
    Page<FinancialEntry> searchByText(
            @Param("searchTerm") String searchTerm,
            @Param("entryType") RecordType entryType,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable
    );

    // REPORTS
    // Category breakdown by month
    @Query("SELECT e.category.name as categoryName, " +
            "EXTRACT(MONTH FROM e.entryDate) as month, " +
            "SUM(e.baseAmount.amount) as total " +
            "FROM FinancialEntry e " +
            "WHERE e.entryType = :entryType " +
            "AND EXTRACT(YEAR FROM e.entryDate) = :year " +
            "GROUP BY e.category.name, EXTRACT(MONTH FROM e.entryDate) " +
            "ORDER BY categoryName, month")
    List<CategoryMonthBreakdownProjection> findCategoryMonthBreakdown(
            @Param("entryType") RecordType entryType,
            @Param("year") int year
    );

    // Monthly income/expense totals for a year
    @Query("SELECT EXTRACT(MONTH FROM e.entryDate) as month, " +
            "e.entryType as entryType, " +
            "SUM(e.baseAmount.amount) as total " +
            "FROM FinancialEntry e " +
            "WHERE EXTRACT(YEAR FROM e.entryDate) = :year " +
            "GROUP BY EXTRACT(MONTH FROM e.entryDate), e.entryType " +
            "ORDER BY month")
    List<MonthlyIncomeExpenseProjection> findMonthlyIncomeExpense(
            @Param("year") int year
    );

    // Main Category breakdown by month (for high-level reports)
    @Query("SELECT e.mainCategoryId as mainCategoryId, " +
            "EXTRACT(MONTH FROM e.entryDate) as month, " +
            "SUM(e.baseAmount.amount) as total, " +
            "COUNT(e.id) as entryCount " +
            "FROM FinancialEntry e " +
            "WHERE e.entryType = :entryType " +
            "AND e.mainCategoryId IS NOT NULL " +
            "AND EXTRACT(YEAR FROM e.entryDate) = :year " +
            "GROUP BY e.mainCategoryId, EXTRACT(MONTH FROM e.entryDate) " +
            "ORDER BY mainCategoryId, month")
    List<MainCategoryMonthBreakdownProjection> findMainCategoryMonthBreakdown(
            @Param("entryType") RecordType entryType,
            @Param("year") int year
    );

    interface CategoryMonthBreakdownProjection {
        String getCategoryName();
        Integer getMonth();
        BigDecimal getTotal();
    }

    interface MonthlyIncomeExpenseProjection {
        Integer getMonth();
        RecordType getEntryType();
        BigDecimal getTotal();
    }

    interface MainCategoryMonthBreakdownProjection {
        Long getMainCategoryId();
        Integer getMonth();
        BigDecimal getTotal();
        Long getEntryCount();
    }

    // STATISTICS

    // Count by type and date range
    long countByEntryTypeAndEntryDateBetween(
            RecordType entryType,
            LocalDate startDate,
            LocalDate endDate
    );

    // Total amount by type
    @Query("SELECT COALESCE(SUM(e.baseAmount.amount), 0) FROM FinancialEntry e " +
            "WHERE e.entryType = :entryType AND e.entryDate BETWEEN :start AND :end")
    BigDecimal sumByEntryTypeAndDateRange(
            @Param("entryType") RecordType entryType,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );

    // Count detailed vs simple entries
    @Query("SELECT COUNT(e) FROM FinancialEntry e " +
            "WHERE e.whoId IS NOT NULL AND e.mainCategoryId IS NOT NULL " +
            "AND e.entryDate BETWEEN :start AND :end")
    long countDetailedEntries(
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );


    // PROJECTION INTERFACES

    interface PeriodTotalProjection {
        RecordType getEntryType();
        BigDecimal getTotal();
    }

    interface CategoryTotalProjection {
        String getCategoryName();
        BigDecimal getTotal();
        Long getEntryCount();
    }

    interface WhoTotalProjection {
        Long getWhoId();
        BigDecimal getTotal();
        Long getEntryCount();
    }

    interface MainCategoryTotalProjection {
        Long getMainCategoryId();
        BigDecimal getTotal();
        Long getEntryCount();
    }

    interface MonthlyTotalProjection {
        Integer getYear();
        Integer getMonth();
        RecordType getEntryType();
        BigDecimal getTotal();
        Long getEntryCount();
    }
}