package com.marine.management.modules.finance.domain.entities;

import com.marine.management.modules.finance.TestDataBuilder;
import com.marine.management.modules.finance.domain.enums.EntryStatus;
import com.marine.management.modules.finance.domain.enums.PaymentMethod;
import com.marine.management.modules.finance.domain.enums.RecordType;
import com.marine.management.modules.finance.domain.vo.EntryNumber;
import com.marine.management.modules.finance.domain.vo.Money;
import com.marine.management.modules.users.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

/**
 * FinancialEntry domain entity için unit testler.
 *
 * ──────────────────────────────────────────────────────────────────
 * NEDEN ENTİTY TESTİ ÖNEMLİ?
 * ──────────────────────────────────────────────────────────────────
 * FinancialEntry bir Aggregate Root — tüm state machine (DRAFT → PAID)
 * bu sınıfın içinde yönetiliyor. Service katmanı izin kontrolü yapar,
 * entity iş kuralını uygular. Bu yüzden entity testleri:
 *
 * - "Hatalı state'de bu metod ne yapmalı?" sorularını yanıtlar
 * - Service mock'lamaya gerek kalmadan business logic'i doğrular
 * - Refactor sırasında davranış bozulunca hemen haber verir
 *
 * TestDataBuilder: test nesnelerini merkezi olarak üretir.
 * Reflection ile private ID field'larını set ediyor — JPA yerine geçiyor.
 * ──────────────────────────────────────────────────────────────────
 */
@DisplayName("FinancialEntry")
class FinancialEntryTest {

    private User crew;

    @BeforeEach
    void setUp() {
        crew = TestDataBuilder.createCrew(1L);
    }

    // ================================================================
    // OLUŞTURMA (CREATE)
    // ================================================================

    @Nested
    @DisplayName("Oluşturma")
    class Creation {

        @Test
        @DisplayName("Geçerli parametrelerle DRAFT entry oluşturulur")
        void shouldCreateDraftEntry_WithValidParameters() {
            FinancialEntry entry = TestDataBuilder.createDraftEntry(crew);

            assertThat(entry.getStatus()).isEqualTo(EntryStatus.DRAFT);
            assertThat(entry.getBaseAmount()).isNotNull();
            assertThat(entry.getPaidBaseAmount().isZero()).isTrue();
            assertThat(entry.getApprovedBaseAmount().isZero()).isTrue();
        }

        @Test
        @DisplayName("Oluşturulur oluşturulmaz ödeme sıfır, onay sıfır")
        void shouldHaveZeroPaymentAndApproval_OnCreation() {
            FinancialEntry entry = TestDataBuilder.createDraftEntry(crew);

            assertThat(entry.getPaidBaseAmount().isZero()).isTrue();
            assertThat(entry.getApprovedBaseAmount().isZero()).isTrue();
            assertThat(entry.hasApprovedAmount()).isFalse();
        }

        @Test
        @DisplayName("Sıfır amount → IllegalStateException (amount sıfırdan büyük olmalı)")
        void shouldThrow_WhenAmountIsZero() {
            // create() → validate() → "Amount must be positive"
            assertThatThrownBy(() ->
                TestDataBuilder.entry()
                    .creator(crew)
                    .amount(Money.zero("EUR"))
                    .build()
            )
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Amount must be positive");
        }

        @Test
        @DisplayName("Gelecek tarih → IllegalStateException")
        void shouldThrow_WhenEntryDateIsInFuture() {
            assertThatThrownBy(() ->
                FinancialEntry.create(
                    EntryNumber.generate(1),
                    RecordType.EXPENSE,
                    TestDataBuilder.createCategory(1L),
                    Money.of("100.00", "EUR"),
                    LocalDate.now().plusDays(1),   // ← gelecek
                    PaymentMethod.CASH,
                    "test",
                    null, null,
                    "Recipient", "TR", "IST", null, null,
                    "EUR"
                )
            )
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Entry date cannot be in the future");
        }

        @Test
        @DisplayName("isDraft() — yeni oluşturulan entry için true")
        void shouldBeDraft_WhenJustCreated() {
            assertThat(TestDataBuilder.createDraftEntry(crew).isDraft()).isTrue();
        }
    }

    // ================================================================
    // WORKFLOW: SUBMIT (TASLAK → ONAY BEKLİYOR)
    // ================================================================

    @Nested
    @DisplayName("Submit (Gönder)")
    class Submit {

        @Test
        @DisplayName("DRAFT → PENDING_CAPTAIN")
        void shouldTransitionToPendingCaptain_WhenSubmitted() {
            FinancialEntry entry = TestDataBuilder.createDraftEntry(crew);

            entry.submit();

            assertThat(entry.getStatus()).isEqualTo(EntryStatus.PENDING_CAPTAIN);
        }

        @Test
        @DisplayName("DRAFT → PENDING_MANAGER (submitToManager)")
        void shouldTransitionToPendingManager_WhenSubmittedToManager() {
            FinancialEntry entry = TestDataBuilder.createDraftEntry(crew);

            entry.submitToManager();

            assertThat(entry.getStatus()).isEqualTo(EntryStatus.PENDING_MANAGER);
        }

        @Test
        @DisplayName("DRAFT → APPROVED (submitAndApprove) + approvedAmount set")
        void shouldTransitionToApproved_WhenSubmitAndApprove() {
            FinancialEntry entry = TestDataBuilder.createDraftEntry(crew);
            Money baseAmount = entry.getBaseAmount();

            entry.submitAndApprove();

            assertThat(entry.getStatus()).isEqualTo(EntryStatus.APPROVED);
            assertThat(entry.getApprovedBaseAmount()).isEqualTo(baseAmount);
        }

        @Test
        @DisplayName("DRAFT olmayan entry submit edilemez → IllegalStateException")
        void shouldThrow_WhenSubmittingNonDraftEntry() {
            FinancialEntry entry = TestDataBuilder.createPendingCaptainEntry(crew);

            assertThatThrownBy(entry::submit)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Only DRAFT entries can be submitted");
        }
    }

    // ================================================================
    // WORKFLOW: CAPTAIN APPROVAL
    // ================================================================

    @Nested
    @DisplayName("Kaptan Onayı")
    class CaptainApproval {

        @Test
        @DisplayName("PENDING_CAPTAIN → APPROVED (manager gerekmez)")
        void shouldApprove_WhenNoManagerNeeded() {
            FinancialEntry entry = TestDataBuilder.createPendingCaptainEntry(crew);
            Money baseAmount = entry.getBaseAmount();

            entry.approveByCaptain(false);

            assertThat(entry.getStatus()).isEqualTo(EntryStatus.APPROVED);
            assertThat(entry.getApprovedBaseAmount()).isEqualTo(baseAmount);
        }

        @Test
        @DisplayName("PENDING_CAPTAIN → PENDING_MANAGER (manager gerekiyor)")
        void shouldSendToManager_WhenManagerNeeded() {
            FinancialEntry entry = TestDataBuilder.createPendingCaptainEntry(crew);

            entry.approveByCaptain(true);

            assertThat(entry.getStatus()).isEqualTo(EntryStatus.PENDING_MANAGER);
        }

        @Test
        @DisplayName("PENDING_CAPTAIN olmayan entry onaylanamaz → IllegalStateException")
        void shouldThrow_WhenNotPendingCaptain() {
            FinancialEntry entry = TestDataBuilder.createDraftEntry(crew);

            assertThatThrownBy(() -> entry.approveByCaptain(false))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not pending captain approval");
        }
    }

    // ================================================================
    // WORKFLOW: MANAGER APPROVAL
    // ================================================================

    @Nested
    @DisplayName("Müdür Onayı")
    class ManagerApproval {

        @Test
        @DisplayName("PENDING_MANAGER → APPROVED (tam onay, null geçildi)")
        void shouldFullyApprove_WhenNoAmountSpecified() {
            FinancialEntry entry = TestDataBuilder.createPendingManagerEntry(crew);
            Money expectedAmount = entry.getBaseAmount();

            entry.approveByManager();

            assertThat(entry.getStatus()).isEqualTo(EntryStatus.APPROVED);
            assertThat(entry.getApprovedBaseAmount()).isEqualTo(expectedAmount);
        }

        @Test
        @DisplayName("PENDING_MANAGER → APPROVED (kısmi onay)")
        void shouldPartiallyApprove_WhenAmountLessThanBase() {
            Money base = Money.of("600.00", "EUR");
            Money partial = Money.of("400.00", "EUR");
            FinancialEntry entry = TestDataBuilder.createPendingManagerEntryWithAmount(crew, base);

            entry.approveByManager(partial);

            assertThat(entry.getStatus()).isEqualTo(EntryStatus.APPROVED);
            assertThat(entry.getApprovedBaseAmount()).isEqualTo(partial);
        }

        @Test
        @DisplayName("Kısmi onay miktarı base amount'u geçemez → IllegalArgumentException")
        void shouldThrow_WhenApprovedAmountExceedsBase() {
            Money base = Money.of("300.00", "EUR");
            Money tooMuch = Money.of("500.00", "EUR");
            FinancialEntry entry = TestDataBuilder.createPendingManagerEntryWithAmount(crew, base);

            assertThatThrownBy(() -> entry.approveByManager(tooMuch))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot exceed requested amount");
        }

        @Test
        @DisplayName("PENDING_MANAGER olmayan entry onaylanamaz → IllegalStateException")
        void shouldThrow_WhenNotPendingManager() {
            FinancialEntry entry = TestDataBuilder.createPendingCaptainEntry(crew);

            assertThatThrownBy(() -> entry.approveByManager())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not pending manager approval");
        }
    }

    // ================================================================
    // WORKFLOW: REJECT (REDDET)
    // ================================================================

    @Nested
    @DisplayName("Reddetme")
    class Rejection {

        @Test
        @DisplayName("PENDING_CAPTAIN → REJECTED")
        void shouldReject_FromPendingCaptain() {
            FinancialEntry entry = TestDataBuilder.createPendingCaptainEntry(crew);

            entry.reject("Yetersiz belge");

            assertThat(entry.getStatus()).isEqualTo(EntryStatus.REJECTED);
            assertThat(entry.getRejectionReason()).isEqualTo("Yetersiz belge");
        }

        @Test
        @DisplayName("PENDING_MANAGER → REJECTED")
        void shouldReject_FromPendingManager() {
            FinancialEntry entry = TestDataBuilder.createPendingManagerEntry(crew);

            entry.reject("Bütçe aşıldı");

            assertThat(entry.getStatus()).isEqualTo(EntryStatus.REJECTED);
        }

        @Test
        @DisplayName("Pending olmayan entry reddedilemez → IllegalStateException")
        void shouldThrow_WhenRejectingNonPending() {
            FinancialEntry entry = TestDataBuilder.createDraftEntry(crew);

            assertThatThrownBy(() -> entry.reject("Sebep"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Only pending entries can be rejected");
        }

        @Test
        @DisplayName("Boş red sebebi → IllegalArgumentException")
        void shouldThrow_WhenReasonIsBlank() {
            FinancialEntry entry = TestDataBuilder.createPendingCaptainEntry(crew);

            assertThatThrownBy(() -> entry.reject(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Rejection reason is required");
        }

        @Test
        @DisplayName("Null red sebebi → IllegalArgumentException")
        void shouldThrow_WhenReasonIsNull() {
            FinancialEntry entry = TestDataBuilder.createPendingCaptainEntry(crew);

            assertThatThrownBy(() -> entry.reject(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Rejection reason is required");
        }
    }

    // ================================================================
    // ÖDEME (PAYMENT)
    // ================================================================

    /**
     * recordPayment: onaylı entry'ye kısmi veya tam ödeme kaydedilir.
     * Status otomatik güncellenir: APPROVED → PARTIALLY_PAID → PAID
     */
    @Nested
    @DisplayName("Ödeme Kaydı")
    class PaymentRecording {

        @Test
        @DisplayName("Kısmi ödeme → PARTIALLY_PAID")
        void shouldBecomePartiallyPaid_AfterPartialPayment() {
            Money approved = Money.of("500.00", "EUR");
            Money partial = Money.of("200.00", "EUR");
            FinancialEntry entry = TestDataBuilder.createApprovedEntryWithAmount(crew, approved);

            entry.recordPayment(partial);

            assertThat(entry.getStatus()).isEqualTo(EntryStatus.PARTIALLY_PAID);
            assertThat(entry.getPaidBaseAmount()).isEqualTo(partial);
        }

        @Test
        @DisplayName("Tam ödeme → PAID")
        void shouldBecomePaid_AfterFullPayment() {
            Money amount = Money.of("300.00", "EUR");
            FinancialEntry entry = TestDataBuilder.createApprovedEntryWithAmount(crew, amount);

            entry.recordPayment(amount);

            assertThat(entry.getStatus()).isEqualTo(EntryStatus.PAID);
            assertThat(entry.isFullyPaid()).isTrue();
        }

        @Test
        @DisplayName("Birden fazla kısmi ödeme birikerek tam ödemeye ulaşır → PAID")
        void shouldAccumulatePayments_UntilFullyPaid() {
            Money amount = Money.of("300.00", "EUR");
            FinancialEntry entry = TestDataBuilder.createApprovedEntryWithAmount(crew, amount);

            entry.recordPayment(Money.of("100.00", "EUR"));
            entry.recordPayment(Money.of("100.00", "EUR"));
            entry.recordPayment(Money.of("100.00", "EUR"));

            assertThat(entry.getStatus()).isEqualTo(EntryStatus.PAID);
        }

        @Test
        @DisplayName("Onaylanan miktarı aşan ödeme → IllegalArgumentException")
        void shouldThrow_WhenPaymentExceedsApprovedAmount() {
            Money approved = Money.of("200.00", "EUR");
            Money tooMuch = Money.of("300.00", "EUR");
            FinancialEntry entry = TestDataBuilder.createApprovedEntryWithAmount(crew, approved);

            assertThatThrownBy(() -> entry.recordPayment(tooMuch))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot exceed approved amount");
        }

        @Test
        @DisplayName("Onaylanmamış entry'ye ödeme yapılamaz → IllegalStateException")
        void shouldThrow_WhenEntryNotApproved() {
            FinancialEntry entry = TestDataBuilder.createPendingCaptainEntry(crew);

            assertThatThrownBy(() -> entry.recordPayment(Money.of("100.00", "EUR")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot record payment for non-approved entry");
        }

        @Test
        @DisplayName("Yanlış para birimi ile ödeme → IllegalArgumentException")
        void shouldThrow_WhenPaymentCurrencyMismatch() {
            FinancialEntry entry = TestDataBuilder.createApprovedEntryWithAmount(
                crew, Money.of("500.00", "EUR")
            );

            assertThatThrownBy(() -> entry.recordPayment(Money.of("500.00", "USD")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("base currency");
        }
    }

    // ================================================================
    // ÖDEME GERİ ALMA (REVERSAL)
    // ================================================================

    @Nested
    @DisplayName("Ödeme Geri Alma")
    class PaymentReversal {

        @Test
        @DisplayName("Tam ödeme geri alınırsa → APPROVED")
        void shouldReturnToApproved_WhenFullyReversed() {
            Money amount = Money.of("300.00", "EUR");
            FinancialEntry entry = TestDataBuilder.createFullyPaidEntry(crew, amount);

            entry.reversePayment(amount);

            assertThat(entry.getStatus()).isEqualTo(EntryStatus.APPROVED);
            assertThat(entry.getPaidBaseAmount().isZero()).isTrue();
        }

        @Test
        @DisplayName("Kısmi ödeme geri alınırsa → PARTIALLY_PAID")
        void shouldRemainPartiallyPaid_WhenPartiallyReversed() {
            Money approved = Money.of("500.00", "EUR");
            Money paid = Money.of("300.00", "EUR");
            Money reversed = Money.of("100.00", "EUR");
            FinancialEntry entry = TestDataBuilder.createPartiallyPaidEntry(crew, approved, paid);

            entry.reversePayment(reversed);

            assertThat(entry.getStatus()).isEqualTo(EntryStatus.PARTIALLY_PAID);
            assertThat(entry.getPaidBaseAmount()).isEqualTo(Money.of("200.00", "EUR"));
        }

        @Test
        @DisplayName("Ödenen miktarı aşan geri alma → IllegalStateException")
        void shouldThrow_WhenReversalExceedsPaidAmount() {
            Money amount = Money.of("200.00", "EUR");
            FinancialEntry entry = TestDataBuilder.createFullyPaidEntry(crew, amount);
            Money tooMuch = Money.of("300.00", "EUR");

            assertThatThrownBy(() -> entry.reversePayment(tooMuch))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Reversed amount cannot exceed total paid amount");
        }

        @Test
        @DisplayName("Ödeme yapılmamış entry'de geri alma → IllegalStateException (status uyumsuz)")
        void shouldThrow_WhenEntryNotPayable() {
            FinancialEntry entry = TestDataBuilder.createApprovedEntry(crew);

            assertThatThrownBy(() -> entry.reversePayment(Money.of("100.00", "EUR")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot reverse payment");
        }
    }

    // ================================================================
    // SORGULAMA METODLARİ (QUERY)
    // ================================================================

    @Nested
    @DisplayName("Sorgulama")
    class QueryMethods {

        @Test
        @DisplayName("isFullyPaid — tam ödenince true")
        void shouldReturnTrue_WhenFullyPaid() {
            Money amount = Money.of("100.00", "EUR");
            FinancialEntry entry = TestDataBuilder.createFullyPaidEntry(crew, amount);

            assertThat(entry.isFullyPaid()).isTrue();
        }

        @Test
        @DisplayName("isFullyPaid — kısmen ödenmişse false")
        void shouldReturnFalse_WhenPartiallyPaid() {
            Money approved = Money.of("500.00", "EUR");
            Money paid = Money.of("200.00", "EUR");
            FinancialEntry entry = TestDataBuilder.createPartiallyPaidEntry(crew, approved, paid);

            assertThat(entry.isFullyPaid()).isFalse();
        }

        @Test
        @DisplayName("isPartiallyPaid — kısmen ödenmişse true")
        void shouldReturnTrue_WhenPartiallyPaid() {
            Money approved = Money.of("500.00", "EUR");
            Money paid = Money.of("200.00", "EUR");
            FinancialEntry entry = TestDataBuilder.createPartiallyPaidEntry(crew, approved, paid);

            assertThat(entry.isPartiallyPaid()).isTrue();
        }

        @Test
        @DisplayName("getRemainingAmount — kalan tutarı doğru döner")
        void shouldReturnCorrectRemainingAmount() {
            Money approved = Money.of("500.00", "EUR");
            Money paid = Money.of("200.00", "EUR");
            FinancialEntry entry = TestDataBuilder.createPartiallyPaidEntry(crew, approved, paid);

            Money remaining = entry.getRemainingAmount();

            assertThat(remaining.getAmount()).isEqualByComparingTo("300.00");
        }

        @Test
        @DisplayName("isExpense / isIncome — entry type'a göre doğru")
        void shouldReturnCorrectEntryType() {
            FinancialEntry expense = TestDataBuilder.entry()
                .creator(crew)
                .type(RecordType.EXPENSE)
                .build();

            FinancialEntry income = TestDataBuilder.entry()
                .creator(crew)
                .type(RecordType.INCOME)
                .build();

            assertThat(expense.isExpense()).isTrue();
            assertThat(expense.isIncome()).isFalse();
            assertThat(income.isIncome()).isTrue();
            assertThat(income.isExpense()).isFalse();
        }

        @Test
        @DisplayName("getFullLocation — şehir ve ülke birleştirilir")
        void shouldCombineCityAndCountry() {
            FinancialEntry entry = FinancialEntry.create(
                EntryNumber.generate(1),
                RecordType.EXPENSE,
                TestDataBuilder.createCategory(1L),
                Money.of("100.00", "EUR"),
                LocalDate.now(),
                PaymentMethod.CASH,
                "test",
                null, null,
                "Recipient", "Turkey", "Istanbul", "Marina",
                null, "EUR"
            );

            String location = entry.getFullLocation();

            assertThat(location).contains("Istanbul").contains("Turkey").contains("Marina");
        }

        @Test
        @DisplayName("getFullLocation — tüm location alanları null ise null döner")
        void shouldReturnNull_WhenAllLocationFieldsAreNull() {
            FinancialEntry entry = FinancialEntry.create(
                EntryNumber.generate(1),
                RecordType.EXPENSE,
                TestDataBuilder.createCategory(1L),
                Money.of("100.00", "EUR"),
                LocalDate.now(),
                PaymentMethod.CASH,
                "test",
                null, null,
                null, null, null, null, null, "EUR"
            );

            assertThat(entry.getFullLocation()).isNull();
        }

        @Test
        @DisplayName("hasApprovedAmount — onay verilince true")
        void shouldReturnTrue_WhenApproved() {
            FinancialEntry entry = TestDataBuilder.createApprovedEntry(crew);

            assertThat(entry.hasApprovedAmount()).isTrue();
        }

        @Test
        @DisplayName("hasApprovedAmount — DRAFT'ta false")
        void shouldReturnFalse_WhenDraft() {
            FinancialEntry entry = TestDataBuilder.createDraftEntry(crew);

            assertThat(entry.hasApprovedAmount()).isFalse();
        }
    }
}
