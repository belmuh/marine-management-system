# My Notes


## ✅ DONE — ReportingCategory + Status Filtresi Refactoru

Tamamlanan işler:
- `ReportingCategory.java` enum oluşturuldu (ACTUAL, COMMITTED, NON_FINANCIAL)
- `EntryStatus.java` → `getReportingCategory()` + `ACTUAL_STATUSES` + `COMMITTED_STATUSES` eklendi
- `FinancialEntrySpecs.java` → `approved()` → `actualEntries()`, `pendingApproval()` → `committedEntries()` rename
- `FinancialEntrySpecs.java` → `forFinancialReports(User)` eklendi (rol bazlı rapor görünürlüğü)
- `FinancialEntryReportRepository.java` → 13 sorguya `AND e.status IN :statuses` eklendi
- `FinancialReportService.java` → `getActualTotal()`, `getCommittedTotal()`, `getCarryOverBalance()` eklendi
- `countDetailedEntries` dead code silindi
- `findCarryOverBalance` yorum satırı temizlendi, parametre ile çalışıyor

---

## TODO — countDetailedEntries dead code (silinmişti ama not kalacak)

Eğer ileride UI'da kayıt sayısı gösterilecekse:
- Status filtresi (`ACTUAL_STATUSES`) ile yazılmalı
- Finansal toplamlarla tutarlı olmalı

---

## PLAN — Yacht Registration Wizard (v2 — güncellenmiş)

### Mevcut Durum
- `RegistrationService.registerNewOrganization()` tek API call
- `baseCurrency` → "EUR" hardcode
- `flagCountry` → "TR" varsayılan
- Yat bilgileri (tip, uzunluk, marina) hiç toplanmıyor
- `timezone` ve `financialYearStartMonth` alanları Organization'da yok
- İlk kullanıcının rolü açıkça tanımlı değil
- ✅ `RegisterYachtCommand` zaten var (tüm temel alanlar mevcut)
- ✅ `OrganizationOnboardingService` shell olarak var (sadece org oluşturma, user/ref data eksik)
- ✅ `TenantReferenceDataInitializer` mevcut (MainCategory + Who kopyalama, henüz onboarding'e bağlı değil)
- ⚠️ `yachtType` şu an String — enum'a çevrilmeli
- ⚠️ `YachtType` enum'u henüz yok
- ⚠️ Draft/onboarding tablosu yok

---

### Backend Değişiklikleri Gerekli

**Organization entity'ye yeni alanlar:**
- `timezone` (String, e.g. "Europe/Istanbul") — immutable
- `financialYearStartMonth` (Integer, 1-12) — immutable
- `baseCurrency` → hardcode kaldırılacak, wizard'dan alınacak — immutable

**Yacht entity güncellemesi:**
- `yachtType` → enum olmalı (`YachtType.java`: MOTOR_YACHT, SAILING_YACHT, CATAMARAN, GULET, OTHER)
- `yachtLength` → min/max validation (5.0m - 200.0m)
- `yachtName` → tenant bazlı unique (global değil)

**RegistrationService güncellenmeli:**
- Mevcut tek-adım kayıt → multi-step wizard'ı destekleyecek
- Yeni alanları alacak (timezone, financialYearStartMonth, yachtType, yachtLength, homeMarina)
- İlk kullanıcıya ADMIN rolü backend'de enforce edilecek (sistem yöneticisi, frontend'den gelmeyecek)
- CAPTAIN rolü sonradan admin tarafından atanabilir (fiili kaptan için)

**RegisterOrganizationRequest güncellenmeli:**
- Yeni alanlar eklenmeli (wizard'ın tüm step verileri)

**Wizard state yönetimi:**
- Frontend'de tutulur, son adımda tek API call ile gönderilir
- Draft model yok (MVP — proje henüz canlı değil, gerekirse sonra eklenir)

---

### Wizard Adımları (Frontend)

**Step 1 — Yat Bilgileri**
- yachtName (zorunlu, tenant bazlı unique)
- yachtType (zorunlu, enum: Motor Yacht / Sailing Yacht / Catamaran / Gulet / Other)
- yachtLength (zorunlu, metre, min: 5.0, max: 200.0)
- flagCountry (zorunlu, ⚠️ sonra değiştirilemez)
- homeMarina (opsiyonel)

**Step 2 — Şirket & Admin**
- companyName (opsiyonel — bazı yatlar şirket altında olmayabilir)
- Admin: firstName, lastName, email, password (zorunlu)
- phoneNumber (opsiyonel)
- ⚠️ İlk user otomatik ADMIN rolü alır (backend enforce, UI'da gösterilmez)
- Kaptan kaydı sonradan admin tarafından açılabilir

**Step 3 — Finansal Ayarlar**
- baseCurrency (zorunlu, ⚠️ sonra değiştirilemez)
- timezone (zorunlu, ⚠️ sonra değiştirilemez)
- financialYearStartMonth (zorunlu, 1-12, ⚠️ sonra değiştirilemez)
- approvalLimit (opsiyonel, varsayılan: 0 = onay gerekmez)
- managerApprovalEnabled (opsiyonel, varsayılan: false)

**Step 4 — Kategori & WHO Seçimi** (opsiyonel, "Şimdilik geç, sonra ayarlarım" butonu ile skip edilebilir)
- MainCategory listesinden hangilerini kullanacak
- WHO listesinden hangilerini kullanacak
- Varsayılan hepsi açık gelir, istenmeyen kapatılır
- ⚠️ MVP yaklaşımı: sadece disable edilenleri sakla (basit)
- ⚠️ Scale yaklaşımı: tenant-specific config tablosu (ileride geçilecek)
- Yeni category eklenirse → mevcut tenantlara varsayılan açık gelir

**Step 5 — Özet & Onay**
- Tüm bilgilerin özeti (step bazlı gruplandırılmış)
- Immutable alanlar için sarı uyarı banner'ı: "Bu alanlar kayıttan sonra değiştirilemez"
- Her step yanında "Düzenle" linki → ilgili step'e geri dönüş
- "Kaydı Tamamla" butonu

---

### Mimari Kararlar
- `flagCountry`, `baseCurrency`, `timezone`, `financialYearStartMonth` → immutable (değişiklik = yeni tenant)
- Yat satılırsa veya bayrak değişirse → yeni tenant oluşturulur
- Wizard state frontend'de tutulur, son adımda tek API call (draft model yok, MVP)
- İlk user = ADMIN (sistem yöneticisi, backend enforce, UI'da rol seçimi yok)
- CAPTAIN = fiili kaptan, admin tarafından sonradan atanır
- Kategori yönetimi: MVP'de disable-only, scale'de config tablosu

### API Endpoint
- `POST /api/onboarding/register` → tek call, tüm wizard verisi bir seferde gelir

### Implementasyon Sırası
1. `YachtType` enum oluştur
2. `V001__initial_schema.sql`'e timezone + financialYearStartMonth kolonları ekle
3. `Organization.java` güncelle (yeni alanlar + yachtType enum)
4. `RegisterYachtCommand.java` güncelle (yeni alanlar + validation)
5. `OrganizationOnboardingService.java` tamamla (user creation + ref data init + ADMIN enforce)
6. `RegistrationService.java` → @Deprecated
7. Frontend wizard component (5 step)
