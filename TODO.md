# Yapılacaklar — Açık İşler
Kaynak: DEPLOY-REVIEW.md bulguları + sohbet kararları. Tamamlananlar en altta.
Detaylar için DEPLOY-REVIEW.md, test adımları için TEST-CHECKLIST.md.

## Özellik

- [ ] **Excel import — doğrulama** (kod aktive edildi 2026-06-11, test bekliyor)
  - Lokalde `mvn compile` + mevcut testleri koş.
  - TEST-CHECKLIST 4.x maddelerini koş; gerçek Excel dosyasıyla uçtan uca dene.
  - Frontend'te import için upload ekranı var mı kontrol et — yoksa ekle.
  - Not: Frontend'te exceljs ile client-side export var; backend'de POI bağımlılığı duruyor ama kullanan aktif kod yok — export backend'e taşınacaksa ayrı iş.

## Deploy Altyapısı

- [ ] **Maven wrapper — commit + sürüm:** ✅ `mvn wrapper:wrapper` ile üretildi (2026-06-11, Maven 3.8.6'ya pinli). Kalan: `git add .mvn mvnw mvnw.cmd` ile commit'le. İsteğe bağlı: `mvn wrapper:wrapper -Dmaven=3.9.9` ile daha güncel sürüme pinle.
- [ ] **Dockerfile'lar:** ✅ Backend yazıldı (multi-stage, wrapper'lı, non-root, 2026-06-11). Kalan: frontend (ng build → nginx) + lokalde `docker build .` ile doğrula.
- [ ] **CI/CD:** ✅ `.github/workflows/ci.yml` yazıldı (build+test her push/PR'da, docker build main'de). Kalan: push'la ve ilk koşuyu izle; registry push (GHCR) deploy hedefi netleşince.
- [ ] **`.env.example`:** Zorunlu env değişkenleri: `DATABASE_URL/USERNAME/PASSWORD`, `JWT_SECRET`, `SYSTEM_ADMIN_PASSWORD`, `CORS_ALLOWED_ORIGINS`, `R2_*`, `MAIL_*` (+`MAIL_ENABLED=true`), `APP_VERIFY_URL`, `APP_RESET_PASSWORD_URL`.
- [ ] Backend root'a `.gitignore` ve `README` ekle.

## Güvenlik (bilinçli karar gerektirir)

- [ ] **Legacy `/api/onboarding/register` endpoint'ini kapat/kaldır:** Frontend artık `/auth/register` + `/setup` (v2) kullanıyor; legacy endpoint hâlâ permitAll ve referans verisini ikinci kez başlatabilir. Kaldır ya da devre dışı bırak. (Starter set seed'i tenant başına idempotent yapıldı, bu riske dayanıklı — ama TenantMainCategory/Who init'i mükerrer kayıt üretebilir.)

- [ ] **Refresh token'ları hash'le:** DB'de düz metin duruyor; SHA-256 hash sakla, lookup'ı hash ile yap. Süreyi `refresh.token.expiration` property'sine bağla (şu an `plusDays(7)` hardcoded).
- [ ] **JWT secret uzunluk doğrulaması:** Startup'ta ≥256-bit kontrolü (`@PostConstruct`).
- [ ] **jjwt 0.11.5 → 0.12.x** yükseltme.
- [ ] **Swagger `permitAll` izinlerini kaldır** (springdoc projede yok, izin ölü).
- [ ] Token'ların localStorage'da tutulması: XSS trade-off'unu belgele ya da refresh token'ı httpOnly cookie'ye taşı.

## Workflow / Tasarım Kararları

- [ ] **W2 — Tenant-scoped entry numaralandırma:** Global sequence yerine tenant başına sayaç (delikli numara + hacim sızıntısı sorunu). Tasarım kararı gerekli: `(tenant_id, yıl)` bazlı sayaç tablosu + `SELECT ... FOR UPDATE`.
- [ ] **W4 — Tek cihaz oturumu:** Login tüm refresh token'ları siliyor; çoklu cihaz isteniyorsa cihaz başına token sakla (`ipAddress`/`userAgent` alanları zaten var, kullanılmıyor).
- [ ] **`switchTenant()` (frontend) ölü kodunu kaldır** ya da yeniden-login akışına bağla — JWT değişmeden tenant değişimi yanıltıcı.
- [ ] `/auth/logout` gerçekten korumalı olsun mu karar ver (şu an permitAll, yorum "PROTECTED" diyor).

## 📦 Talebe Bağlı (MVP sonrası — kullanıcı geri bildirimi gelirse)

- [ ] **Basit/Detaylı mod (tenant ayarı):** Basit modda WHO + ana kategori alanları formda hiç görünmez; detaylı modda onay anında zorunlu olur. (Karar: 2026-06-11 — MVP esnek/opsiyonel modelle çıkıyor, "girilmeyen veri = yarım rapor" felsefesi bilinçli tercih.)
- [ ] **Onay ekranında inline detaylandırma:** Kaptan onaylarken WHO/ana kategoriyi kartın üstünde seçebilsin (crew-girer-kaptan-detaylandırır akışının pratikte yaşaması için).
- [ ] **Kategoriye `suggestedMainCategoryId`:** Who'daki desenle aynı — kategori seçilince ana kategori otomatik önerilir, rapor tutarlılığını artırır.
- [ ] **WHO filtresini yumuşat:** Teknik kategori seçilince teknik olmayan WHO'lar gizlenmesin; "alakalılar üstte + tümünü göster".
- [ ] **Sefer/charter etiketi:** Piyasa yazılımında kullanıcılar sefer başına kategori açıyor (anti-pattern) — gerçek ihtiyaç sefer bazlı maliyet takibi; etiket/sefer boyutu olarak değerlendir.
- [ ] **EntryStatus'u ikiye ayırma (approvalStatus × paymentStatus):** İade/fazla ödeme senaryoları gelirse; şimdilik bilinçli teknik borç.

## Kalite / Performans

- [ ] **`User.getAuthorities()` içindeki debug `System.out.println`'leri kaldır** — her istekte kullanıcının tüm izinlerini stdout'a basıyor (bilgi sızıntısı + gürültü).
- [ ] **Tenant izolasyon integration testi** (multi-tenant SaaS için en kritik eksik test).
- [ ] Auth akışı testleri (login/refresh/verify/reset).
- [ ] Frontend'teki 64 `console.log`'u LoggerService arkasına al ya da prod build'de strip et.
- [ ] `server.shutdown=graceful` + `server.compression.enabled=true` ekle.
- [ ] JWT filter'daki istek başına DB lookup için kısa TTL'li cache değerlendir.
- [ ] Frontend bundle analizi (`ng build --stats-json`) — exceljs/chart.js lazy-load kontrolü.
- [ ] `spring.flyway.baseline-on-migrate=true` gerçekten gerekli mi gözden geçir.

---

## ✔ Tamamlananlar (2026-06-11)

- [x] **K1 — Başlangıç kategori seti (CATEGORY_SEED.md)** — `TenantReferenceDataInitializer`'a starter set eklendi: 22 gider (tenant'ın sihirbazda seçtiği ana kategorilere göre filtrelenir) + 3 gelir (her zaman). İsimler bayrak TR ise Türkçe, değilse İngilizce. Tenant başına idempotent (legacy `/onboarding/register` + `/setup` çifte çağrısına dayanıklı). `MainCategoryDataLoader` idempotent yapıldı + **İletişim/Communication** (8.) eklendi — mevcut DB'lere migration'sız girer. `FinancialCategoryRepository`'ye tenant-explicit sorgular eklendi (`countByTenantId`, `findByTenantIdOrderByDisplayOrderAsc`) — onboarding/CLI bağlamında Hibernate tenant filtresi aktif değil, `findByName` oralarda tenant'lar arası sızdırabilirdi. `DemoDataInitializer` artık "X - Regular" uydurma kategoriler yerine starter seti (TR) kullanıyor.

- [x] **DemoDataInitializer yeniden yazıldı** — eski dağılım (%30 DRAFT, %25 PENDING, ödeme kaydı hiç yok) onay/ödeme listelerini şişiriyordu. Yeni kurgu: geçmiş 9 ay temiz PAID kayıtlar (raporlar dolu görünür) + güncel açık işler az sayıda (2 taslak, 3 onay bekleyen, 2 ödeme bekleyen, 1 kısmi ödenmiş, 1 reddedilmiş, 1 bekleyen gelir). Ek düzeltmeler: demo kayıtlara WHO + MainCategory atanıyor (drill-down raporlar artık demo'da çalışır), entry numaraları DB sequence'tan (W1 çakışması demo'da da vardı), `String.format` Locale.US ile (TR locale'de virgül → Money parse hatası), tarihler bugüne göreli (demo eskimez), kullanılmayan ApprovalService bağımlılığı kaldırıldı. NOT: Yeni veri için mevcut DEMO-YACHT org'u DB'den silinmeli (guard varken yeniden üretmez).

- [x] **Excel import aktive edildi** — `DataImportService` + `/api/files/import` açıldı ve güncel domain modeline uyarlandı (`FinancialEntry.create` yeni imza, `TenantBaseCurrencyProvider`). Kararlar: import edilen kayıtlar **PAID** girer (`submitAndApprove()` + entryDate tarihli tam `Payment` kaydı — geçmiş işlemler "ödeme bekliyor" görünmesin diye), yetki **`CATEGORY_MANAGE`** (Captain+), para birimi şimdilik **EUR**. W1 sequence supplier düzeltmesi korundu. `System.out` → SLF4J. `categoriesCreated` sayacı düzeltildi (eski kodda hep 0 dönüyordu).
- [x] **`@EnableMethodSecurity` eklendi (SecurityConfig)** — kritik bulgu: bu anotasyon eksik olduğu için projedeki TÜM `@PreAuthorize` kontrolleri (EntryApprovalController dahil) sessizce devre dışıydı; sadece URL bazlı kurallar çalışıyordu. Artık aktif — regresyon riski: endpoint yetki testleri koşulmalı (CREW ile approve denenmeli vs).

## ✔ Tamamlananlar (2026-06-10)

- [x] CORS — `cors.allowed-origins` property'si koda bağlandı (SecurityConfig)
- [x] `app.r2.enabled=${R2_ENABLED:true}` prod profile eklendi
- [x] `@EnableScheduling` eklendi (refresh token temizliği artık çalışıyor)
- [x] JWT filter hata sızıntısı — generic mesaj + errorId korelasyonu
- [x] W1 — Import sequence supplier pattern (yorumlu blokta, aktivasyona hazır)
- [x] Income filtre sıfırlama (`clearFilters`) — hard refresh sorunu
- [x] W5 — Interceptor kuyruğu sentinel pattern (asılı istek sorunu)
- [x] W3 — `/auth/me` artık ilk istekte token alıyor
