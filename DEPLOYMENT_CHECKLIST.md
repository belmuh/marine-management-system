# Deployment Checklist — Marine Management System

**Proje:** Marine Management System (Backend + Angular Frontend)
**Son Güncelleme:** Nisan 2026

---

## 1. KRİTİK GÜVENLİK AÇIKLARI — Canlıya Çıkmadan Önce Düzeltilmeli

Kod incelemesinde tespit edilen, production'da doğrudan istismar edilebilecek sorunlar.

### 1.1 Kaynak Kodda Açık Şifreler (CRITICAL)

- [x] **`application.properties` satır ~103** — JWT secret varsayılan değeri kaldırıldı. `jwt.secret=${JWT_SECRET}` — sadece env var ile çalışıyor. ⚠️ `.env`'deki dev secret (`marineManagementSuperSecretKey2025ForCaptainTahir`) production'da kullanılmamalı; `openssl rand -hex 32` ile yeni üret.
- [x] **`application.properties` satır ~93** — Sistem admin şifresi varsayılan değeri kaldırıldı. `system.admin.password=${SYSTEM_ADMIN_PASSWORD}` — sadece env var. ⚠️ `.env`'deki `SuperAdmin123!` production'da kullanılmamalı; güçlü bir şifre set et.
- [x] **`application.properties` satır ~14** — Veritabanı şifresi varsayılan değeri kaldırıldı. `spring.datasource.password=${DATABASE_PASSWORD}` — sadece env var. ⚠️ `.env`'deki `maritime2025!` production'da kullanılmamalı.
- [x] **`application-dev.properties` satır ~61** — Gmail SMTP uygulama şifresi (`rmrxlqorghzvnrmp`) kaynak kodda açık. `.env` dosyasına taşınmalı, git geçmişinden de temizlenmeli (kaynak kodda zaten env var, git geçmişi BFG ile temizleniyor)

### 1.2 Backend-Frontend Endpoint Uyumsuzluğu (CRITICAL)

- [x] **`GET /finance/entries/{id}/approvals`** — Eklendi. `ApprovalService.getApprovalHistory()` + `EntryApprovalController` güncellendi.
- [x] **`GET /finance/entries/search/text`** — Eklendi. `FinancialEntryController`'a `/search/text` endpoint'i eklendi.
- [x] **`POST /finance/entries/income`** ve **`POST /finance/entries/expense`** — Eklendi. `entryType` endpoint path'ten otomatik set ediliyor.

### 1.3 Frontend Production URL (CRITICAL)

- [x] **`environment.prod.ts`** — `REPLACE_WITH_YOUR_API_DOMAIN` placeholder'ı gerçek production domain ile değiştirilmeli. `angular.json`'a `fileReplacements` eklendi (production build artık bu dosyayı kullanıyor).

---

## 2. GÜVENLİK İYİLEŞTİRMELERİ — Yüksek Öncelik

### 2.1 Rate Limiting (HIGH)

- [x] **Login endpoint'ine rate limiting eklendi** — `RateLimitFilter.java` (Caffeine tabanlı, yeni dependency yok). Login / forgot-password / reset-password: IP başına dakikada 5 istek. Register / resend-verification: IP başına saatte 3 istek. `@Order(HIGHEST_PRECEDENCE)` ile Spring Security'den önce servlet katmanında çalışır.
- [ ] **Genel API rate limiting** — Kimlik doğrulanmış endpoint'ler için DoS koruması henüz yok. İleride `bucket4j` ile eklenebilir.

### 2.2 JWT Token Güvenliği (HIGH)

- [ ] **Token blacklist/revocation mekanizması ekle** — Şu an JWT iptal edilemiyor; çalınan token süre bitene kadar geçerli. Kısa vadede: token expiration'ı 1 saatten 15 dakikaya düşür. Orta vadede: Caffeine cache ile token blacklist implement et.
- [ ] **Refresh token rotation** — Şu an aynı refresh token 7 gün boyunca değişmiyor. Her refresh işleminde yeni token üretilmeli, eski token iptal edilmeli.

### 2.3 Güvenlik Başlıkları (HTTP Headers) (HIGH)

Backend (`SecurityConfig.java`) şu an sadece `X-Frame-Options: DENY` ayarlıyor. Eksik başlıklar:

- [ ] **`X-Content-Type-Options: nosniff`** — MIME sniffing koruması
- [ ] **`Strict-Transport-Security` (HSTS)** — `max-age=31536000; includeSubDomains`
- [ ] **`Content-Security-Policy`** — `default-src 'self'` temelinde yapılandır
- [ ] **`Referrer-Policy`** — `strict-origin-when-cross-origin`

Frontend (`index.html`) için de CSP meta tag veya Nginx header'ı eklenmelı.

### 2.4 SystemAdminInitializer Profil Kısıtlaması (HIGH)

- [x] **`SystemAdminInitializer.java`** — `@Profile("!test")` eklendi (test profilinde çalışmaz). Boş `SYSTEM_ADMIN_PASSWORD` veya `SYSTEM_ADMIN_EMAIL` için fail-fast validation eklendi.

### 2.5 Şifre Politikası (MEDIUM)

- [ ] **Minimum şifre uzunluğunu 12 karaktere çıkar** — Şu an `PasswordResetService.java` ve `RegisterRequest.java`'da minimum 8 karakter. Modern standartlarda yetersiz.
- [ ] **Karmaşıklık kuralı ekle** — Büyük/küçük harf, rakam ve özel karakter zorunluluğu.

### 2.6 Eksik DTO Validasyonları (MEDIUM)

- [ ] **`CreateEntryRequest.java`** — Hiç validation annotation yok (`@NotNull`, `@Size`, `@DecimalMin` vb.). Amount, description, date alanları doğrulanmadan backend'e geçiyor.
- [ ] **`EntrySearchRequest.java`** — Arama parametreleri doğrulanmıyor.

---

## 3. FRONTEND GÜVENLİK — Yüksek Öncelik

### 3.1 Console Log Temizliği (HIGH)

- [ ] **24 dosyada ~70+ `console.log` ifadesi var** — Bunlar production'da bilgi sızıntısına yol açar. En kritiği: `auth.interceptor.ts` (7 adet, token refresh mekanizmasını açığa çıkarıyor), `dashboard.ts` (20+ adet), `expense-form.ts` (10+ adet), `entry.service.ts` (debug: `console.log("this.entries.value()")`), `income-list.ts` (debug: `console.log("tt")`).
- [ ] Tamamını kaldır veya `if (!environment.production)` kontrolüne al.

### 3.2 Global Error Handler (HIGH)

- [ ] **Angular'da global ErrorHandler yok** — `app.config.ts`'de `provideBrowserGlobalErrorListeners()` var ama custom ErrorHandler servisi tanımlı değil. Yakalanmayan hatalar sessizce kayboluyor. `GlobalErrorHandler` servisi yazılıp provider olarak kaydedilmeli.

### 3.3 Token Depolama (MEDIUM)

- [ ] JWT token'lar `localStorage` / `sessionStorage`'da saklanıyor (`storage.service.ts`). XSS açığı olursa token çalınabilir. Kısa vadede kabul edilebilir (Angular'ın DomSanitizer koruması var), uzun vadede `HttpOnly + Secure` cookie'ye geçiş değerlendirilmeli.

---

## 4. Altyapı & Sunucu Hazırlığı

- [ ] Production sunucusu / hosting platformu belirlendi ve hazırlandı
- [ ] Domain adı alındı ve DNS ayarları yapıldı
- [ ] SSL/TLS sertifikası yapılandırıldı (HTTPS zorunlu)
- [ ] Backend için reverse proxy yapılandırıldı (Nginx / Caddy)
- [x] PostgreSQL production instance kuruldu (Neon) — bağlantı henüz test edilmedi
- [ ] Cloudflare R2 bucket oluşturuldu ve CORS ayarları yapıldı
- [ ] Sunucu güvenlik duvarı kuralları yapılandırıldı (yalnızca 80, 443, SSH)
- [ ] Nginx'de Angular SPA routing: `try_files $uri /index.html` yapılandırıldı
- [ ] HTTP → HTTPS redirect aktif

---

## 5. Backend — Ortam Değişkenleri

`application-prod.properties`'de tanımlı, **tamamı** set edilmeli:

- [ ] `DATABASE_URL` — `jdbc:postgresql://host:5432/marine_db`
- [ ] `DATABASE_USERNAME`
- [ ] `DATABASE_PASSWORD` — güçlü, rastgele üretilmeli
- [ ] `JWT_SECRET` — `openssl rand -hex 32` ile en az 256-bit
- [ ] `JWT_EXPIRATION` — varsayılan 3600000ms (1 saat), 900000ms (15 dk) önerilir
- [ ] `SYSTEM_ADMIN_PASSWORD` — güçlü, kaydet, ilk girişte değiştir
- [ ] `CORS_ALLOWED_ORIGINS` — frontend production URL'i (wildcard yok)
- [ ] `R2_ACCOUNT_ID`, `R2_ACCESS_KEY`, `R2_SECRET_KEY`
- [ ] `R2_BUCKET` — varsayılan: `marine-attachments`
- [ ] `R2_PRESIGNED_URL_EXPIRY` — varsayılan: 60 dk
- [ ] `DB_POOL_SIZE` — varsayılan: 20
- [ ] `DB_POOL_MIN_IDLE` — varsayılan: 10

---

## 6. Backend — Build & Deploy

- [ ] `mvn clean package -Pprod` ile production JAR oluşturuldu
- [ ] `spring.profiles.active=prod` ile başlatılıyor
- [ ] Flyway migration'ları başarıyla çalıştı (V001–V009)
- [ ] `spring.jpa.hibernate.ddl-auto=validate` — hata yok
- [ ] `GET /actuator/health` → `{"status":"UP"}`
- [ ] Hibernate tenant filter'ları aktif — cross-tenant veri sızıntısı yok
- [ ] SUPER_ADMIN hesabı oluşturuldu ve ilk giriş test edildi
- [ ] JWT token üretimi ve doğrulaması test edildi
- [ ] Loglar doğru seviyede: `INFO` uygulama, `WARN` root
- [ ] Log rotasyonu ve saklama yapılandırıldı
- [ ] `DemoDataInitializer` çalışmıyor (`@Profile("dev")` — doğrulandı)

---

## 7. Frontend — Build & Deploy

- [x] `environment.prod.ts` → `apiUrl` gerçek production adresiyle güncellendi (`api.maritar.com`)
- [ ] `ng build --configuration production` başarılı (hata yok)
- [ ] Bundle boyutu kontrol edildi (budget: initial < 500kB, max < 1MB)
- [ ] Source map'ler production build'de dahil DEĞİL
- [ ] `outputHashing: "all"` aktif (cache busting)
- [ ] `dist/` production sunucusuna yüklendi
- [ ] i18n dosyaları (`/assets/i18n/tr.json`, `en.json`) build'e dahil
- [ ] `npm audit` çalıştırıldı, kritik zafiyet yok

---

## 8. Veritabanı

- [ ] Düzenli yedekleme (backup) planı oluşturuldu
- [ ] İlk yedek alındı ve restore test edildi
- [ ] Ayrı uygulama kullanıcısı oluşturuldu (root/superuser kullanılmıyor)
- [ ] `pg_hba.conf` — sadece uygulama sunucusundan erişim
- [ ] Flyway migration geçmişi `flyway_schema_history` tablosunda doğrulandı
- [ ] Envers audit tabloları oluşturuldu ve çalışıyor

---

## 9. Cloudflare R2 (Dosya Depolama)

- [ ] Production bucket oluşturuldu (dev bucket ile ayrı)
- [ ] R2 CORS politikası frontend domain'ini içeriyor
- [ ] Dosya upload ve pre-signed URL download uçtan uca test edildi
- [ ] R2 key'lerinin sadece gerekli izinleri var (put, get, delete)

---

## 10. Güvenlik Kontrolleri — Genel

- [ ] Tüm secret'lar `.env` veya secret manager'da (kod içinde yok)
- [ ] `.gitignore`'da `.env` dosyaları var
- [ ] Git geçmişinde kalan şifreler temizlendi (`git filter-branch` veya `BFG`)
- [ ] JWT secret production'a özgü ve rastgele üretildi
- [ ] `server.error.include-message=never` — stack trace dönmüyor
- [ ] Actuator endpoint'leri korunuyor (`/actuator/health` hariç erişim kısıtlı)
- [ ] CORS `allowed-origins` — sadece production frontend URL (wildcard yok)
- [ ] BCrypt password hashing aktif
- [ ] Dosya yükleme boyut ve tip kısıtlamaları çalışıyor

---

## 11. İşlevsel Test — Son Onay

Production ortamında elle doğrulama:

- [ ] Kullanıcı kaydı ve giriş (login/logout) çalışıyor
- [ ] JWT token yenileme (refresh) çalışıyor
- [ ] Token süresi dolduğunda 401 → otomatik refresh → sorunsuz devam
- [ ] Tenant izolasyonu — farklı tenant'lar birbirinin verisini görmüyor
- [ ] Finansal kayıt CRUD çalışıyor
- [ ] Onay akışı: DRAFT → PENDING_CAPTAIN → APPROVED
- [ ] Manager onay limiti (approvalLimit) doğru çalışıyor
- [ ] Dosya ekleme (attachment) upload ve görüntüleme çalışıyor
- [ ] Excel export çalışıyor
- [ ] Türkçe / İngilizce dil değiştirme çalışıyor
- [ ] Rapor sayfası verileri doğru yükleniyor
- [ ] 403 yetkisiz erişim denemeleri doğru yakalanıyor
- [ ] Eş zamanlı istek (concurrent request) sırasında token refresh çakışma yaratmıyor

---

## 12. Monitoring & Operasyon (Canlı Sonrası İlk Hafta)

- [ ] Prometheus metrikleri toplanıyor (`/actuator/prometheus`)
- [ ] Uptime monitoring aktif (ör. UptimeRobot, Healthchecks.io)
- [ ] Log aggregation kuruldu (ör. Loki, ELK, Papertrail)
- [ ] Hata bildirimi kuruldu (ör. Sentry) — frontend ve backend için
- [ ] Veritabanı bağlantı havuzu metrikleri izleniyor (HikariCP)
- [ ] R2 depolama kullanımı izleniyor
- [ ] SSL sertifika yenileme hatırlatıcısı kuruldu

---

## 13. Planlanan Geliştirmeler (Canlı Sonrası)

- [ ] Partial approve: `approvedAmount` approve endpoint'ine eklenmeli
- [ ] Email bildirimleri: onay bekleyen ve reddedilen kayıtlar için
- [ ] Bütçe yönetimi: kategori bazlı aylık/yıllık limitler
- [ ] Döviz kuru servisi: API entegrasyonu, cache, fallback
- [ ] Dosya import (Excel): `FileImportController` endpoint'leri aktif edilmeli
- [ ] Yacht Registration Wizard (v2): multi-step onboarding flow
- [ ] Mobil uyumlu onay akışı
- [ ] Banka API entegrasyonu: ödeme mutabakatı
- [ ] Token depolama: `HttpOnly + Secure` cookie'ye geçiş
- [ ] 2FA (iki faktörlü doğrulama) admin hesapları için
- [ ] Dosya yükleme: virüs tarama entegrasyonu

---

## Özet: Öncelik Sıralaması

| Öncelik | Konu | Bölüm |
|---------|------|-------|
| CRITICAL | Kaynak kodda açık şifreler | 1.1 |
| CRITICAL | Backend-Frontend endpoint uyumsuzluğu | 1.2 |
| CRITICAL | Frontend production URL placeholder | 1.3 |
| HIGH | Rate limiting yok | 2.1 |
| HIGH | JWT token revocation yok | 2.2 |
| HIGH | Eksik HTTP güvenlik başlıkları | 2.3 |
| HIGH | SystemAdminInitializer profil kısıtlaması yok | 2.4 |
| HIGH | Frontend'te ~70 console.log | 3.1 |
| HIGH | Frontend global error handler yok | 3.2 |
| MEDIUM | Zayıf şifre politikası (min 8 → 12) | 2.5 |
| MEDIUM | Eksik DTO validasyonları | 2.6 |
| MEDIUM | Token localStorage'da saklanıyor | 3.3 |

---

*Bu dosya projenin her iki dizininde de geçerlidir: `marine-management-system` (backend) ve `marine-managment-angular` (frontend).*
