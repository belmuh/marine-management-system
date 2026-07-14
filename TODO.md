# Yapılacaklar — Maritar Canlı Çıkış Listesi

> Son güncelleme: 2026-07-13 (VPS geçişi bölümü eklendi)
> **Tek kaynak:** Bu dosya tüm yapılacakların master listesidir. CLAUDE.md "Pending" bölümü buraya taşındı.
> Kaynak: TODO.md + LAUNCH_CHECKLIST.md + CLAUDE.md Pending + kod analizi (birleştirildi, tamamlananlar altta).

---

## 🔴 P0 — Deploy Engelleyiciler

Bunlar kapanmadan canlıya çıkma. **Hepsi tamamlandı** — detaylar "Tamamlananlar" bölümünde.

### [ ] P0-5 — Excel import frontend'ini doğrula

⏸️ **Şu an gündemde değil — pilot öncesi tekrar değerlendir.**

Backend endpoint açık (`/api/files/import`), frontend tarafı yapılmadı. Pilot kullanıcılar bu feature'ı kullanmayacaksa blocker değil; kullanacaksa pilot öncesi tamamlanmalı.

---

## 🟠 VPS Geçişi — Render/Neon → Hetzner (devam ediyor)

> Durum (2026-07-13): VPS'te uygulama ayakta ve healthy (backend + postgres + caddy,
> ağ segmentasyonlu). CI push→VPS deploy zinciri uçtan uca çalışıyor. Trafik hâlâ
> Render'da. **Karar:** Neon verisi taşınmayacak — VPS temiz DB ile sıfırdan başlayacak.
> As-built dokümantasyon: `DEPLOYMENT_GUIDE.md` Bölüm 17

### [ ] V1 — VPS yedeklemesi + restore provası (cutover ÖNCESİ şart)

Sıfırdan başlama kararıyla asıl risk değişti: cutover sonrası gerçek veri VPS postgres'inde
olacak ama **onu yedekleyen hiçbir şey yok** (backup.yml hâlâ Neon'u yedekliyor).

1. `backup.yml`'i VPS postgres'ini yedekleyecek şekilde güncelle
   (SSH ile `docker compose exec postgres pg_dump` → gzip → R2 `db-backups/`; Neon secret'ı yerine VPS erişimi)
2. İlk yedeğin R2'ye düştüğünü doğrula
3. **Restore provası:** yedeği boş bir DB'ye restore et, süreyi ölç ve buraya not et (RTO)
   - Prova için VPS'te geçici ikinci DB veya lokal Docker yeterli; canlı `pgdata`'ya dokunma
   - Restore sonrası smoke: login + entry listesi

### [ ] V2 — DNS cutover (veri taşıma YOK — sadeleşti)

Ön koşul: V1 tamam (yedekleme çalışıyor + restore kanıtlı).

1. Cloudflare'de `api.maritar.com` → önce "DNS only" (gri bulut), VPS IP'sine (91.98.37.251) çevir — TTL düşük tut
2. Caddy Let's Encrypt sertifikayı alsın (`docker compose logs -f caddy` izle)
3. Sertifika alınınca Cloudflare proxy (turuncu bulut) tekrar aç
4. Smoke check (aşağıdaki "Deploy Günü Final Smoke Check") — CORS dahil
5. Sorunsuzsa: Render servisini durdur (silme — 1 hafta rollback payı)
6. 1 hafta sorunsuz geçince: Render + Neon kapat; `NEON_DATABASE_URL` ve `RENDER_DEPLOY_HOOK_URL` secret'larını sil

**Rollback:** DNS'i Render'a geri çevir. Not: sıfırdan başlandığı için rollback,
VPS'te bu arada oluşan veriyi Render/Neon tarafında GÖRMEZ — cutover sonrası
oluşan veri kaybolmasın istiyorsan rollback penceresini kısa tut.

### [ ] V3 — Deploy'da `latest` yerine commit SHA tag'i

`ci.yml` deploy adımı `latest` çekiyor; rollback belirsiz. GHCR'a zaten SHA tag'i push'lanıyor —
compose'a `IMAGE_TAG` env geçirip deploy'da SHA kullan. P1-6 (release versiyonlama) ile birleşebilir.

---

## 🟡 P1 — İlk Hafta İçinde

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

### [ ] K2 — W4: Tek cihaz oturumu pilot kullanıcılara bildir

**Durum:** `deleteByUserId()` kasıtlı — yeni cihazdan login tüm token'ları siliyor. Pilot kullanıcılar telefon + laptop'tan girmeyi denerlerse sürpriz logout yaşar.

**Aksiyon:** Pilot öncesi onlara söyle. Çoklu cihaz istiyorlarsa ayrı iş (`ipAddress`/`userAgent` alanları zaten var, kullanılmıyor).

---

## 🟢 P2 — İlk 30 Gün (Pilot Verisiyle)

Pilot'tan gelen gerçek kullanıcı sorunlarına göre sıralama değişebilir.

### [ ] P2-1 — `RoleHierarchy` Spring bean ekle

> Kod kontrolü (2026-06-28): henüz eklenmemiş, açık.

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

### [ ] P2-9 — Restore (soft delete geri alma) endpoint'i

> Eklendi: 2026-07-08. Gerekçe: müşteri "yanlışlıkla sildik" dediğinde uygulama içinden anında geri alma. Yedekten dönmek multi-tenant'ta tek müşteri hatası için kullanılamaz (tüm tenant'lar geri gider).

**Durum:** Altyapı hazır ama kullanılmıyor — `BaseAuditedEntity.restore()` ve `BaseTenantEntity.restore(User)` mevcut, **hiçbir servis/endpoint çağırmıyor**. Bugün geri alma ancak elle DB'de `is_deleted = false` ile mümkün.

**Aksiyon:** `POST /api/finance/entries/{id}/restore` (öncelik: FinancialEntry; kategori vb. sonra).
- Silinmiş kayda erişim için `@Where(is_deleted = false)` filtresini aşan native/`@Query` sorgu gerekir
- **Tenant kontrolü şart** — silinmişe erişen sorgu tenant filter'ı da atlayabilir, `tenant_id` explicit doğrulanmalı
- Yetki: CAPTAIN/SUPER_ADMIN (`@PreAuthorize`)
- Envers zaten restore'u da loglar (`updated_by` set ediliyor)
- Frontend: entry listesinde "silinenleri göster" + geri al butonu (ayrı iş olabilir)

---

### [ ] P2-10 — Spring Boot 4.1 migration

> Eklendi: 2026-07-14. Sürüm denetimi bulgusu: 3.5 hattı 2026-06-30'da OSS EOL oldu.
> Son patch olan 3.5.16'ya çekildi (güvenlik açığı kapandı) ama bundan sonra 3.5'e CVE yaması **gelmeyecek**.

**Aksiyon:** Spring Boot 4.1'e (Spring Framework 7) migration planla — major iş, cutover/pilot sonrası. Testcontainers pin'i (`1.21.4`) 4.1 BOM'unda gereksizleşiyor olabilir, birlikte gözden geçir.

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
- [ ] Angular 20 → 21/22 bump (v20 aktif desteği Kasım 2026'ya kadar; acil değil)

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

### 2026-07-13 (VPS geçişi — altyapı ayağı)
- [x] **Backend healthcheck bug'ı** → compose `wget` kullanıyordu ama `temurin-jre` imajında wget yok; healthcheck hiç geçmiyordu. `curl`'e çevrildi, curl Dockerfile'a eklendi, `start_period: 90s` korundu (commit `3b82f36`)
- [x] **Ağ segmentasyonu** → `frontend_network` (caddy↔backend) + `backend_network` (backend↔postgres); Caddy artık DB'ye erişemiyor. Sunucuda doğrulandı: dışa açık portlar sadece 80/443
- [x] **CI deploy hedefi Render→VPS** → `12d1101`: scp ile compose+Caddyfile senkronu + `docker compose pull & up` + Caddy graceful reload. Secrets: `VPS_HOST`, `VPS_SSH_KEY` (CI'a özel `maritar_ci` anahtarı)
- [x] **Prod `.env` VPS'e kuruldu** → dev'den ayrı taze secret'lar (DB şifresi, JWT, admin şifresi), `SENTRY_DSN` düzeltmesi (dev'deki `sentry.dsn` formatı prod'da `SENTRY_DSN` bekleniyordu — açılış crash'inin sebebiydi)
- [x] **VPS'te uygulama healthy** → backend 13.9s'de açıldı, Flyway + seed + SYSTEM bootstrap tamam. CI #36 uçtan uca yeşil

### 2026-06-28 (kod analizi doğrulaması — açık sanılıp aslında bitmiş olanlar)
- [x] **P1-1 — `console.log` temizliği** → `LoggerService` pattern uygulandı. Kodda kalan 4 `console.*` meşru (LoggerService'in kendi içi + `main.ts` bootstrap catch). İsteğe bağlı: `angular.json`'a `drop_console` terser ayarı eklenebilir (zorunlu değil).
- [x] **P1-2 — CSP header (nginx)** → `nginx.conf`'ta CSP + X-Frame-Options (DENY) + X-Content-Type-Options (nosniff) + Referrer-Policy + Permissions-Policy mevcut.
- [x] **P1-3 — `/auth/logout` kararı** → A seçeneği uygulandı. `SecurityConfig`: `.requestMatchers("/api/auth/logout").authenticated()` satırı `/api/auth/**` permitAll'dan önce tanımlı.
- [x] **P1-4 — Swagger `permitAll` kaldırıldı** → SecurityConfig'te artık `/swagger-ui/**` veya `/v3/api-docs/**` matcher'ı yok. `springdoc` dependency de pom.xml'de yok (tutarlı).
- [x] **K1 — Tenant-scoped entry numaralandırma** → B seçeneği uygulandı. `V002__tenant_entry_counter.sql` migration + `TenantEntryCounterRepository` + `FE-YYYY-NNNN` formatı mevcut.
- [x] **Demo data review** (CLAUDE.md Pending'den taşındı) → `DemoDataService`: org "S/Y Maritar", uluslararası ekip (Luca Romano / Claire Dubois / Marco Rossi / Sophie Martin), `enableManagerApproval(500.00 EUR)`. Reset endpoint: `POST /api/admin/demo/reset`.
- [x] **Sentry entegrasyonu** (CLAUDE.md Pending'den taşındı) → Backend: `sentry-spring-boot-starter-jakarta` + `sentry.dsn=${SENTRY_DSN}` (prod). Frontend: `@sentry/angular` + `main.ts` init, `error.interceptor`, `logger.service`, `app.config`.
- [x] **Test altyapısı** (CLAUDE.md Pending'den taşındı) → `MoneyTest`, `TenantIsolationIntegrationTest`, `SmokeIntegrationTest` (T1–T8 auth flow). Bkz. P0-4.
- [x] **superadmin SUPER_ADMIN rolü** → role yükseltildi, `AccessDeniedException` 403 döndürüyor (git: 5d97720).

### 2026-06-21 (P0 deploy engelleyiciler)
- [x] **P0-1 — PostgreSQL otomatik yedekleme** → `.github/workflows/backup.yml`. Her gece 02:00 UTC GitHub Actions → `pg_dump` → gzip → Cloudflare R2 (`db-backups/`). 30 günden eski dosyalar otomatik silinir. R2'de `backup-20260621-090431.sql.gz` doğrulandı.
- [x] **P0-2 — Maven wrapper commit'lendi** → CI yeşil.
- [x] **P0-3 — CI pipeline GitHub'a push'landı** → CI #21 yeşil.
- [x] **P0-4 — Smoke testler doğrulandı** → CI #22: `SmokeIntegrationTest` T1–T8 → 8/8. Toplam 142 test, 0 failure, 0 skipped. Testcontainers gerçek DB ile koştu.

### 2026-06-20 (kod analizi doğrulaması)
- [x] **jjwt 0.12.6** — pom.xml zaten 0.12.6, upgrade gerekmedi (TODO'da "0.11.5'ten yükselt" yazıyordu, hatalıydı)
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
