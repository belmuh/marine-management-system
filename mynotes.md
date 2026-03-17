# My Notes


## TODO — findCarryOverBalance SQL filtresi

`FinancialEntryReportRepository.java` içinde `findCarryOverBalance()` sorgusu var.
Şu an status filtresi yorum satırına alınmış durumda:

```java
/* AND e.status IN ('APPROVED', 'PAID', 'PARTIALLY_PAID')*/
```

Bu filtre kapalı olduğu için PENDING, DRAFT, REJECTED gibi onaylanmamış entry'ler de
carry-over bakiyesine dahil oluyor — yani raporlarda yanlış başlangıç bakiyesi gösteriliyor.

**Düzeltme:** Aşağıdaki satırı aktif et:

```java
AND e.status IN ('APPROVED', 'PAID', 'PARTIALLY_PAID')
```

Veya JPQL olarak (entity enum kullanarak):

```java
AND e.status IN (
  com.marine.management.modules.finance.domain.enums.EntryStatus.APPROVED,
  com.marine.management.modules.finance.domain.enums.EntryStatus.PAID,
  com.marine.management.modules.finance.domain.enums.EntryStatus.PARTIALLY_PAID
)
```
