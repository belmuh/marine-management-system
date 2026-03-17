# Code Review — SOLID & Clean Code Analizi

**Proje:** marine-management-system
**Tarih:** 2026-03-18
**Kapsam:** 35 dosya, 611 ekleme / 280 silme

---

## Ne Değişti — Özet

Bu commit 4 ayrı iş konusunu bir arada getirmiş:

1. **Auth → Permission tabanlı yanıt** — Login ve refresh token cevaplarına `permissions: Set<String>` eklendi, `LoginResponse.java` silindi
2. **Dosya depolama → Cloudflare R2** — Yerel disk yerine S3-uyumlu R2 kullanımına geçildi
3. **Ödeme → Aggregate Root doğru uygulandı** — `FinancialEntry` üzerinden `addPayment()` / `removePayment()` ile kontrol sağlandı
4. **Raporlar → Carry-over bakiye** — Dönem başındaki birikimli bakiye raporlara eklendi; `FinancialEntryReportRepository` doğru kullanılmaya başlandı

---

## ✅ İyi Yapılanlar

### 1. Aggregate Root Pattern (DDD) — Ödeme
`FinancialEntry` artık kendi `payments` koleksiyonunu yönetiyor. Servis doğrudan `paymentRepository.save()` yerine `entry.addPayment()` çağırıyor; cascade bunu hallediyor.

```java
// Önce (kötü) — aggregate dışından mutation
paymentRepository.save(payment);
entry.recordPayment(amount);

// Sonra (iyi) — aggregate içinden kontrol
entry.addPayment(payment);
entryRepository.save(entry);
```

Bu DDD Aggregate Root prensibinin doğru uygulamasıdır. Ödeme silerek bakiye geri alma (`removePayment`) da aynı şekilde aggregate üzerinden geçiyor.

---

### 2. Interface Segregation — GeneratePeriodReportUseCase
`GeneratePeriodReportUseCase` artık `FinancialEntryRepository` yerine `FinancialEntryReportRepository` kullanıyor. Daha önce tüm entity'yi çekip Java'da hesaplıyordu; şimdi projection'larla DB'de aggregate ediliyor. Doğru ayrıştırma.

---

### 3. Factory Method + Immutable DTO
`AuthResult.from()` ve `AuthResponse.from()` factory method pattern'i doğru kullanılmış; record'lar immutable.

---

### 4. LoginResponse Silindi — DRY
`LoginResponse.java` ile `AuthResponse.java` aynı bilgiyi taşıyan iki farklı DTO'ydu. Birini kaldırmak doğru karar.

---

### 5. R2 Disable Modu — Geliştirme/Prod Ayrımı
`app.r2.enabled=false` ile local ortamda R2 bağlantısı olmadan uygulama ayağa kalkıyor. Bu pragmatik bir çözüm.

---

### 6. UserController — Role → Permission Geçişi
`hasAnyRole('ADMIN')` yerine `hasAuthority('USER_MANAGE')` kullanımı doğru yön. Rol bazlı değil izin bazlı erişim kontrolü daha granüler ve esnek.

---

## ⚠️ Sorunlar

### 1. DRY İhlali — Permission Hesabı 3 Yerde Tekrar Ediyor

Aynı 3 satır kod `AuthResult.from()`, `AuthResponse.from()` ve `AuthController.refreshToken()` içinde kopyalanmış:

```java
// Bu kod 3 farklı yerde birebir aynı
user.getRoleEnum()
    .getAllPermissions()
    .stream()
    .map(Permission::name)
    .collect(Collectors.toUnmodifiableSet());
```

**Öneri:** Küçük bir utility method ya da `AuthResult.from()` döndükten sonra `AuthResponse.from(authResult)` şeklinde tek nokta.

```java
// AuthController.refreshToken() içinde şöyle olabilir:
AuthResult result = AuthResult.from(newAccessToken, ..., jwtUtil.getExpirationMs(), ...);
RefreshTokenResponse response = new RefreshTokenResponse(
    result.accessToken(),
    jwtUtil.getExpirationMs(),
    result.permissions()   // ← tekrar hesaplamak yerine AuthResult'tan al
);
```

---

### 2. OCP İhlali — FileStorageService İçine Gömülü R2 Bağımlılığı

`FileStorageService` somut olarak R2'ye bağlı. İleride Azure Blob ya da S3 eklemek gerekirse sınıfı değiştirmek gerekecek.

**Öneri:** Bir `StoragePort` interface'i çıkar:

```java
public interface StoragePort {
    String store(MultipartFile file, String entryNumber, AttachmentType type, int seq);
    String getDownloadUrl(String key);
    void delete(String key);
}

@Service
public class R2StorageAdapter implements StoragePort { ... }

@Service
@ConditionalOnProperty(name = "app.r2.enabled", havingValue = "false")
public class NoOpStorageAdapter implements StoragePort { ... }
```

Bu hem OCP'yi çözer hem de `Optional<S3Client>` enjeksiyonu gibi garip yapıyı kaldırır.

---

### 3. `ALLOWED_EXTENSIONS` ve `ALLOWED_CONTENT_TYPES` Sync Problemi

İkisi de ayrı sabitler olarak tanımlanmış ve kodda `// Must stay in sync` yorumu var. Bu bir code smell'dir — ileride biri güncellenir diğeri unutulur.

**Öneri:** Tek bir `enum AttachmentMimeType` oluştur ve hem content-type hem extension oradan türesin:

```java
public enum AttachmentMimeType {
    PDF("application/pdf", ".pdf"),
    JPEG("image/jpeg", ".jpg", ".jpeg"),
    ...;

    public String contentType() { ... }
    public Set<String> extensions() { ... }
}
```

---

### 4. Yorum Satırına Alınmış SQL — Clean Code

`FinancialEntryReportRepository.findCarryOverBalance()` içinde:

```java
/* AND e.status IN ('APPROVED', 'PAID', 'PARTIALLY_PAID')*/
```

Bu filtre kasıtlı olarak devre dışı bırakılmış ama yorum olarak bırakılmış. Bu hem kafa karıştırıcı hem de yanlış veri döndürmesine neden olabilir (PENDING/DRAFT entry'ler carry-over'a dahil oluyor).

**Öneri:** Ya aktif et ya da neden devre dışı bırakıldığını açıklayan bir TODO yaz:

```java
// TODO: status filtresi şimdilik kapalı — tüm entry'ler dahil ediliyor.
// Sadece APPROVED/PAID dahil edilmesi gerekiyorsa aşağıdaki satırı aç:
// AND e.status IN ('APPROVED', 'PAID', 'PARTIALLY_PAID')
```

---

### 5. `allowsPaymentReversal()` Biçimlendirme

```java
// Şu an
public boolean allowsPaymentReversal() { return this == PARTIALLY_PAID || this == PAID;}

// Proje geri kalanıyla tutarlı olması için
public boolean allowsPaymentReversal() {
    return this == PARTIALLY_PAID || this == PAID;
}
```

Küçük ama codebase'in geri kalanıyla tutarsız.

---

### 6. SecurityConfig ile UserController Arasında Potansiyel Tutarsızlık

`SecurityConfig`'de CAPTAIN rolü `/api/users/**`'a eklendi:

```java
.hasAnyRole("SUPER_ADMIN", "ADMIN", "MANAGER", "CAPTAIN")
```

Ama `UserController`'da endpoint'ler `hasAuthority('USER_VIEW')` ve `hasAuthority('USER_MANAGE')` ile korunuyor. CAPTAIN rolünün bu permission'lara sahip olup olmadığı `Permission` enum'una bakılmadan bilinemez.

**Yapılacak:** CAPTAIN'ın `USER_VIEW` permission'ına sahip olduğunu doğrula. Yoksa CAPTAIN kullanıcılar 403 alır (HTTP level geçer ama method security bloker).

---

### 7. `AuthResult` Paket Yeri Yanlış

`AuthResult.java` şu an `domain.commands` altında. Commands genellikle input nesneleridir; `AuthResult` bir output/response. Daha doğru yer: `auth.application.dto` veya `auth.domain.model`.

---

## 📋 Commit Mesajı Önerisi

Tüm bu değişikliklere bakarak doğru commit mesajı şu:

```
feat: add R2 file storage, carry-over balance in reports, and permission-based auth response

- Replace local disk storage with Cloudflare R2; add no-op mode for local dev
- Add AttachmentType enum and DB migration (V004)
- Return permissions set in login/refresh responses; remove redundant LoginResponse DTO
- Enforce aggregate root pattern for payments (addPayment/removePayment on FinancialEntry)
- Add carry-over balance to annual and period reports
- Migrate period report to use FinancialEntryReportRepository projections
- Add status filter to EntrySearchCriteria and EntrySearchRequest
- Switch UserController from role-based to permission-based authorization
- Allow CAPTAIN role to access /api/users endpoint
```

---

## Öncelik Sırası

| # | Sorun | Risk | Efor |
|---|-------|------|------|
| 1 | Yorum satırındaki SQL filtresi (`carry-over`) | Yüksek — yanlış veri | Düşük |
| 2 | Permission hesabı 3 yerde tekrar | Orta — bakım sorunu | Düşük |
| 3 | SecurityConfig ↔ CAPTAIN permission uyumu | Orta — 403 riski | Düşük |
| 4 | `StoragePort` interface çıkarma | Düşük — gelecek esnekliği | Orta |
| 5 | MIME/extension tek kaynak | Düşük — bakım sorunu | Orta |
