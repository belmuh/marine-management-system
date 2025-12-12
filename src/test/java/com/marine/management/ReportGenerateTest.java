package com.marine.management;

import com.marine.management.modules.finance.domain.entity.FinancialCategory;
import com.marine.management.modules.finance.domain.entity.FinancialEntry;
import com.marine.management.modules.finance.domain.enums.PaymentMethod;
import com.marine.management.modules.finance.domain.enums.RecordType;
import com.marine.management.modules.finance.domain.model.AnnualReport;
import com.marine.management.modules.finance.domain.vo.EntryNumber;
import com.marine.management.modules.finance.domain.vo.Money;
import com.marine.management.modules.finance.domain.service.ReportGenerator;
import com.marine.management.modules.users.domain.User;
import com.marine.management.shared.kernel.security.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ReportGenerator Domain Service Tests")
class ReportGeneratorTest {

    private ReportGenerator reportGenerator;
    private User testUser;
    private FinancialCategory testCategory;
    private TestDataFactory testDataFactory;

    @BeforeEach
    void setUp() {
        reportGenerator = new ReportGenerator();
        testDataFactory = new TestDataFactory();
        testUser = testDataFactory.createTestUser();
        testCategory = testDataFactory.createTestCategory("Test Category");
    }

    @Nested
    @DisplayName("Annual Report Generation")
    class AnnualReportGenerationTests {

        @Test
        @DisplayName("Should generate annual report with correct totals")
        void should_generate_annual_report_with_correct_totals() {
            // Given
            List<FinancialEntry> entries = List.of(
                    testDataFactory.createIncomeEntry(
                            LocalDate.of(2025, 1, 15),
                            "1000",
                            testCategory,
                            "Client A",
                            "Turkey",
                            "Bodrum"
                    ),
                    testDataFactory.createExpenseEntry(
                            LocalDate.of(2025, 1, 20),
                            "500",
                            testCategory,
                            "Crew",
                            "Turkey",
                            "Bodrum",
                            "Market XYZ"
                    ),
                    testDataFactory.createExpenseEntry(
                            LocalDate.of(2025, 2, 10),
                            "300",
                            testCategory,
                            "Main Yacht",
                            "Turkey",
                            "Bodrum",
                            "Marina ABC"
                    )
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
            FinancialCategory salaryCategory = testDataFactory.createTestCategory("Salary");
            FinancialCategory marinaCategory = testDataFactory.createTestCategory("Marina");

            List<FinancialEntry> entries = List.of(
                    testDataFactory.createExpenseEntry(
                            LocalDate.of(2025, 1, 15),
                            "500",
                            salaryCategory,
                            "Crew",
                            "Turkey",
                            "Bodrum",
                            "Bank Transfer"
                    ),
                    testDataFactory.createExpenseEntry(
                            LocalDate.of(2025, 2, 10),
                            "300",
                            salaryCategory,
                            "Captain",
                            "Turkey",
                            "Bodrum",
                            "Bank Transfer"
                    ),
                    testDataFactory.createExpenseEntry(
                            LocalDate.of(2025, 3, 5),
                            "200",
                            marinaCategory,
                            "Main Yacht",
                            "Greece",
                            "Athens",
                            "Port Authority"
                    )
            );

            // When
            AnnualReport report = reportGenerator.generateAnnualReport(entries, 2025);

            // Then
            assertThat(report.getCategoryBreakdowns())
                    .hasSize(2)
                    .extracting("categoryName")
                    .containsExactlyInAnyOrder("Salary", "Marina");

            var salaryBreakdown = report.getCategoryBreakdowns().stream()
                    .filter(b -> "Salary".equals(b.getCategoryName()))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Salary breakdown not found"));

            assertThat(salaryBreakdown.getTotal()).isEqualByComparingTo("800");
        }

        @Test
        @DisplayName("Should calculate monthly totals correctly")
        void should_calculate_monthly_totals_correctly() {
            // Given
            List<FinancialEntry> entries = List.of(
                    testDataFactory.createIncomeEntry(
                            LocalDate.of(2025, 1, 15),
                            "1000",
                            testCategory,
                            "Charter",
                            "Turkey",
                            "Bodrum"
                    ),
                    testDataFactory.createExpenseEntry(
                            LocalDate.of(2025, 1, 20),
                            "500",
                            testCategory,
                            "Crew",
                            "Turkey",
                            "Bodrum",
                            "Market"
                    ),
                    testDataFactory.createExpenseEntry(
                            LocalDate.of(2025, 2, 10),
                            "300",
                            testCategory,
                            "Main Yacht",
                            "Turkey",
                            "Bodrum",
                            "Fuel Station"
                    )
            );

            // When
            AnnualReport report = reportGenerator.generateAnnualReport(entries, 2025);

            // Then
            // ✅ DÜZELTME: ReportGenerator tüm 12 ayı dönüyor (UI için iyi)
            assertThat(report.getMonthlyTotals()).hasSize(12);

            // January - İşlem var
            var januaryTotal = report.getMonthlyTotals().get(1);
            assertThat(januaryTotal.income()).isEqualByComparingTo("1000");
            assertThat(januaryTotal.expense()).isEqualByComparingTo("500");
            assertThat(januaryTotal.cumulative()).isEqualByComparingTo("500");

            // February - İşlem var
            var februaryTotal = report.getMonthlyTotals().get(2);
            assertThat(februaryTotal.income()).isEqualByComparingTo("0");
            assertThat(februaryTotal.expense()).isEqualByComparingTo("300");
            assertThat(februaryTotal.cumulative()).isEqualByComparingTo("200");

            // March - İşlem yok (0 olmalı)
            var marchTotal = report.getMonthlyTotals().get(3);
            assertThat(marchTotal.income()).isEqualByComparingTo("0");
            assertThat(marchTotal.expense()).isEqualByComparingTo("0");
            assertThat(marchTotal.cumulative()).isEqualByComparingTo("200"); // Cumulative devam ediyor
        }

        @Test
        @DisplayName("Should include only specified year entries")
        void should_include_only_specified_year_entries() {
            // Given
            List<FinancialEntry> entries = List.of(
                    testDataFactory.createExpenseEntry(
                            LocalDate.of(2024, 12, 31),
                            "100",
                            testCategory,
                            "Main Yacht",
                            "Turkey",
                            "Bodrum",
                            "Marina"
                    ),
                    testDataFactory.createExpenseEntry(
                            LocalDate.of(2025, 1, 1),
                            "200",
                            testCategory,
                            "Main Yacht",
                            "Turkey",
                            "Bodrum",
                            "Fuel"
                    ),
                    testDataFactory.createExpenseEntry(
                            LocalDate.of(2025, 11, 22),
                            "300",
                            testCategory,
                            "Crew",
                            "Turkey",
                            "Bodrum",
                            "Market"
                    )
            );

            // When
            AnnualReport report = reportGenerator.generateAnnualReport(entries, 2025);

            // Then
            assertThat(report.getGrandTotal()).isEqualByComparingTo("500"); // 200 + 300 (sadece 2025)
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle empty entries list with all 12 months initialized")
        void should_handle_empty_entries_list() {
            // Given
            List<FinancialEntry> entries = List.of();

            // When
            AnnualReport report = reportGenerator.generateAnnualReport(entries, 2025);

            // Then
            assertThat(report.getYear()).isEqualTo(2025);
            assertThat(report.getTotalIncome()).isEqualByComparingTo("0");
            assertThat(report.getGrandTotal()).isEqualByComparingTo("0");
            assertThat(report.getRemainingMoney()).isEqualByComparingTo("0");
            assertThat(report.getCategoryBreakdowns()).isEmpty();

            // ✅ DÜZELTME: Tüm 12 ay sıfırlarla dönmeli (UI için tutarlı)
            assertThat(report.getMonthlyTotals()).hasSize(12);

            // Her ay sıfır olmalı
            report.getMonthlyTotals().forEach((month, total) -> {
                assertThat(total.income())
                        .as("Month %d income should be 0", month)
                        .isEqualByComparingTo("0");
                assertThat(total.expense())
                        .as("Month %d expense should be 0", month)
                        .isEqualByComparingTo("0");
                assertThat(total.cumulative())
                        .as("Month %d cumulative should be 0", month)
                        .isEqualByComparingTo("0");
            });
        }

        @Test
        @DisplayName("Should filter entries by year correctly")
        void should_handle_entries_from_multiple_years() {
            // Given - Üç farklı yıl, hepsi geçmişte
            List<FinancialEntry> entries = List.of(
                    testDataFactory.createExpenseEntry(
                            LocalDate.of(2023, 6, 15),
                            "100",
                            testCategory,
                            "Main Yacht",
                            "Turkey",
                            "Bodrum",
                            "Marina 2023"
                    ),
                    testDataFactory.createExpenseEntry(
                            LocalDate.of(2024, 6, 15),
                            "200",
                            testCategory,
                            "Main Yacht",
                            "Turkey",
                            "Bodrum",
                            "Marina 2024"
                    ),
                    testDataFactory.createExpenseEntry(
                            LocalDate.of(2025, 6, 15),
                            "300",
                            testCategory,
                            "Main Yacht",
                            "Turkey",
                            "Bodrum",
                            "Marina 2025"
                    )
            );

            // When - 2025 raporu iste
            AnnualReport report2025 = reportGenerator.generateAnnualReport(entries, 2025);
            AnnualReport report2024 = reportGenerator.generateAnnualReport(entries, 2024);
            AnnualReport report2023 = reportGenerator.generateAnnualReport(entries, 2023);

            // Then - Her yıl sadece kendi entry'sini içermeli
            assertThat(report2025.getYear()).isEqualTo(2025);
            assertThat(report2025.getGrandTotal()).isEqualByComparingTo("300");

            assertThat(report2024.getYear()).isEqualTo(2024);
            assertThat(report2024.getGrandTotal()).isEqualByComparingTo("200");

            assertThat(report2023.getYear()).isEqualTo(2023);
            assertThat(report2023.getGrandTotal()).isEqualByComparingTo("100");
        }

        @Test
        @DisplayName("Should handle year with no matching entries")
        void should_handle_year_with_no_matching_entries() {
            // Given
            List<FinancialEntry> entries = List.of(
                    testDataFactory.createExpenseEntry(
                            LocalDate.of(2024, 1, 15),
                            "100",
                            testCategory,
                            "Main Yacht",
                            "Turkey",
                            "Bodrum",
                            "2024 expense"
                    ),
                    testDataFactory.createExpenseEntry(
                            LocalDate.of(2024, 6, 15),
                            "200",
                            testCategory,
                            "Main Yacht",
                            "Turkey",
                            "Bodrum",
                            "Another 2024 expense"
                    )
            );

            // When
            AnnualReport report2025 = reportGenerator.generateAnnualReport(entries, 2025);

            // Then
            assertThat(report2025.getYear()).isEqualTo(2025);
            assertThat(report2025.getGrandTotal()).isEqualByComparingTo("0");
            assertThat(report2025.getTotalIncome()).isEqualByComparingTo("0");
            assertThat(report2025.getRemainingMoney()).isEqualByComparingTo("0");
            assertThat(report2025.getCategoryBreakdowns()).isEmpty();

            // Tüm aylar sıfır olmalı
            assertThat(report2025.getMonthlyTotals()).hasSize(12);
            report2025.getMonthlyTotals().values().forEach(total -> {
                assertThat(total.income()).isEqualByComparingTo("0");
                assertThat(total.expense()).isEqualByComparingTo("0");
                assertThat(total.cumulative()).isEqualByComparingTo("0");
            });
        }
    }

    @Nested
    @DisplayName("Report Details")
    class ReportDetailsTests {

        @Test
        @DisplayName("Should correctly calculate remaining money")
        void should_correctly_calculate_remaining_money() {
            // Given
            List<FinancialEntry> entries = List.of(
                    testDataFactory.createIncomeEntry(
                            LocalDate.of(2025, 1, 1),
                            "5000",
                            testCategory,
                            "Charter",
                            "Turkey",
                            "Bodrum"
                    ),
                    testDataFactory.createExpenseEntry(
                            LocalDate.of(2025, 1, 2),
                            "2000",
                            testCategory,
                            "Main Yacht",
                            "Turkey",
                            "Bodrum",
                            "Fuel"
                    ),
                    testDataFactory.createExpenseEntry(
                            LocalDate.of(2025, 1, 3),
                            "1000",
                            testCategory,
                            "Crew",
                            "Turkey",
                            "Bodrum",
                            "Salaries"
                    )
            );

            // When
            AnnualReport report = reportGenerator.generateAnnualReport(entries, 2025);

            // Then
            assertThat(report.getRemainingMoney()).isEqualByComparingTo("2000"); // 5000 - (2000 + 1000)
        }

        @Test
        @DisplayName("Should handle negative remaining money (deficit)")
        void should_handle_negative_remaining_money_deficit() {
            // Given
            List<FinancialEntry> entries = List.of(
                    testDataFactory.createIncomeEntry(
                            LocalDate.of(2025, 1, 1),
                            "1000",
                            testCategory,
                            "Charter",
                            "Turkey",
                            "Bodrum"
                    ),
                    testDataFactory.createExpenseEntry(
                            LocalDate.of(2025, 1, 2),
                            "1500",
                            testCategory,
                            "Main Yacht",
                            "Turkey",
                            "Bodrum",
                            "Major Repair"
                    )
            );

            // When
            AnnualReport report = reportGenerator.generateAnnualReport(entries, 2025);

            // Then
            assertThat(report.getRemainingMoney()).isEqualByComparingTo("-500"); // 1000 - 1500
        }

        @Test
        @DisplayName("Should calculate cumulative totals correctly across months")
        void should_calculate_cumulative_totals_across_months() {
            // Given
            List<FinancialEntry> entries = List.of(
                    testDataFactory.createIncomeEntry(
                            LocalDate.of(2025, 1, 15),
                            "1000",
                            testCategory,
                            "Charter",
                            "Turkey",
                            "Bodrum"
                    ),
                    testDataFactory.createExpenseEntry(
                            LocalDate.of(2025, 2, 10),
                            "300",
                            testCategory,
                            "Crew",
                            "Turkey",
                            "Bodrum",
                            "Food"
                    ),
                    testDataFactory.createExpenseEntry(
                            LocalDate.of(2025, 3, 5),
                            "200",
                            testCategory,
                            "Main Yacht",
                            "Turkey",
                            "Bodrum",
                            "Fuel"
                    )
            );

            // When
            AnnualReport report = reportGenerator.generateAnnualReport(entries, 2025);

            // Then
            var jan = report.getMonthlyTotals().get(1);
            assertThat(jan.cumulative()).isEqualByComparingTo("1000");

            var feb = report.getMonthlyTotals().get(2);
            assertThat(feb.cumulative()).isEqualByComparingTo("700"); // 1000 - 300

            var mar = report.getMonthlyTotals().get(3);
            assertThat(mar.cumulative()).isEqualByComparingTo("500"); // 700 - 200

            // Sonraki aylar cumulative'i korumalı
            var apr = report.getMonthlyTotals().get(4);
            assertThat(apr.cumulative()).isEqualByComparingTo("500");
        }
    }

    // ============================================
    // TEST DATA FACTORY (Clean Code Principle)
    // ============================================

    private static class TestDataFactory {

        private int entryCounter = 1;

        public User createTestUser() {
            try {
                var constructor = User.class.getDeclaredConstructor();
                constructor.setAccessible(true);
                User user = constructor.newInstance();

                setField(user, "id", UUID.randomUUID());
                setField(user, "username", "testuser");
                setField(user, "email", "test@example.com");
                setField(user, "role", Role.ADMIN);

                return user;
            } catch (Exception e) {
                throw new TestDataCreationException("Failed to create test user", e);
            }
        }

        public FinancialCategory createTestCategory(String name) {
            try {
                var constructor = FinancialCategory.class.getDeclaredConstructor();
                constructor.setAccessible(true);
                FinancialCategory category = constructor.newInstance();

                setField(category, "id", UUID.randomUUID());
                setField(category, "code", name.toUpperCase().replace(" ", "_"));
                setField(category, "name", name);
                setField(category, "description", "Test category for " + name);
                setField(category, "isActive", true);
                setField(category, "isTechnical", name.equals("Marina") || name.equals("Fuel"));

                return category;
            } catch (Exception e) {
                throw new TestDataCreationException("Failed to create test category: " + name, e);
            }
        }

        public FinancialEntry createIncomeEntry(
                LocalDate date,
                String amount,
                FinancialCategory category,
                String source,
                String country,
                String city
        ) {
            EntryNumber entryNumber = EntryNumber.generate(entryCounter++);
            Money money = Money.of(amount, "EUR");

            return FinancialEntry.create(
                    entryNumber,
                    RecordType.INCOME,
                    category,
                    money,
                    date,
                    PaymentMethod.BANK_TRANSFER,
                    "Test income from " + source,
                    createTestUser(),
                    null,
                    null,
                    null,
                    "Türkiye",
                    null,
                    null,
                    null
            );
        }

        public FinancialEntry createExpenseEntry(
                LocalDate date,
                String amount,
                FinancialCategory category,
                String recipient,
                String country,
                String city,
                String vendor
        ) {
            EntryNumber entryNumber = EntryNumber.generate(entryCounter++);
            Money money = Money.of(amount, "EUR");

            return FinancialEntry.create(
                    entryNumber,
                    RecordType.EXPENSE,
                    category,
                    money,
                    date,
                    PaymentMethod.BANK_TRANSFER,
                    "Test expense for " + recipient,
                    createTestUser(),
                    null,
                    null,
                    null,
                    "Türkiye",
                    null,
                    null,
                    null
            );
        }

        private void setField(Object target, String fieldName, Object value) {
            try {
                var field = findField(target.getClass(), fieldName);
                field.setAccessible(true);
                field.set(target, value);
            } catch (Exception e) {
                throw new TestDataCreationException(
                        String.format("Failed to set field '%s' on %s", fieldName, target.getClass().getSimpleName()),
                        e
                );
            }
        }

        private java.lang.reflect.Field findField(Class<?> clazz, String fieldName) throws NoSuchFieldException {
            try {
                return clazz.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                if (clazz.getSuperclass() != null) {
                    return findField(clazz.getSuperclass(), fieldName);
                }
                throw e;
            }
        }
    }

    // ============================================
    // CUSTOM EXCEPTION (Clean Code)
    // ============================================

    private static class TestDataCreationException extends RuntimeException {
        public TestDataCreationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}