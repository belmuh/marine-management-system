package com.marine.management.modules.finance.domain.entities;

import com.marine.management.modules.finance.domain.vo.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

/**
 * Money value object için unit testler.
 *
 * ──────────────────────────────────────────────────────────────────
 * TEST MANTIĞI — TEMEL KAVRAMLAR
 * ──────────────────────────────────────────────────────────────────
 *
 * 1. Her @Test metodu tek bir davranışı test eder ("tek sorumluluk").
 *    İsim formatı: shouldDoX_WhenY — "Y durumunda X yapmalı"
 *
 * 2. Given / When / Then (AAA: Arrange / Act / Assert) yapısı:
 *    - Given  → test ortamını hazırla (veriler, mock'lar)
 *    - When   → test edilen kodu çalıştır
 *    - Then   → sonucu doğrula
 *
 * 3. AssertJ tercih edilir (JUnit'in assertXxx'ine göre daha okunabilir):
 *    assertThat(actual).isEqualTo(expected)
 *    assertThatThrownBy(() -> ...).isInstanceOf(X.class).hasMessageContaining("...")
 *
 * 4. @Nested sınıflar testleri gruplara ayırır → test çıktısı daha okunaklı.
 *
 * 5. Money bir Value Object: equals/hashCode amount+currency üzerine kurulu,
 *    mutable state yok, her işlem yeni bir Money döner.
 * ──────────────────────────────────────────────────────────────────
 */
@DisplayName("Money")
class MoneyTest {

    // ================================================================
    // OLUŞTURMA (CONSTRUCTION)
    // ================================================================

    /**
     * Factory method'ların ve constructor'ın doğru Money üretip üretmediğini test eder.
     *
     * Money'nin birden fazla oluşturma yolu var (of, ofMajor, ofMinor, zero).
     * Her birinin beklenen amount/currency'yi döndürdüğünü doğruluyoruz.
     */
    @Nested
    @DisplayName("Oluşturma")
    class Creation {

        @Test
        @DisplayName("String amount ile oluşturulabilir")
        void shouldCreateMoneyFromString() {
            // Given + When — tek satırda çünkü çok basit
            Money money = Money.of("100.50", "EUR");

            // Then
            assertThat(money.getAmount()).isEqualByComparingTo("100.50");
            assertThat(money.getCurrencyCode()).isEqualTo("EUR");
        }

        @Test
        @DisplayName("BigDecimal amount ile oluşturulabilir")
        void shouldCreateMoneyFromBigDecimal() {
            Money money = Money.of(new BigDecimal("250.00"), "USD");

            assertThat(money.getAmount()).isEqualByComparingTo("250.00");
            assertThat(money.getCurrencyCode()).isEqualTo("USD");
        }

        @Test
        @DisplayName("ofMajor — tam birim (100 EUR → 100.00)")
        void shouldCreateFromMajorUnit() {
            // Given — 100 EUR (tam lira/euro cinsinden)
            Money money = Money.ofMajor(100L, "EUR");

            // Then — 2 ondalık hane ile saklanmalı
            assertThat(money.getAmount()).isEqualByComparingTo("100.00");
        }

        @Test
        @DisplayName("ofMinor — kuruş cinsinden (10050 → 100.50 EUR)")
        void shouldCreateFromMinorUnit() {
            // Given — 10050 kuruş = 100.50 EUR
            Money money = Money.ofMinor(10050L, "EUR");

            assertThat(money.getAmount()).isEqualByComparingTo("100.50");
        }

        @Test
        @DisplayName("zero — sıfır para oluşturur")
        void shouldCreateZeroMoney() {
            Money money = Money.zero("TRY");

            assertThat(money.isZero()).isTrue();
            assertThat(money.getCurrencyCode()).isEqualTo("TRY");
        }

        @Test
        @DisplayName("Currency code küçük harf verilse büyük harfe çevrilir")
        void shouldNormalizeCurrencyCodeToUpperCase() {
            // Given — kullanıcı küçük harf girmiş
            Money money = Money.of("50.00", "eur");

            // Then — içeride büyük harf olmalı
            assertThat(money.getCurrencyCode()).isEqualTo("EUR");
        }

        @Test
        @DisplayName("Amount 2 ondalık haneye yuvarlanır (HALF_EVEN)")
        void shouldRoundAmountToTwoDecimalPlaces() {
            // Given — 3 ondalık hane
            // HALF_EVEN (banker's rounding): 100.125 → 100.12 (5 çift'e yuvarlanır)
            Money money = Money.of("100.125", "EUR");

            assertThat(money.getAmount()).isEqualByComparingTo("100.12");
        }
    }

    // ================================================================
    // DOĞRULAMA (VALIDATION)
    // ================================================================

    /**
     * Geçersiz girdilerde ne olduğunu test eder.
     *
     * "Negatif testler" olarak da bilinir — sistemin hatalı durumları
     * doğru Exception ile reddettiğini doğrularız.
     *
     * assertThatThrownBy(() -> ...) → lambda içindeki kod exception fırlatmalı;
     * fırlatmazsa test FAIL olur.
     */
    @Nested
    @DisplayName("Doğrulama")
    class Validation {

        @Test
        @DisplayName("Null amount → IllegalArgumentException")
        void shouldThrow_WhenAmountIsNull() {
            assertThatThrownBy(() -> Money.of((BigDecimal) null, "EUR"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Amount cannot be null");
        }

        @Test
        @DisplayName("Negatif amount → IllegalArgumentException")
        void shouldThrow_WhenAmountIsNegative() {
            assertThatThrownBy(() -> Money.of("-1.00", "EUR"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Amount cannot be negative");
        }

        @Test
        @DisplayName("Null currency code → IllegalArgumentException")
        void shouldThrow_WhenCurrencyCodeIsNull() {
            assertThatThrownBy(() -> Money.of("100.00", null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Currency code is required");
        }

        @Test
        @DisplayName("Boş currency code → IllegalArgumentException")
        void shouldThrow_WhenCurrencyCodeIsEmpty() {
            assertThatThrownBy(() -> Money.of("100.00", ""))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Currency code is required");
        }

        @Test
        @DisplayName("2 karakterli currency code → IllegalArgumentException (ISO 4217 = 3 karakter)")
        void shouldThrow_WhenCurrencyCodeIsTwoChars() {
            assertThatThrownBy(() -> Money.of("100.00", "EU"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("3 characters");
        }

        @Test
        @DisplayName("Geçersiz currency code (XYZ) → IllegalArgumentException")
        void shouldThrow_WhenCurrencyCodeIsInvalid() {
            assertThatThrownBy(() -> Money.of("100.00", "XYZ"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid currency code");
        }

        @Test
        @DisplayName("Sıfır amount geçerlidir")
        void shouldAcceptZeroAmount() {
            // Sıfır para geçerli — "henüz ödeme yapılmadı" gibi durumlar için
            assertThatCode(() -> Money.of("0.00", "EUR"))
                    .doesNotThrowAnyException();
        }
    }

    // ================================================================
    // ARİTMETİK İŞLEMLER
    // ================================================================

    /**
     * Money nesneleri immutable'dır: her işlem yeni bir Money döner,
     * orijinal nesne değişmez. Bu testler hem hesabı hem de immutability'yi doğrular.
     */
    @Nested
    @DisplayName("Aritmetik")
    class Arithmetic {

        // ---------- TOPLAMA ----------

        @Test
        @DisplayName("Aynı para birimi toplanabilir")
        void shouldAddMoneyWithSameCurrency() {
            // Given
            Money a = Money.of("100.00", "EUR");
            Money b = Money.of("50.00", "EUR");

            // When
            Money result = a.add(b);

            // Then
            assertThat(result.getAmount()).isEqualByComparingTo("150.00");
            assertThat(result.getCurrencyCode()).isEqualTo("EUR");
        }

        @Test
        @DisplayName("Farklı para birimi toplanamaz → IllegalArgumentException")
        void shouldThrow_WhenAddingDifferentCurrencies() {
            Money eur = Money.of("100.00", "EUR");
            Money usd = Money.of("100.00", "USD");

            assertThatThrownBy(() -> eur.add(usd))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Currency mismatch");
        }

        @Test
        @DisplayName("Toplama orijinal nesneyi değiştirmez (immutability)")
        void shouldNotMutateOriginalOnAdd() {
            // Given
            Money original = Money.of("100.00", "EUR");

            // When — sonucu alıyoruz ama original'a atamıyoruz
            original.add(Money.of("50.00", "EUR"));

            // Then — original aynı kalmalı
            assertThat(original.getAmount()).isEqualByComparingTo("100.00");
        }

        // ---------- ÇIKARMA ----------

        @Test
        @DisplayName("Küçük miktardan büyük miktar çıkarılabilir (aynı para birimi)")
        void shouldSubtractMoneyWithSameCurrency() {
            Money a = Money.of("100.00", "EUR");
            Money b = Money.of("30.00", "EUR");

            Money result = a.subtract(b);

            assertThat(result.getAmount()).isEqualByComparingTo("70.00");
        }

        @Test
        @DisplayName("Çıkarma sonucu negatif olursa → IllegalArgumentException (amount < 0 kabul edilmez)")
        void shouldThrow_WhenSubtractResultIsNegative() {
            // Bu önemli bir business kuralı:
            // Money her zaman >= 0; "eksi para" bu modelde temsil edilemez.
            Money a = Money.of("30.00", "EUR");
            Money b = Money.of("100.00", "EUR");

            assertThatThrownBy(() -> a.subtract(b))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Amount cannot be negative");
        }

        @Test
        @DisplayName("Eşit miktarları çıkarmak sıfır verir")
        void shouldReturnZero_WhenSubtractingEqualAmounts() {
            Money a = Money.of("100.00", "EUR");
            Money b = Money.of("100.00", "EUR");

            Money result = a.subtract(b);

            assertThat(result.isZero()).isTrue();
        }

        @Test
        @DisplayName("Farklı para birimi çıkarılamaz → IllegalArgumentException")
        void shouldThrow_WhenSubtractingDifferentCurrencies() {
            Money eur = Money.of("100.00", "EUR");
            Money usd = Money.of("50.00", "USD");

            assertThatThrownBy(() -> eur.subtract(usd))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Currency mismatch");
        }

        // ---------- ÇARPMA ----------

        @Test
        @DisplayName("BigDecimal çarpanı ile çarpılabilir")
        void shouldMultiplyByBigDecimal() {
            Money money = Money.of("100.00", "EUR");

            Money result = money.multiply(new BigDecimal("1.5"));

            assertThat(result.getAmount()).isEqualByComparingTo("150.00");
        }

        @Test
        @DisplayName("long çarpanı ile çarpılabilir")
        void shouldMultiplyByLong() {
            Money money = Money.of("25.00", "EUR");

            Money result = money.multiply(4L);

            assertThat(result.getAmount()).isEqualByComparingTo("100.00");
        }

        @Test
        @DisplayName("Sıfır ile çarpım → sıfır")
        void shouldReturnZero_WhenMultipliedByZero() {
            Money money = Money.of("100.00", "EUR");

            Money result = money.multiply(0L);

            assertThat(result.isZero()).isTrue();
        }

        // ---------- BÖLME ----------

        @Test
        @DisplayName("BigDecimal bölenine bölünebilir")
        void shouldDivideByBigDecimal() {
            Money money = Money.of("100.00", "EUR");

            Money result = money.divide(new BigDecimal("4"));

            assertThat(result.getAmount()).isEqualByComparingTo("25.00");
        }

        @Test
        @DisplayName("long bölenine bölünebilir")
        void shouldDivideByLong() {
            Money money = Money.of("90.00", "EUR");

            Money result = money.divide(3L);

            assertThat(result.getAmount()).isEqualByComparingTo("30.00");
        }

        @Test
        @DisplayName("Sıfıra bölme → IllegalArgumentException")
        void shouldThrow_WhenDividingByZero() {
            Money money = Money.of("100.00", "EUR");

            assertThatThrownBy(() -> money.divide(BigDecimal.ZERO))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Cannot divide by zero");
        }

        @Test
        @DisplayName("Tam bölünmez sonuç HALF_EVEN ile yuvarlanır")
        void shouldRoundOnDivision() {
            // 100 / 3 = 33.3333... → HALF_EVEN ile 33.33
            Money money = Money.of("100.00", "EUR");

            Money result = money.divide(3L);

            assertThat(result.getAmount()).isEqualByComparingTo("33.33");
        }

        // ---------- NEGATE / ABS ----------

        @Test
        @DisplayName("abs() pozitif amount üzerinde değişiklik yapmaz")
        void shouldReturnSameAmount_WhenAbsOfPositive() {
            Money money = Money.of("100.00", "EUR");

            Money result = money.abs();

            assertThat(result.getAmount()).isEqualByComparingTo("100.00");
        }

        @Test
        @DisplayName("abs() sıfır üzerinde sıfır döner")
        void shouldReturnZero_WhenAbsOfZero() {
            Money result = Money.zero("EUR").abs();

            assertThat(result.isZero()).isTrue();
        }
    }

    // ================================================================
    // KARŞILAŞTIRMA (COMPARISON)
    // ================================================================

    /**
     * isZero, isPositive, isNegative, isGreaterThan vb. metotları test eder.
     *
     * NOT: isNegative() her zaman false döner çünkü Money < 0 oluşturulamaz.
     * Bu da negate() sorunuyla aynı kökten geliyor.
     */
    @Nested
    @DisplayName("Karşılaştırma")
    class Comparison {

        @Test
        @DisplayName("isZero — sıfır para için true")
        void shouldReturnTrue_WhenZero() {
            assertThat(Money.zero("EUR").isZero()).isTrue();
        }

        @Test
        @DisplayName("isZero — pozitif para için false")
        void shouldReturnFalse_WhenPositive() {
            assertThat(Money.of("0.01", "EUR").isZero()).isFalse();
        }

        @Test
        @DisplayName("isPositive — sıfırdan büyük için true")
        void shouldReturnTrue_WhenPositive() {
            assertThat(Money.of("100.00", "EUR").isPositive()).isTrue();
        }

        @Test
        @DisplayName("isPositive — sıfır için false")
        void shouldReturnFalse_WhenZeroAndIsPositive() {
            assertThat(Money.zero("EUR").isPositive()).isFalse();
        }

        @Test
        @DisplayName("isGreaterThan — büyük olan için true")
        void shouldReturnTrue_WhenGreaterThan() {
            Money big   = Money.of("200.00", "EUR");
            Money small = Money.of("100.00", "EUR");

            assertThat(big.isGreaterThan(small)).isTrue();
            assertThat(small.isGreaterThan(big)).isFalse();
        }

        @Test
        @DisplayName("isGreaterThanOrEqual — eşit olan için true")
        void shouldReturnTrue_WhenEqual_ForGreaterThanOrEqual() {
            Money a = Money.of("100.00", "EUR");
            Money b = Money.of("100.00", "EUR");

            assertThat(a.isGreaterThanOrEqual(b)).isTrue();
        }

        @Test
        @DisplayName("isLessThan — küçük olan için true")
        void shouldReturnTrue_WhenLessThan() {
            Money small = Money.of("50.00", "EUR");
            Money big   = Money.of("100.00", "EUR");

            assertThat(small.isLessThan(big)).isTrue();
            assertThat(big.isLessThan(small)).isFalse();
        }

        @Test
        @DisplayName("isLessThanOrEqual — eşit olan için true")
        void shouldReturnTrue_WhenEqual_ForLessThanOrEqual() {
            Money a = Money.of("100.00", "EUR");
            Money b = Money.of("100.00", "EUR");

            assertThat(a.isLessThanOrEqual(b)).isTrue();
        }

        @Test
        @DisplayName("Farklı para birimleri karşılaştırılamaz → IllegalArgumentException")
        void shouldThrow_WhenComparingDifferentCurrencies() {
            Money eur = Money.of("100.00", "EUR");
            Money usd = Money.of("100.00", "USD");

            assertThatThrownBy(() -> eur.isGreaterThan(usd))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Currency mismatch");
        }

        @Test
        @DisplayName("hasSameCurrency — aynı para birimi için true")
        void shouldReturnTrue_ForSameCurrency() {
            Money a = Money.of("100.00", "EUR");
            Money b = Money.of("200.00", "EUR");

            assertThat(a.hasSameCurrency(b)).isTrue();
        }

        @Test
        @DisplayName("isCurrency — büyük/küçük harf farkı gözetmez")
        void shouldBeCaseInsensitive_ForIsCurrency() {
            Money money = Money.of("100.00", "EUR");

            assertThat(money.isCurrency("eur")).isTrue();
            assertThat(money.isCurrency("EUR")).isTrue();
        }
    }

    // ================================================================
    // PARA BİRİMİ DÖNÜŞÜMÜ (CURRENCY CONVERSION)
    // ================================================================

    @Nested
    @DisplayName("Para Birimi Dönüşümü")
    class CurrencyConversion {

        @Test
        @DisplayName("Geçerli kur ile dönüştürülebilir")
        void shouldConvert_WithValidRate() {
            // Given — 100 EUR, kur: 1 EUR = 1.10 USD
            Money eur = Money.of("100.00", "EUR");
            BigDecimal rate = new BigDecimal("1.10");

            // When
            Money usd = eur.convertUsing(rate, "USD");

            // Then
            assertThat(usd.getAmount()).isEqualByComparingTo("110.00");
            assertThat(usd.getCurrencyCode()).isEqualTo("USD");
        }

        @Test
        @DisplayName("Null kur → NullPointerException")
        void shouldThrow_WhenRateIsNull() {
            Money money = Money.of("100.00", "EUR");

            assertThatThrownBy(() -> money.convertUsing(null, "USD"))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Exchange rate cannot be null");
        }

        @Test
        @DisplayName("Sıfır kur → IllegalArgumentException")
        void shouldThrow_WhenRateIsZero() {
            Money money = Money.of("100.00", "EUR");

            assertThatThrownBy(() -> money.convertUsing(BigDecimal.ZERO, "USD"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Exchange rate must be positive");
        }

        @Test
        @DisplayName("Negatif kur → IllegalArgumentException")
        void shouldThrow_WhenRateIsNegative() {
            Money money = Money.of("100.00", "EUR");

            assertThatThrownBy(() -> money.convertUsing(new BigDecimal("-1.5"), "USD"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Exchange rate must be positive");
        }

        @Test
        @DisplayName("Null hedef para birimi → NullPointerException")
        void shouldThrow_WhenTargetCurrencyIsNull() {
            Money money = Money.of("100.00", "EUR");

            assertThatThrownBy(() -> money.convertUsing(new BigDecimal("1.10"), null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Target currency cannot be null");
        }

        @Test
        @DisplayName("isEuro — EUR için true")
        void shouldReturnTrue_ForEuro() {
            assertThat(Money.of("100.00", "EUR").isEuro()).isTrue();
        }

        @Test
        @DisplayName("isEuro — USD için false")
        void shouldReturnFalse_ForNonEuro() {
            assertThat(Money.of("100.00", "USD").isEuro()).isFalse();
        }
    }

    // ================================================================
    // DÖNÜŞÜM METODLARI (CONVERSION)
    // ================================================================

    @Nested
    @DisplayName("Major / Minor Dönüşüm")
    class Conversion {

        @Test
        @DisplayName("getAmountMajor — tam birimi döner")
        void shouldReturnMajorUnit() {
            Money money = Money.of("123.45", "EUR");

            assertThat(money.getAmountMajor()).isEqualTo(123L);
        }

        @Test
        @DisplayName("getAmountMinor — kuruş cinsinden döner")
        void shouldReturnMinorUnit() {
            Money money = Money.of("123.45", "EUR");

            // 123.45 EUR = 12345 kuruş
            assertThat(money.getAmountMinor()).isEqualTo(12345L);
        }

        @Test
        @DisplayName("ofMinor → getAmountMinor gidiş-dönüş tutarlı")
        void shouldBeRoundTrip_ForMinorUnit() {
            long originalMinor = 9999L;

            Money money = Money.ofMinor(originalMinor, "EUR");

            assertThat(money.getAmountMinor()).isEqualTo(originalMinor);
        }
    }

    // ================================================================
    // EŞİTLİK ve HASH (EQUALITY & HASH)
    // ================================================================

    /**
     * equals() ve hashCode() testleri — Value Object için kritik.
     *
     * Value Object semantiği:
     * - İki Money nesnesi aynı amount + currency'ye sahipse eşittir.
     * - Object identity (== referans) önemli değil.
     *
     * Java'da equals() override edilince hashCode() da override edilmeli;
     * bu testler her ikisini de doğrular.
     */
    @Nested
    @DisplayName("Eşitlik ve Hash")
    class EqualityAndHash {

        @Test
        @DisplayName("Aynı amount ve currency → eşit")
        void shouldBeEqual_WhenSameAmountAndCurrency() {
            Money a = Money.of("100.00", "EUR");
            Money b = Money.of("100.00", "EUR");

            // Value Object: referanslar farklı ama değerler aynı → eşit olmalı
            assertThat(a).isEqualTo(b);
        }

        @Test
        @DisplayName("Farklı amount → eşit değil")
        void shouldNotBeEqual_WhenDifferentAmounts() {
            Money a = Money.of("100.00", "EUR");
            Money b = Money.of("200.00", "EUR");

            assertThat(a).isNotEqualTo(b);
        }

        @Test
        @DisplayName("Farklı currency → eşit değil")
        void shouldNotBeEqual_WhenDifferentCurrencies() {
            Money eur = Money.of("100.00", "EUR");
            Money usd = Money.of("100.00", "USD");

            assertThat(eur).isNotEqualTo(usd);
        }

        @Test
        @DisplayName("Eşit Money'lerin hashCode'u aynı olmalı")
        void shouldHaveSameHashCode_WhenEqual() {
            Money a = Money.of("100.00", "EUR");
            Money b = Money.of("100.00", "EUR");

            // HashMap/HashSet'te doğru çalışması için gerekli
            assertThat(a.hashCode()).isEqualTo(b.hashCode());
        }

        @Test
        @DisplayName("null ile eşit değil")
        void shouldNotBeEqual_ToNull() {
            Money money = Money.of("100.00", "EUR");

            assertThat(money).isNotEqualTo(null);
        }

        @Test
        @DisplayName("Farklı tipte nesne ile eşit değil")
        void shouldNotBeEqual_ToDifferentType() {
            Money money = Money.of("100.00", "EUR");

            assertThat(money).isNotEqualTo("100.00 EUR");
        }
    }

    // ================================================================
    // FORMAT
    // ================================================================

    @Nested
    @DisplayName("Format")
    class Formatting {

        @Test
        @DisplayName("format() — currency sembolü ve miktarı içerir")
        void shouldFormatWithCurrencySymbol() {
            Money money = Money.of("1500.00", "EUR");

            String formatted = money.format();

            // EUR sembolü ve miktar içermeli
            assertThat(formatted).contains("€").contains("1500");
        }

        @Test
        @DisplayName("toString() — amount ve currency içerir")
        void shouldIncludeAmountAndCurrencyInToString() {
            Money money = Money.of("100.00", "EUR");

            String str = money.toString();

            assertThat(str).contains("100.00").contains("EUR");
        }
    }
}
