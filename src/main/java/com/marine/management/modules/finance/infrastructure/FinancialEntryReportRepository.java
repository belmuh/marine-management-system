package com.marine.management.modules.finance.infrastructure;

import com.marine.management.modules.finance.domain.enums.RecordType;
import com.marine.management.modules.finance.domain.entity.FinancialEntry;
import com.marine.management.modules.finance.domain.model.PivotReportProjection;
import com.marine.management.modules.finance.domain.model.TreeReportProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface FinancialEntryReportRepository extends JpaRepository<FinancialEntry, UUID> {

    // ============================================
    // PERIOD TOTALS
    // ============================================

    @Query("""
        SELECT e.entryType as entryType, 
               SUM(e.baseAmount.amount) as total 
        FROM FinancialEntry e 
        WHERE e.entryDate BETWEEN :start AND :end 
        GROUP BY e.entryType
    """)
    List<PeriodTotalProjection> findPeriodTotals(
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );

    // ============================================
    // CATEGORY REPORTS
    // ============================================

    @Query("""
        SELECT c.id as categoryId,
               c.code as categoryCode,
               c.name as categoryName,
               c.technical as technical,
               SUM(e.baseAmount.amount) as total, 
               COUNT(e.id) as entryCount 
        FROM FinancialEntry e 
        JOIN e.category c
        WHERE e.entryType = :entryType 
        AND e.entryDate BETWEEN :start AND :end 
        GROUP BY c.id, c.code, c.name, c.technical
        ORDER BY SUM(e.baseAmount.amount) DESC
    """)
    List<CategoryTotalProjection> findCategoryTotals(
            @Param("entryType") RecordType entryType,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );

    default List<CategoryTotalProjection> findExpenseTotals(LocalDate start, LocalDate end) {
        return findCategoryTotals(RecordType.EXPENSE, start, end);
    }

    default List<CategoryTotalProjection> findIncomeTotals(LocalDate start, LocalDate end) {
        return findCategoryTotals(RecordType.INCOME, start, end);
    }

    // ============================================
    // WHO REPORTS
    // ============================================

    @Query("""
        SELECT w.id as whoId,
               w.code as whoCode,
               w.nameTr as whoNameTr,
               w.nameEn as whoNameEn,
               w.technical as technical,
               SUM(e.baseAmount.amount) as total, 
               COUNT(e.id) as entryCount 
        FROM FinancialEntry e 
        JOIN e.tenantWho tw
        JOIN tw.who w
        WHERE e.entryType = :entryType 
        AND e.entryDate BETWEEN :start AND :end 
        GROUP BY w.id, w.code, w.nameTr, w.nameEn, w.technical
        ORDER BY SUM(e.baseAmount.amount) DESC
    """)
    List<WhoTotalProjection> findWhoTotals(
            @Param("entryType") RecordType entryType,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );

    // ============================================
    // MAIN CATEGORY REPORTS
    // ============================================

    @Query("""
        SELECT mc.id as mainCategoryId,
               mc.code as mainCategoryCode,
               mc.nameTr as mainCategoryNameTr,
               mc.nameEn as mainCategoryNameEn,
               mc.technical as technical,
               SUM(e.baseAmount.amount) as total, 
               COUNT(e.id) as entryCount 
        FROM FinancialEntry e 
        JOIN e.tenantMainCategory tmc
        JOIN tmc.mainCategory mc
        WHERE e.entryType = :entryType 
        AND e.entryDate BETWEEN :start AND :end 
        GROUP BY mc.id, mc.code, mc.nameTr, mc.nameEn, mc.technical
        ORDER BY SUM(e.baseAmount.amount) DESC
    """)
    List<MainCategoryTotalProjection> findMainCategoryTotals(
            @Param("entryType") RecordType entryType,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );

    // ============================================
    // TREE REPORT PROJECTION (RECORD CLASS)
    // ============================================

    @Query("""
        SELECT new com.marine.management.modules.finance.domain.model.TreeReportProjection(
            mc.id,
            mc.code,
            mc.nameTr,
            mc.nameEn,
            mc.technical,
            c.id,
            c.code,
            c.name,
            c.technical,
            w.id,
            w.code,
            w.nameTr,
            w.nameEn,
            w.technical,
            SUM(e.baseAmount.amount)
        )
        FROM FinancialEntry e
        JOIN e.category c
        LEFT JOIN e.tenantMainCategory tmc
        LEFT JOIN tmc.mainCategory mc
        LEFT JOIN e.tenantWho tw
        LEFT JOIN tw.who w
        WHERE e.entryType = :entryType
        AND e.entryDate BETWEEN :startDate AND :endDate
        GROUP BY mc.id, mc.code, mc.nameTr, mc.nameEn, mc.technical,
                 c.id, c.code, c.name, c.technical,
                 w.id, w.code, w.nameTr, w.nameEn, w.technical
        ORDER BY mc.code NULLS LAST, c.code, w.code NULLS LAST
    """)
    List<TreeReportProjection> findTreeProjections(
            @Param("entryType") RecordType entryType,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    // ============================================
    // PIVOT REPORT PROJECTION (RECORD CLASS)
    // ============================================

    @Query("""
        SELECT new com.marine.management.modules.finance.domain.model.PivotReportProjection(
            mc.id,
            mc.code,
            mc.nameTr,
            mc.nameEn,
            mc.technical,
            c.id,
            c.code,
            c.name,
            c.technical,
            w.id,
            w.code,
            w.nameTr,
            w.nameEn,
            w.technical,
            EXTRACT(MONTH FROM e.entryDate),
            SUM(e.baseAmount.amount)
        )
        FROM FinancialEntry e
        JOIN e.category c
        LEFT JOIN e.tenantMainCategory tmc
        LEFT JOIN tmc.mainCategory mc
        LEFT JOIN e.tenantWho tw
        LEFT JOIN tw.who w
        WHERE e.entryType = :entryType
        AND EXTRACT(YEAR FROM e.entryDate) = :year
        GROUP BY mc.id, mc.code, mc.nameTr, mc.nameEn, mc.technical,
                 c.id, c.code, c.name, c.technical,
                 w.id, w.code, w.nameTr, w.nameEn, w.technical,
                 EXTRACT(MONTH FROM e.entryDate)
        ORDER BY mc.code NULLS LAST, c.code, w.code NULLS LAST, EXTRACT(MONTH FROM e.entryDate)
    """)
    List<PivotReportProjection> findPivotProjections(
            @Param("entryType") RecordType entryType,
            @Param("year") int year
    );

    // ============================================
    // MONTHLY REPORTS
    // ============================================

    @Query("""
        SELECT EXTRACT(YEAR FROM e.entryDate) as year, 
               EXTRACT(MONTH FROM e.entryDate) as month, 
               e.entryType as entryType, 
               SUM(e.baseAmount.amount) as total, 
               COUNT(e.id) as entryCount 
        FROM FinancialEntry e 
        WHERE e.entryDate BETWEEN :start AND :end 
        GROUP BY EXTRACT(YEAR FROM e.entryDate), 
                 EXTRACT(MONTH FROM e.entryDate), 
                 e.entryType 
        ORDER BY year, month
    """)
    List<MonthlyTotalProjection> findMonthlyTotals(
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );

    @Query("""
        SELECT c.id as categoryId,
               c.name as categoryName, 
               EXTRACT(MONTH FROM e.entryDate) as month, 
               SUM(e.baseAmount.amount) as total 
        FROM FinancialEntry e 
        JOIN e.category c
        WHERE e.entryType = :entryType 
        AND EXTRACT(YEAR FROM e.entryDate) = :year 
        GROUP BY c.id, c.name, EXTRACT(MONTH FROM e.entryDate) 
        ORDER BY c.name, month
    """)
    List<CategoryMonthBreakdownProjection> findCategoryMonthBreakdown(
            @Param("entryType") RecordType entryType,
            @Param("year") int year
    );

    @Query("""
        SELECT EXTRACT(MONTH FROM e.entryDate) as month, 
               e.entryType as entryType, 
               SUM(e.baseAmount.amount) as total 
        FROM FinancialEntry e 
        WHERE EXTRACT(YEAR FROM e.entryDate) = :year 
        GROUP BY EXTRACT(MONTH FROM e.entryDate), e.entryType 
        ORDER BY month
    """)
    List<MonthlyIncomeExpenseProjection> findMonthlyIncomeExpense(
            @Param("year") int year
    );

    @Query("""
        SELECT mc.id as mainCategoryId,
               EXTRACT(MONTH FROM e.entryDate) as month, 
               SUM(e.baseAmount.amount) as total, 
               COUNT(e.id) as entryCount 
        FROM FinancialEntry e 
        JOIN e.tenantMainCategory tmc
        JOIN tmc.mainCategory mc
        WHERE e.entryType = :entryType 
        AND EXTRACT(YEAR FROM e.entryDate) = :year 
        GROUP BY mc.id, EXTRACT(MONTH FROM e.entryDate) 
        ORDER BY mc.id, month
    """)
    List<MainCategoryMonthBreakdownProjection> findMainCategoryMonthBreakdown(
            @Param("entryType") RecordType entryType,
            @Param("year") int year
    );

    // ============================================
    // AGGREGATES
    // ============================================

    @Query("""
        SELECT COALESCE(SUM(e.baseAmount.amount), 0) 
        FROM FinancialEntry e 
        WHERE e.entryType = :entryType 
        AND e.entryDate BETWEEN :start AND :end
    """)
    BigDecimal sumByEntryTypeAndDateRange(
            @Param("entryType") RecordType entryType,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );

    @Query("""
        SELECT COUNT(e) FROM FinancialEntry e 
        WHERE e.tenantWho IS NOT NULL 
        AND e.tenantMainCategory IS NOT NULL 
        AND e.entryDate BETWEEN :start AND :end
    """)
    long countDetailedEntries(
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );

    // ============================================
    // PROJECTION INTERFACES (Dashboard için basit interface projections)
    // ============================================

    interface PeriodTotalProjection {
        RecordType getEntryType();
        BigDecimal getTotal();
    }

    interface CategoryTotalProjection {
        UUID getCategoryId();
        String getCategoryCode();
        String getCategoryName();
        Boolean getTechnical();
        BigDecimal getTotal();
        Long getEntryCount();
    }

    interface WhoTotalProjection {
        Long getWhoId();
        String getWhoCode();
        String getWhoNameTr();
        String getWhoNameEn();
        Boolean getTechnical();
        BigDecimal getTotal();
        Long getEntryCount();
    }

    interface MainCategoryTotalProjection {
        Long getMainCategoryId();
        String getMainCategoryCode();
        String getMainCategoryNameTr();
        String getMainCategoryNameEn();
        Boolean getTechnical();
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

    interface CategoryMonthBreakdownProjection {
        UUID getCategoryId();
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
}