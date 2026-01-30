package com.marine.management.modules.finance.application.usecase;

import com.marine.management.modules.finance.application.mapper.ReportMapper;
import com.marine.management.modules.finance.domain.model.MonthlyBreakdown;
import com.marine.management.modules.finance.domain.model.MonthlyTotal;
import com.marine.management.modules.finance.domain.model.PeriodReport;
import com.marine.management.modules.finance.domain.vo.Period;
import com.marine.management.modules.finance.domain.enums.RecordType;
import com.marine.management.modules.finance.infrastructure.FinancialEntryRepository;
import com.marine.management.modules.finance.infrastructure.FinancialEntryRepository.FinancialEntryProjection;
import com.marine.management.modules.finance.presentation.dto.reports.PeriodBreakdownDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class GeneratePeriodReportUseCase {

    private final FinancialEntryRepository repository;
    private final ReportMapper reportMapper;

    public GeneratePeriodReportUseCase(
            FinancialEntryRepository repository,
            ReportMapper reportMapper
    ) {
        this.repository = repository;
        this.reportMapper = reportMapper;
    }

    public PeriodBreakdownDto execute(LocalDate startDate, LocalDate endDate) {
        Period period = new Period(startDate, endDate);

        // 1️⃣ Verileri çek
        List<FinancialEntryProjection> entries = repository.findByEntryDateBetweenOrderByEntryDateDesc(
                period.startDate(),
                period.endDate()
        );

        // 2️⃣ CategoryBreakdowns ve monthlyTotals hesapla
        List<MonthlyBreakdown> categoryBreakdowns = calculateCategoryBreakdowns(entries);
        Map<Integer, MonthlyTotal> monthlyTotals = calculateMonthlyTotals(entries);

        // 3️⃣ PeriodReport oluştur
        PeriodReport report = new PeriodReport(
                period,
                categoryBreakdowns,
                monthlyTotals,
                "EUR" // veya context'den al
        );

        // 4️⃣ DTO'ya map et
        return reportMapper.toPeriodBreakdownDto(report);
    }

    // ============================================
    // HELPERS
    // ============================================

    private List<MonthlyBreakdown> calculateCategoryBreakdowns(List<FinancialEntryProjection> entries) {
        // Category bazlı aylık toplam
        Map<String, Map<Integer, BigDecimal>> categoryMonthTotals = new HashMap<>();

        for (FinancialEntryProjection entry : entries) {
            String category = entry.getCategoryName();
            Integer month = entry.getEntryDate().getMonthValue();
            BigDecimal amount = entry.getBaseAmount(); // ✅ getAmount() → getBaseAmount()

            categoryMonthTotals
                    .computeIfAbsent(category, k -> new HashMap<>())
                    .merge(month, amount, BigDecimal::add);
        }

        // CategoryYearSummary benzeri MonthlyBreakdown listesi oluştur
        List<MonthlyBreakdown> breakdowns = new ArrayList<>();
        for (Map.Entry<String, Map<Integer, BigDecimal>> e : categoryMonthTotals.entrySet()) {
            String categoryName = e.getKey();
            Map<Integer, BigDecimal> monthlyValues = e.getValue();

            // Toplam yıl değeri
            BigDecimal yearTotal = monthlyValues.values().stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            breakdowns.add(new MonthlyBreakdown(categoryName, monthlyValues, yearTotal));
        }

        return breakdowns;
    }

    private Map<Integer, MonthlyTotal> calculateMonthlyTotals(List<FinancialEntryProjection> entries) {
        Map<Integer, BigDecimal> incomeByMonth = new HashMap<>();
        Map<Integer, BigDecimal> expenseByMonth = new HashMap<>();

        for (FinancialEntryProjection entry : entries) {
            Integer month = entry.getEntryDate().getMonthValue();
            BigDecimal amount = entry.getBaseAmount(); // ✅ getAmount() → getBaseAmount()

            if (entry.getEntryType() == RecordType.INCOME) {
                incomeByMonth.merge(month, amount, BigDecimal::add);
            } else {
                expenseByMonth.merge(month, amount, BigDecimal::add);
            }
        }

        // Cumulative hesapla
        Map<Integer, MonthlyTotal> totals = new HashMap<>();
        BigDecimal cumulative = BigDecimal.ZERO;
        for (int month = 1; month <= 12; month++) {
            BigDecimal income = incomeByMonth.getOrDefault(month, BigDecimal.ZERO);
            BigDecimal expense = expenseByMonth.getOrDefault(month, BigDecimal.ZERO);
            BigDecimal net = income.subtract(expense);
            cumulative = cumulative.add(net);

            totals.put(month, new MonthlyTotal(income, expense, cumulative));
        }

        return totals;
    }
}