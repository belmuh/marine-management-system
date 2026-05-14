# My Notes

---

## ✅ DONE — ReportingCategory + Status Filtresi Refactoru

- `ReportingCategory.java` enum oluşturuldu (ACTUAL, COMMITTED, NON_FINANCIAL)
- `EntryStatus.java` → `getReportingCategory()` + `ACTUAL_STATUSES` + `COMMITTED_STATUSES` eklendi
- `FinancialEntrySpecs.java` → `approved()` → `actualEntries()`, `pendingApproval()` → `committedEntries()` rename
- `FinancialEntrySpecs.java` → `forFinancialReports(User)` eklendi (rol bazlı rapor görünürlüğü)
- `FinancialEntryReportRepository.java` → 13 sorguya `AND e.status IN :statuses` eklendi
- `FinancialReportService.java` → `getActualTotal()`, `getCommittedTotal()`, `getCarryOverBalance()` eklendi
- `countDetailedEntries` dead code silindi

---

## ✅ DONE — Yacht Registration Wizard (v2)

### Mimari (iki aşamalı kayıt)

**Aşama 1 — Kayıt (`/register` → `register.ts`)**
- Kullanıcı: yachtName + ad/soyad/email/şifre girer
- `registerUser()` → `POST /api/auth/register` → email doğrulama gönderilir
- Org `onboardingCompleted = false` olarak oluşturulur

**Aşama 2 — Setup Wizard (`/setup` → `setup.ts`)**
- Email doğrulaması sonrası login → `onboardingCompleted = false` → `/setup` yönlendirmesi
- 3 adımlı wizard: Yat Detayları / Finansal Ayarlar / Kategori & WHO Seçimi
- `completeSetup()` → `POST /api/onboarding/setup` (authenticated)
- Dashboard'a yönlendirme

### Tamamlanan bileşenler
- `YachtType.java` enum ✅
- `RegisterYachtCommand.java` (tüm validationlar) ✅
- `OrganizationOnboardingService.java` (user creation + ref data init + ADMIN enforce) ✅
- `OnboardingController.java` (`POST /register`, `POST /setup`, `GET /reference-data`) ✅
- `setup.ts` + `setup.html` (3 adımlı wizard) ✅
- `register.ts` + `register.html` (basic kayıt formu) ✅
- `AuthService.registerYacht()` + `completeSetup()` + `getOnboardingReferenceData()` ✅

### Mimari kararlar
- `flagCountry`, `baseCurrency`, `timezone`, `financialYearStartMonth` → immutable
- İlk user = ADMIN (backend enforce, UI'da gösterilmez)
- `POST /api/onboarding/register` (all-in-one) mevcut ama frontend iki aşamalı flow kullanıyor

---

## ✅ DONE — Frontend–Backend Gap Kapatma

Önceden eksik olan endpoint'ler backend'e eklendi:
- `POST /api/finance/entries/income` ✅
- `POST /api/finance/entries/expense` ✅
- `GET /api/finance/entries/search/text` ✅
- `GET /api/finance/entries/{id}/approvals` ✅

---

## ✅ DONE — Entry History Timeline

**Backend:**
- `EntryHistoryService.java` — approval eventleri + Envers revision eventlerini birleştirip tek timeline döndürüyor (chronological, newest first)
- `EntryRevisionService.java` — Hibernate Envers'tan field-level diff çıkarıyor; status/createdAt/entryNumber gibi alanlar exclude, kategori/who/mainCategory isimleri resolve ediliyor
- `EntryHistoryItemDto.java` — unified DTO (APPROVAL + REVISION + SYSTEM tipleri)

**Frontend:**
- `entry-history.service.ts` — `GET /api/finance/entries/{id}/history` çağırıyor
- `entry-history.model.ts` — DTO mirror + HistoryAction başına UI config (icon, color, label)

---

## ✅ DONE — TenantBaseCurrencyProvider

`TenantBaseCurrencyProvider.java` — hardcoded "EUR" yerine Organization tablosundan tenant'ın gerçek `baseCurrency`'sini okuyor. Fallback olarak EUR kullanıyor.

---

## ✅ DONE — EntryCommandService Ayrımı

`entry-command.service.ts` — write operasyonları (create, update, delete, receipt-number, exchange-rate) `EntryService`'ten ayrıldı. `EntryService` artık sadece query/resource API sorumluluğunda.

---

## ⬜ BEKLEYEN — File Import (Excel)

`FileImportController.java`'da `@PostMapping("/import")` hâlâ yorum satırı içinde.
`DataImportService` ve `ExcelParserService` mevcut ama endpoint aktif değil.

Açmak için:
1. `FileImportController.java` → yorum satırını kaldır
2. Frontend'de `data-import` component'ini bul/yaz (route: `/data-import`, permission: `ENTRY_EDIT_ALL`)
3. Test et

---

## ⬜ BEKLEYEN — Partial Approve (Frontend)

Backend'de `EntryApproval.approvePartialAmount()` mevcut.
`POST /api/finance/entries/{id}/approve` endpoint'i henüz `approvedAmount` parametresi almıyor.

Yapılacaklar:
1. `ApproveEntryRequest.java` → `approvedAmount` alanı ekle (nullable)
2. `EntryApprovalController.approve()` → partial/full ayrımı ekle
3. Frontend ActionCenter → onay modalına miktar input'u ekle

---

## 📋 PLANLANAN — Gelecek Geliştirmeler

- Email bildirimleri (onay bekleyen / reddedilen entry'ler için)
- Bütçe yönetimi (kategori bazlı aylık/yıllık limit)
- Exchange rate servisi (API kaynağı, cache stratejisi, fallback)
- Banka API entegrasyonu (ödeme mutabakatı)
- Mobil uyumlu onay akışı

---

## NOT — countDetailedEntries

Eğer ileride UI'da kayıt sayısı gösterilecekse `ACTUAL_STATUSES` filtresi ile yazılmalı.
