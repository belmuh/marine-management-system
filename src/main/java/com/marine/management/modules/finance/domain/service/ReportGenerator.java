package com.marine.management.modules.finance.domain.service;

import com.marine.management.modules.finance.domain.EntryType;
import com.marine.management.modules.finance.domain.FinancialEntry;
import com.marine.management.modules.finance.domain.model.AnnualReport;
import com.marine.management.modules.finance.domain.model.MonthlyBreakdown;
import com.marine.management.modules.finance.domain.model.Period;
import com.marine.management.modules.finance.domain.model.PeriodReport;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

@Component
public class ReportGenerator {

    /**
     * Generates an annual financial report
     *
     * @param entries All financial entries
     * @param year Target year
     * @return Annual report with category breakdowns and monthly totals
     */
    public AnnualReport generateAnnualReport(List<FinancialEntry> entries, int year) {
        Period period = Period.ofYear(year);
        List<FinancialEntry> yearEntries = filterEntriesByPeriod(entries, period);

        List<MonthlyBreakdown> categoryBreakdowns = generateCategoryBreakdowns(yearEntries);
        Map<Integer, AnnualReport.MonthlyTotal> monthlyTotals = generateAnnualMonthlyTotals(yearEntries);

        return new AnnualReport(year, categoryBreakdowns, monthlyTotals);
    }

    /**
     * Generates a period financial report
     *
     * @param entries All financial entries
     * @param period Target period
     * @return Period report with category breakdowns and monthly totals
     */
    public PeriodReport generatePeriodReport(List<FinancialEntry> entries, Period period) {
        List<FinancialEntry> periodEntries = filterEntriesByPeriod(entries, period);

        List<MonthlyBreakdown> categoryBreakdowns = generateCategoryBreakdowns(periodEntries);
        Map<Integer, PeriodReport.MonthlyTotal> monthlyTotals =
                generatePeriodMonthlyTotals(periodEntries, period);

        return new PeriodReport(period, categoryBreakdowns, monthlyTotals);
    }

    private List<FinancialEntry> filterEntriesByPeriod(
            List<FinancialEntry> entries,
            Period period
    ) {
        return entries.stream()
                .filter(entry -> period.contains(entry.getEntryDate()))
                .toList();
    }

    private List<MonthlyBreakdown> generateCategoryBreakdowns(List<FinancialEntry> entries) {
        Map<String, MonthlyBreakdown> categoryMap = new HashMap<>();

        entries.stream()
                .filter(entry -> entry.getEntryType() == EntryType.EXPENSE)
                .forEach(entry -> {
                    String categoryName = entry.getCategory().getName();
                    int month = entry.getEntryDate().getMonthValue();
                    BigDecimal amount = entry.getBaseAmount().amount();

                    categoryMap
                            .computeIfAbsent(categoryName, MonthlyBreakdown::new)
                            .addAmount(month, amount);
                });

        return new ArrayList<>(categoryMap.values());
    }

    private Map<Integer, AnnualReport.MonthlyTotal> generateAnnualMonthlyTotals(
            List<FinancialEntry> entries
    ) {
        Map<Integer, BigDecimal> monthlyIncome = new HashMap<>();
        Map<Integer, BigDecimal> monthlyExpense = new HashMap<>();

        entries.forEach(entry -> {
            int month = entry.getEntryDate().getMonthValue();
            BigDecimal amount = entry.getBaseAmount().amount();

            if (entry.getEntryType() == EntryType.INCOME) {
                monthlyIncome.merge(month, amount, BigDecimal::add);
            } else {
                monthlyExpense.merge(month, amount, BigDecimal::add);
            }
        });

        BigDecimal cumulative = BigDecimal.ZERO;
        Map<Integer, AnnualReport.MonthlyTotal> totals = new HashMap<>();
        for (int month = 1; month <= 12; month++) {
            BigDecimal income = monthlyIncome.getOrDefault(month, BigDecimal.ZERO);
            BigDecimal expense = monthlyExpense.getOrDefault(month, BigDecimal.ZERO);
            BigDecimal net = income.subtract(expense);
            cumulative = cumulative.add(net);
            totals.put(month, new AnnualReport.MonthlyTotal(income, expense, cumulative));
        }

        return totals;
    }

    private Map<Integer, PeriodReport.MonthlyTotal> generatePeriodMonthlyTotals(
            List<FinancialEntry> entries,
            Period period
    ) {
        Map<Integer, BigDecimal> monthlyIncome = new HashMap<>();
        Map<Integer, BigDecimal> monthlyExpense = new HashMap<>();

        entries.forEach(entry -> {
            int month = entry.getEntryDate().getMonthValue();
            BigDecimal amount = entry.getBaseAmount().amount();

            if (entry.getEntryType() == EntryType.INCOME) {
                monthlyIncome.merge(month, amount, BigDecimal::add);
            } else {
                monthlyExpense.merge(month, amount, BigDecimal::add);
            }
        });

        BigDecimal cumulative = BigDecimal.ZERO;
        Map<Integer, PeriodReport.MonthlyTotal> totals = new HashMap<>();
        int startMonth = period.startDate().getMonthValue();
        int endMonth = period.endDate().getMonthValue();

        for (int month = startMonth; month <= endMonth; month++) {
            BigDecimal income = monthlyIncome.getOrDefault(month, BigDecimal.ZERO);
            BigDecimal expense = monthlyExpense.getOrDefault(month, BigDecimal.ZERO);
            BigDecimal net = income.subtract(expense);
            cumulative = cumulative.add(net);
            totals.put(month, new PeriodReport.MonthlyTotal(income, expense, cumulative));
        }

        return totals;
    }
}