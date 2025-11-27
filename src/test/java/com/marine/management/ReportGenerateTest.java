package com.marine.management;

import com.marine.management.modules.finance.domain.EntryNumber;
import com.marine.management.modules.finance.domain.EntryType;
import com.marine.management.modules.finance.domain.FinancialCategory;
import com.marine.management.modules.finance.domain.FinancialEntry;
import com.marine.management.modules.finance.domain.model.AnnualReport;
import com.marine.management.modules.finance.domain.model.Money;
import com.marine.management.modules.finance.domain.service.ReportGenerator;
import com.marine.management.modules.users.domain.User;
import com.marine.management.shared.kernel.security.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ReportGenerator Domain Service Tests")
class ReportGeneratorTest {

    private ReportGenerator reportGenerator;
    private User testUser;
    private FinancialCategory testCategory;

    @BeforeEach
    void setUp() {
        reportGenerator = new ReportGenerator();
        testUser = createTestUser();
        testCategory = createTestCategory("Test Category");
    }

    @Test
    @DisplayName("Should generate annual report with correct totals")
    void should_generate_annual_report_with_correct_totals() {
        // Given
        var entries = List.of(
                createEntry(LocalDate.of(2025, 1, 15), EntryType.INCOME, "1000"),
                createEntry(LocalDate.of(2025, 1, 20), EntryType.EXPENSE, "500"),
                createEntry(LocalDate.of(2025, 2, 10), EntryType.EXPENSE, "300")
        );

        // When
        AnnualReport report = reportGenerator.generateAnnualReport(entries, 2025);

        // Then
        assertThat(report.getYear()).isEqualTo(2025);
        assertThat(report.getTotalIncome()).isEqualByComparingTo("1000");
        assertThat(report.getGrandTotal()).isEqualByComparingTo("800");
        assertThat(report.getRemainingMoney()).isEqualByComparingTo("200");
    }

    @Test
    @DisplayName("Should group expenses by category")
    void should_group_expenses_by_category() {
        // Given
        var salaryCategory = createTestCategory("Salary");
        var marinaCategory = createTestCategory("Marina");

        var entries = List.of(
                createEntry(LocalDate.of(2025, 1, 15), EntryType.EXPENSE, "500", salaryCategory),
                createEntry(LocalDate.of(2025, 2, 10), EntryType.EXPENSE, "300", salaryCategory),
                createEntry(LocalDate.of(2025, 3, 5), EntryType.EXPENSE, "200", marinaCategory)
        );

        // When
        AnnualReport report = reportGenerator.generateAnnualReport(entries, 2025);

        // Then
        assertThat(report.getCategoryBreakdowns()).hasSize(2);

        var salaryBreakdown = report.getCategoryBreakdowns().stream()
                .filter(b -> b.getCategoryName().equals("Salary"))
                .findFirst()
                .orElseThrow();

        assertThat(salaryBreakdown.getTotal()).isEqualByComparingTo("800");
    }

    @Test
    @DisplayName("Should calculate monthly totals correctly")
    void should_calculate_monthly_totals_correctly() {
        // Given
        var entries = List.of(
                createEntry(LocalDate.of(2025, 1, 15), EntryType.INCOME, "1000"),
                createEntry(LocalDate.of(2025, 1, 20), EntryType.EXPENSE, "500"),
                createEntry(LocalDate.of(2025, 2, 10), EntryType.EXPENSE, "300")
        );

        // When
        AnnualReport report = reportGenerator.generateAnnualReport(entries, 2025);

        // Then
        var januaryTotal = report.getMonthlyTotals().get(1);
        assertThat(januaryTotal.income()).isEqualByComparingTo("1000");
        assertThat(januaryTotal.expense()).isEqualByComparingTo("500");
        assertThat(januaryTotal.cumulative()).isEqualByComparingTo("500");

        var februaryTotal = report.getMonthlyTotals().get(2);
        assertThat(februaryTotal.income()).isEqualByComparingTo("0");
        assertThat(februaryTotal.expense()).isEqualByComparingTo("300");
        assertThat(februaryTotal.cumulative()).isEqualByComparingTo("-300");
    }

    @Test
    @DisplayName("Should handle empty entries list")
    void should_handle_empty_entries_list() {
        // Given
        var entries = List.<FinancialEntry>of();

        // When
        AnnualReport report = reportGenerator.generateAnnualReport(entries, 2025);

        // Then
        assertThat(report.getYear()).isEqualTo(2025);
        assertThat(report.getTotalIncome()).isEqualByComparingTo("0");
        assertThat(report.getGrandTotal()).isEqualByComparingTo("0");
        assertThat(report.getCategoryBreakdowns()).isEmpty();
    }

    @Test
    @DisplayName("Should include only specified year entries")
    void should_include_only_specified_year_entries() {
        // Given
        var entries = List.of(
                createEntry(LocalDate.of(2024, 12, 31), EntryType.EXPENSE, "100"),
                createEntry(LocalDate.of(2025, 1, 1), EntryType.EXPENSE, "200"),
                createEntry(LocalDate.of(2025, 11, 22), EntryType.EXPENSE, "300")
                //createEntry(LocalDate.of(2026, 1, 1), EntryType.EXPENSE, "400")
        );

        // When
        AnnualReport report = reportGenerator.generateAnnualReport(entries, 2025);

        // Then
        assertThat(report.getGrandTotal()).isEqualByComparingTo("500"); // Only 200 + 300
    }

    // ============================================
    // HELPER METHODS
    // ============================================

    private FinancialEntry createEntry(LocalDate date, EntryType type, String amount) {
        return createEntry(date, type, amount, testCategory);
    }

    private FinancialEntry createEntry(
            LocalDate date,
            EntryType type,
            String amount,
            FinancialCategory category
    ) {
        // Generate unique entry number
        EntryNumber entryNumber = EntryNumber.generate(1);

        // Create Money object
        Money money = Money.of(amount, "EUR");

        // Create entry using factory method
        return FinancialEntry.create(
                entryNumber,
                type,
                category,
                money,
                date,
                testUser,
                "Test entry for " + category.getName()
        );
    }

    private User createTestUser() {
        try {
            // Get the protected constructor
            Constructor<User> constructor = User.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            User user = constructor.newInstance();

            setField(user, "id", UUID.randomUUID());
            setField(user, "username", "testuser");
            setField(user, "email", "test@example.com");
            setField(user, "role", Role.ADMIN);
            return user;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create test user", e);
        }
    }

    private FinancialCategory createTestCategory(String name) {
        try {
            // Get the protected constructor
            Constructor<FinancialCategory> constructor = FinancialCategory.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            FinancialCategory category = constructor.newInstance();

            setField(category, "id", UUID.randomUUID());
            setField(category, "name", name);
            setField(category, "description", "Test category");
            return category;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create test category", e);
        }
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set field: " + fieldName, e);
        }
    }
}