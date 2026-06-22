# Yapılacaklar — Maritar Canlı Çıkış Listesi

> Son güncelleme: 2026-06-21 (P0-1 ✅ P0-2 ✅ P0-3 ✅ P0-4 ✅ — CI yeşil, backup R2'de, 142 test geçti)
> Kaynak: TODO.md + LAUNCH_CHECKLIST.md + kod analizi (tüm listeler birleştirildi, tamamlananlar altta)

---

## 🔴 P0 — Deploy Engelleyiciler

Bunlar kapanmadan canlıya çıkma.

---

### [x] P0-1 — PostgreSQL otomatik yedekleme ✅ 2026-06-20

**Çözüm:** `.github/workflows/backup.yml` oluşturuldu.
- Her gece 02:00 UTC'de GitHub Actions çalışır
- `pg_dump` → gzip → Cloudflare R2 (`db-backups/` prefix)
- 30 günden eski dosyalar otomatik silinir
- Manuel tetikleme: Actions → "DB Backup — Neon → R2" → Run workflow

**✅ GitHub Secrets eklendi + R2'de `backup-20260621-090431.sql.gz` doğrulandı (2026-06-21)**

---

### [x] P0-2 — Maven wrapper'ı commit'le ✅ 2026-06-21

CI yeşil geçtiği doğrulandı.

---

### [x] P0-3 — CI pipeline'ı GitHub'a push'la ✅ 2026-06-21

CI #21 yeşil.

---

### [x] P0-4 — Smoke testlerin gerçekten geçtiğini doğrula ✅ 2026-06-21

CI #22: `SmokeIntegrationTest` T1–T8 → 8/8 geçti. Toplam 142 test, 0 failure, 0 skipped. Testcontainers gerçek DB ile çalıştı.

---

### [ ] P0-4 — Smoke testlerin gerçekten geçtiğini doğrula

**Sorun:** `SmokeIntegrationTest` (T1–T8) ve `TenantIsolationIntegrationTest` yazıldı ama hiç gerçek ortamda koşulmadı.

**Aksiyon:**
```bash
./mvnw test -q
```
8 smoke testi + tenant izolasyon testlerinin hepsi yeşil geçmeli. Kırmızı olan test deploy'u bloklar.

---

### [ ] P0-5 — Excel import frontend'ini doğrula

⏸️ **Şu an gündemde değil — pilot öncesi tekrar değerlendir.**

Backend endpoint açık (`/api/files/import`), frontend tarafı yapılmadı. Pilot kullanıcılar bu feature'ı kullanmayacaksa blocker değil; kullanacaksa pilot öncesi tamamlanmalı.

---

## 🟡 P1 — İlk Hafta İçinde

---

### [ ] P1-1 — 64 `console.log`'u prod'dan çıkar

**Sorun:** 20 dosyada 64 `console.log/warn/error` var, prod build'e gidiyor. Angular bunları otomatik strip etmiyor.

**Aksiyon (ikisinden birini seç):**
- Hızlı: `angular.json` production build'ine terser options ekle:
  ```json
  "optimization": {
    "scripts": true,
    "terserOptions": { "compress": { "drop_console": true } }
  }
  ```
- Kalıcı: `LoggerService` arkasına al (`environment.production` kontrolüyle).

---

### [ ] P1-2 — CSP header ekle (nginx)

**Sorun:** `nginx.conf`'ta gzip ve cache var ama CSP yok. Token localStorage'da olduğu için XSS'e karşı ilk savunma hattı bu.

**Aksiyon:** `nginx.conf`'a ekle:
```nginx
add_header Content-Security-Policy "
  default-src 'self';
  script-src 'self';
  style-src 'self' 'unsafe-inline';
  img-src 'self' data: https://*.r2.cloudflarestorage.com;
  connect-src 'self' https://api.maritar.com;
  frame-ancestors 'none';
" always;
```

---

### [ ] P1-3 — `/auth/logout` kararını ver ve kapat

**Sorun:** SecurityConfig `/api/auth/**` → `permitAll`. Logout bu path altında → korumasız. Kod yorumu "PROTECTED" diyor, gerçek değil.

**Seçenekler:**
- A) Korumalı yap: `logout` endpoint'ini `SecurityConfig`'te `authenticated()` listesine çıkar.
- B) Bilinçli tercih: Mevcut davranışı koru (body'deki refresh token yeterli güvence), yorumu düzelt ve belgele.

---

### [ ] P1-4 — Swagger `permitAll` iznini kaldır

**Sorun:** `SecurityConfig`'te `/swagger-ui/**` ve `/v3/api-docs/**` `permitAll` var ama `pom.xml`'de `springdoc` dependency yok. Ölü ama tehlikeli: ileride dependency eklenirse prod'da API dökümantasyonu herkese açılır.

**Aksiyon:** İzinleri `SecurityConfig`'ten sil. Swagger lazım olursa dev profile'a `@Profile("!prod")` ile bağla.

---

### [ ] P1-5 — Uptime monitoring aç

**Durum:** `/actuator/health` açık. Monitoring yok.

**Aksiyon:** UptimeRobot / Better Uptime / Pingdom ile 1–5 dakika aralıkla ping. Down olunca SMS veya email.

---

### [ ] P1-6 — Release versiyonlama + rollback planı

**Aksiyon:**
```bash
git tag v0.1.0-pilot.1
```
Docker image'i aynı tag'la işaretle. Önceki sürüme rollback tek komutla yapılabilmeli — bunu belgele. Pilot döneminde DB migration'ları forward-only; rollback gerekirse snapshot restore.

---

### [ ] P1-7 — Pilot kullanıcı feedback kanalı

Her iki yat işletmecisi için Slack Connect veya Discord kanalı aç. İlk 30 gün haftalık 30 dk video call.

---

## 🔵 Karar Bekleyenler

Deploy öncesi netleşmeli.

---

### [ ] K1 — W2: Tenant-scoped entry numaralandırma

**Sorun:** Global DB sequence kullanıyorsun. Tenant başına numara delikli (FE-2026-005 → FE-2026-012 atlayabilir) ve komşu tenant'ın işlem hacmi numaradan tahmin edilebilir.

**Seçenekler:**
- A) "Pilot için kabul" → bilinçli borç olarak belgele ve kapat.
- B) Düzelt → `(tenant_id, yıl)` bazlı sayaç tablosu + `SELECT ... FOR UPDATE`.

---

### [ ] K2 — W4: Tek cihaz oturumu pilot kullanıcılara bildir

**Durum:** `deleteByUserId()` kasıtlı — yeni cihazdan login tüm token'ları siliyor. Pilot kullanıcılar telefon + laptop'tan girmeyi denerlerse sürpriz logout yaşar.

**Aksiyon:** Pilot öncesi onlara söyle. Çoklu cihaz istiyorlarsa ayrı iş (`ipAddress`/`userAgent` alanları zaten var, kullanılmıyor).

---

## 🟢 P2 — İlk 30 Gün (Pilot Verisiyle)

Pilot'tan gelen gerçek kullanıcı sorunlarına göre sıralama değişebilir.

---

### [ ] P2-1 — `RoleHierarchy` Spring bean ekle

```java
@Bean
public RoleHierarchy roleHierarchy() {
    return RoleHierarchyImpl.fromHierarchy("""
        ROLE_SUPER_ADMIN > ROLE_CAPTAIN
        ROLE_CAPTAIN > ROLE_MANAGER
        ROLE_CAPTAIN > ROLE_CREW
        """);
}
```
Sonra `SecurityConfig`'teki uzun `hasAnyRole(...)` listelerini sadeleştir.

---

### [ ] P2-2 — Domain → application bağımlılık ihlalini düzelt

`FinancialEntry.calculateBaseAmount(ExchangeRateService, ...)` domain'den application katmanını çağırıyor. Domain'de `ExchangeRateProvider` interface tanımla, `application.ExchangeRateService` bunu implement etsin.

---

### [ ] P2-3 — Dev profilinde Flyway'i aç

`application-dev.properties`:
```properties
spring.flyway.enabled=true
spring.jpa.hibernate.ddl-auto=validate
```
"Bende çalışıyor" sendromunu önler; dev'de schema drift erken yakalanır.

---

### [ ] P2-4 — `spring.flyway.baseline-on-migrate=true` gözden geçir

Boş DB'de gereksiz, yanlış kullanımda migration atlama riskine yol açar. Geçmiş DB'ler yoksa kaldır.

---

### [ ] P2-5 — JWT refresh token reuse detection

Refresh token kullanıldığında `used=true` işaretle. Aynı token tekrar gelirse tüm session'ları invalidate et (token theft senaryosu).

---

### [ ] P2-6 — TenantAwareScheduledTask AOP güvenliği

**Sorun:** Scheduled task lambda'sı içinde doğrudan JpaRepository çağrısı AOP proxy'yi atlıyor → Hibernate tenant filter aktif olmaz → cross-tenant sızıntı riski.
**Şu an güvende:** Hiç extend eden sınıf yok — latent risk (gelecek geliştirici tuzağı).
**Değerlendir:** `executeForAllTenants` içinde `entityManager.unwrap(Session.class).enableFilter(...)` explicit çağrısı ekle.

---

### [ ] P2-7 — JWT filter'da istek başına DB lookup cache'le

Her istekte user DB'den çekiliyor. Yük altında darboğaz olabilir. Kısa TTL'li (ör. 5 dk) user cache değerlendir.

---

### [ ] P2-8 — Frontend bundle analizi

```bash
ng build --stats-json
```
exceljs + chart.js ağır. 1 MB budget'ı aşma riski var. Lazy-load kontrolü yap.

---

## 📋 MVP Sonrası (Kullanıcı Geri Bildirimine Göre)

- [ ] Partial approve — frontend (backend `approvePartialAmount()` hazır, `ApproveEntryRequest`'e `approvedAmount` ekle)
- [ ] Email bildirimleri: onay bekleyen / reddedilen entry'ler
- [ ] Bütçe yönetimi (kategori bazlı aylık/yıllık limit)
- [ ] Exchange rate API entegrasyonu (şu an cache'de manuel)
- [ ] Basit/Detaylı mod tenant ayarı
- [ ] Onay ekranında inline detaylandırma (WHO + ana kategori)
- [ ] WHO filtresini yumuşat ("alakalılar üstte + tümünü göster")
- [ ] Sefer/charter etiketi (sefer bazlı maliyet takibi)
- [ ] `EntryStatus` ikiye bölme (`approvalStatus` × `paymentStatus`) — iade/fazla ödeme gelirse
- [ ] `switchTenant()` ölü kodunu frontend'den sil

---

## 🚀 Deploy Günü Final Smoke Check (manuel, ~15 dk)

Pilot kullanıcılar login atmadan önce sırasıyla yap:

- [ ] Production domain'inden login başarılı (CORS ✓)
- [ ] Login sonrası dashboard yükleniyor
- [ ] Yeni entry oluşturuldu, kaydedildi
- [ ] Approval flow çalışıyor (CAPTAIN onayı test hesabıyla)
- [ ] Logout sonrası localStorage temiz (DevTools → Application)
- [ ] `/actuator/health` → 200
- [ ] Sentry'ye test hatası düştü ve dashboard'da görünüyor
- [ ] Backup çalışıyor (en son backup timestamp güncel)
- [ ] Tüm env var'lar set edildi (aşağıya bak)
- [ ] Pilot kullanıcı hesapları oluşturuldu, giriş bilgileri güvenli kanaldan gönderildi

**Zorunlu env var'lar:**
`JWT_SECRET`, `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`,
`R2_ACCOUNT_ID`, `R2_ACCESS_KEY`, `R2_SECRET_KEY`, `R2_BUCKET`,
`CORS_ALLOWED_ORIGINS`, `SENTRY_DSN`,
`MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM`, `MAIL_ENABLED=true`,
`APP_VERIFY_URL`, `APP_RESET_PASSWORD_URL`,
`COOKIE_SECURE=true`, `SYSTEM_ADMIN_PASSWORD`

---

## ✅ Tamamlananlar

### 2026-06-20 (kod analizi doğrulaması)
- [x] **jjwt 0.12.6** — pom.xml zaten 0.12.6, upgrade gerekmedi (TODO'da "0.11.5'ten yükselt" yazıyordu, hatalıydı)
- [x] **Frontend Sentry** — `main.ts` init, `error.interceptor.ts` capture, `environment.prod.ts` DSN: hepsi tamam. Sadece deploy'da `SENTRY_DSN` env var set edilmeli.
- [x] **`server.shutdown=graceful`** — `application.properties`'te mevcut
- [x] **`bypassSecurityTrustResourceUrl` audit** — güvenli; sadece `URL.createObjectURL(blob)` ile kullanılıyor, user input yok
- [x] **`switchTenant()` ölü kod tespiti** — hiçbir yerde çağrılmıyor (silinebilir, MVP sonrası listesine eklendi)

### 2026-06-19
- [x] Smoke test suite yazıldı (T1–T8, `SmokeIntegrationTest`)
- [x] `TenantIsolationIntegrationTest` yazıldı (5 senaryo)
- [x] SYSTEM tenant fallback — tasarım gereği, bug değil (kapatıldı)

### 2026-06-18
- [x] Refresh token SHA-256 hash — DB'ye hash yazılıyor, lookup hash üzerinden
- [x] JWT secret uzunluk doğrulaması — `@PostConstruct` ile ≥32 karakter kontrolü
- [x] Frontend Dockerfile — ng build → nginx multi-stage
- [x] `environment.prod.ts` gerçek API URL — `https://api.maritar.com/api`
- [x] `.env.example` + `.gitignore` — tüm zorunlu env var'lar belgelenmiş
- [x] Legacy `/api/onboarding/register` kapatıldı — `RateLimitFilter`'daki ölü referans temizlendi

### 2026-06-11
- [x] `@EnableMethodSecurity` eklendi — kritik: bu olmadan tüm `@PreAuthorize` sessizce devre dışıydı
- [x] Excel import aktive edildi — `DataImportService` + `/api/files/import` açıldı, güncel domain modeline uyarlandı
- [x] Başlangıç kategori seti — 22 gider + 3 gelir, TR/EN, tenant başına idempotent
- [x] DemoDataInitializer yeniden yazıldı — gerçekçi demo veri, WHO + MainCategory atandı

### 2026-06-10
- [x] CORS — `cors.allowed-origins` property'si koda bağlandı (`SecurityConfig`)
- [x] `app.r2.enabled=${R2_ENABLED:true}` prod profile'a eklendi
- [x] `@EnableScheduling` eklendi — refresh token temizliği çalışıyor
- [x] JWT filter hata sızıntısı kapatıldı — generic mesaj + errorId korelasyonu
- [x] W1 — Excel import sequence supplier pattern düzeltildi
- [x] W3 — `/auth/me` skip listesi düzeltildi, ilk istekte token alıyor
- [x] W5 — Interceptor kuyruğu sentinel pattern (asılı istek sorunu)
- [x] W7 — Income filtre sıfırlama (`clearFilters`) hard refresh sorunu
