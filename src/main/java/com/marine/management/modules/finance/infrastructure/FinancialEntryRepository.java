package com.marine.management.modules.finance.infrastructure;

import com.marine.management.modules.finance.domain.EntryNumber;
import com.marine.management.modules.finance.domain.EntryType;
import com.marine.management.modules.finance.domain.FinancialCategory;
import com.marine.management.modules.finance.domain.FinancialEntry;
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
    List<FinancialEntry> findByEntryTypeOrderByEntryDateDesc(EntryType type);
    Page<FinancialEntry> findByEntryType(EntryType type, Pageable pageable);

    List<FinancialEntry> findByCategoryOrderByEntryDateDesc(FinancialCategory category);
    Page<FinancialEntry> findByCategory(FinancialCategory category, Pageable pageable);


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
    @Query("SELECT e.category.name as categoryName, SUM(e.baseAmount.amount) as total " +
            "FROM FinancialEntry e " +
            "WHERE e.entryType = :entryType AND e.entryDate BETWEEN :start AND :end " +
            "GROUP BY e.category.name " +
            "ORDER BY total DESC")
    List<CategoryTotalProjection> findCategoryTotals(
            @Param("entryType") EntryType entryType,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );

    // Helper methods for type-safe category totals
    default List<CategoryTotalProjection> findExpenseTotals(LocalDate start, LocalDate end) {
        return findCategoryTotals(EntryType.EXPENSE, start, end);
    }

    default List<CategoryTotalProjection> findIncomeTotals(LocalDate start, LocalDate end) {
        return findCategoryTotals(EntryType.INCOME, start, end);
    }

    // Monthly totals for charts
    @Query("SELECT FUNCTION('YEAR', e.entryDate) as year, " +
            "FUNCTION('MONTH', e.entryDate) as month, " +
            "e.entryType as entryType, " +
            "SUM(e.baseAmount.amount) as total " +
            "FROM FinancialEntry e " +
            "WHERE e.entryDate BETWEEN :start AND :end " +
            "GROUP BY FUNCTION('YEAR', e.entryDate), FUNCTION('MONTH', e.entryDate), e.entryType " +
            "ORDER BY year, month")
    List<MonthlyTotalProjection> findMonthlyTotals(
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );


    // ADVANCED SEARCH
    @Query("SELECT e FROM FinancialEntry e WHERE " +
            "(:categoryId IS NULL OR e.category.id = :categoryId) AND " +
            "(:entryType IS NULL OR e.entryType = :entryType) AND " +
            "(:startDate IS NULL OR e.entryDate >= :startDate) AND " +
            "(:endDate IS NULL OR e.entryDate <= :endDate) " +
            "ORDER BY e.entryDate DESC")
    Page<FinancialEntry> search(
            @Param("categoryId") UUID categoryId,
            @Param("entryType") EntryType entryType,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable
    );

    // Text search with optional filters
    @Query("SELECT e FROM FinancialEntry e WHERE " +
            "(LOWER(e.description) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(e.receiptNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) AND " +
            "(:entryType IS NULL OR e.entryType = :entryType) AND " +
            "(:startDate IS NULL OR e.entryDate >= :startDate) AND " +
            "(:endDate IS NULL OR e.entryDate <= :endDate)")
    Page<FinancialEntry> searchByText(
            @Param("searchTerm") String searchTerm,
            @Param("entryType") EntryType entryType,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable
    );

    // STATISTICS

    // Count by type and date range
    long countByEntryTypeAndEntryDateBetween(
            EntryType entryType,
            LocalDate startDate,
            LocalDate endDate
    );

    // Total amount by type
    @Query("SELECT COALESCE(SUM(e.baseAmount.amount), 0) FROM FinancialEntry e " +
            "WHERE e.entryType = :entryType AND e.entryDate BETWEEN :start AND :end")
    BigDecimal sumByEntryTypeAndDateRange(
            @Param("entryType") EntryType entryType,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );


    // PROJECTION INTERFACES

    interface PeriodTotalProjection {
        EntryType getEntryType();
        BigDecimal getTotal();
    }

    interface CategoryTotalProjection {
        String getCategoryName();
        BigDecimal getTotal();
    }

    interface MonthlyTotalProjection {
        Integer getYear();
        Integer getMonth();
        EntryType getEntryType();
        BigDecimal getTotal();
    }
}
