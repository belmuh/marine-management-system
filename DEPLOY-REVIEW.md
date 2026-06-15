# Deploy Hazırlık İncelemesi — Marine Management System
**Kapsam:** Backend (Spring Boot 3.5.7 / Java 21) + Frontend (Angular 20) · Tarih: 2026-06-10

> **Durum güncellemesi (2026-06-10):** 1, 2, 3, 5, W1, W3, W5 ve W7 maddeleri giderildi
> (✔ işaretli). Kalan açık işler TODO.md'de, doğrulama adımları TEST-CHECKLIST.md'de.

---

## 🔴 KRİTİK — Deploy Engelleyiciler

### ✔ 1. CORS konfigürasyonu prod'da çalışmaz — GİDERİLDİ (2026-06-10)
`SecurityConfig.corsConfigurationSource()` origin'leri **hardcoded localhost** olarak tanımlıyor.
`application.properties` ve `application-prod.properties` içindeki `cors.allowed-origins=${CORS_ALLOWED_ORIGINS}` property'si **kodda hiçbir yerde okunmuyor**. Sonuç: prod'a çıktığınızda `https://maritar.com`'dan gelen tüm istekler CORS'a takılır.

**Çözüm:**
```java
@Value("${cors.allowed-origins}")
private List<String> allowedOrigins;
// ...
configuration.setAllowedOrigins(allowedOrigins);
```

### ✔ 2. Prod'da dosya yükleme sessizce çalışmıyor (R2 kapalı) — GİDERİLDİ (2026-06-10)
`application-prod.properties` R2 credential'larını tanımlıyor ama **`app.r2.enabled=true` set etmiyor**. `FileStorageService`'in default'u `false`, `R2StorageConfig` `@ConditionalOnProperty(havingValue="true")`. Sonuç: prod'da upload API'leri başarılı döner ama **hiçbir dosya kaydedilmez** — fark edilmesi en zor hata türü.

**Çözüm:** prod profile'a `app.r2.enabled=${R2_ENABLED:true}` ekleyin.

### ✔ 3. `@EnableScheduling` yok — zamanlanmış görevler hiç çalışmıyor — GİDERİLDİ (2026-06-10)
`RefreshTokenService.cleanupExpiredTokens()` `@Scheduled(cron = "0 0 2 * * ?")` ile işaretli ama projede `@EnableScheduling` hiçbir yerde yok. Expired refresh token'lar DB'de sonsuza dek birikir. `TenantAwareScheduledTask` da aynı şekilde ölü kod.

**Çözüm:** Ana uygulama sınıfına `@EnableScheduling` ekleyin.

### 3.5. Maven wrapper bozuk (sonradan tespit edildi)
`mvnw` script'i repoda var ama `.mvn/wrapper/maven-wrapper.properties` yok — temiz clone'da
`./mvnw` çalışmaz, CI/CD kurulamaz. Muhtemelen `.gitignore` `.mvn` klasörünü dışlıyor ya da
klasör hiç commit'lenmemiş. **Çözüm:** `mvn wrapper:wrapper` ile yeniden üret ve `.mvn` klasörünü commit'le.

### 4. Deploy artefaktı yok
İki repoda da `Dockerfile`, `docker-compose.yml`, CI/CD pipeline (`.github/workflows`), nginx config veya platform dosyası (railway/render/fly) yok. Backend root'unda `.gitignore` ve `README` da görünmüyor. "Nereye, nasıl deploy edilecek" sorusunun cevabı repoda yok.

**Çözüm (asgari):** Backend için multi-stage Dockerfile (maven build → JRE 21 runtime), frontend için `ng build` + nginx Dockerfile, basit bir GitHub Actions workflow, ortam değişkenleri için `.env.example`.

---

## 🟠 YÜKSEK — Çıkmadan önce düzeltilmeli

### ✔ 5. JwtAuthenticationFilter iç hata mesajlarını dışarı sızdırıyor — GİDERİLDİ (2026-06-10)
`sendErrorResponse(..., e.getMessage())` ile exception detayı JSON'a yazılıyor; ayrıca `String.format` ile elle JSON üretiliyor (mesajda `"` varsa bozuk JSON). Prod'da `server.error.include-message=never` özeniyle çelişiyor. Generic mesaj döndürün, detayı sadece loglayın; JSON'u ObjectMapper ile üretin.

### 6. Refresh token DB'de düz metin
`RefreshTokenService` token'ı olduğu gibi saklıyor. DB sızıntısında tüm aktif oturumlar ele geçirilebilir. SHA-256 hash'ini saklayıp lookup'ı hash üzerinden yapın. Ayrıca süre `plusDays(7)` hardcoded — `refresh.token.expiration` property'si yok sayılıyor.

### 7. Token'lar localStorage'da
Access + refresh token `localStorage`/`sessionStorage`'da → XSS durumunda her ikisi de çalınabilir. İdeal: refresh token'ı httpOnly+Secure cookie'ye taşımak. MVP için kabul edilebilir bir trade-off ama bilinçli karar olarak belgelenmeli; XSS yüzeyini daraltın (Angular zaten escape ediyor, `innerHTML`/`bypassSecurityTrust` kullanımlarını denetleyin).

### 8. Swagger izni var, Swagger yok
`SecurityConfig` `/swagger-ui/**` ve `/v3/api-docs/**`'u `permitAll` yapıyor ama pom'da springdoc yok. Ölü izin; ileride dependency eklenirse prod'da API dokümantasyonu herkese açılır. İzni kaldırın ya da profile bağlayın.

### 9. Test kapsamı dar
8 test dosyası, tamamı finance modülünde. **Multi-tenant SaaS için en kritik test eksik: tenant izolasyonu** (A organizasyonunun kullanıcısı B'nin verisini görebiliyor mu?). Auth akışı (login/refresh/verify) ve `JwtAuthenticationFilter`'daki tenant-mismatch kontrolü için integration test ekleyin.

### 10. JWT secret uzunluk doğrulaması yok
HS256 ≥ 256-bit secret ister. Kısa `JWT_SECRET` ile uygulama ya çöker ya zayıf imza üretir. Startup'ta `@PostConstruct` ile uzunluk kontrolü ekleyin (jjwt 0.11.5 da eski — 0.12.x'e yükseltin).

---

## 🟡 ORTA — Bilinçli karar / yakın vadede

- **Prod env checklist eksik:** `MAIL_ENABLED=true` set edilmezse doğrulama/şifre sıfırlama mailleri sessizce gitmez. Zorunlu env'ler: `DATABASE_URL/USERNAME/PASSWORD`, `JWT_SECRET`, `SYSTEM_ADMIN_PASSWORD`, `CORS_ALLOWED_ORIGINS`, `R2_*`, `MAIL_*`, `APP_VERIFY_URL`, `APP_RESET_PASSWORD_URL`. Bir `.env.example` yazın.
- **Rate limiting in-memory (Caffeine):** Tek instance için yeterli, yatay ölçeklemede çalışmaz — kod yorumunda belgelenmiş, doğru yaklaşım. Reverse proxy'nin `X-Forwarded-For`'u overwrite ettiğinden emin olun (spoofing).
- **Frontend'te 19 dosyada 64 `console.log/error`:** Prod build'de kalıyor. Bir `LoggerService` arkasına alın ya da build'de strip edin.
- **`auth.interceptor` `url.includes('/auth/')` ile token atlıyor:** `/auth/me` çağrısı da token'sız gider — bu endpoint kullanıcı kimliği gerektiriyorsa kırıktır. Skip listesini login/refresh/register ile sınırlayın.
- **Graceful shutdown + compression yok:** `server.shutdown=graceful` ve `server.compression.enabled=true` ekleyin.
- **Frontend bundle:** exceljs + chart.js ağır; 1MB budget'ı aşma riski var. Lazy loading ve `ng build --stats-json` ile doğrulayın.
- **`JwtAuthenticationFilter` her istekte DB lookup yapıyor:** Yük altında darboğaz olabilir; kısa TTL'li user cache düşünün.
- **`spring.flyway.baseline-on-migrate=true`** boş DB'de gereksiz; yanlış kullanımda migration atlamasına yol açabilir. Yeni DB için kaldırmayı değerlendirin.

---

## ⚙️ Workflow / İş Mantığı Hataları

### ✔ W1. Excel import → entry numarası çakışması — GİDERİLDİ (yorumlu blokta; kod şu an devre dışı)
> **Düzeltme notu (2026-06-10):** `DataImportService` gövdesi ve `FileImportController`'daki
> `/api/files/import` endpoint'i yorum satırında — özellik aktif değil, bug "uyuyan" kodda.
> Supplier pattern düzeltmesi yorumlu bloğa uygulandı; özellik açıldığında bug geri gelmez.
> Özelliği aktive ederken: yorumları kaldır + testleri yaz (TEST-CHECKLIST 4.x).
`DataImportService` import başında `NEXTVAL('financial_entry_seq')`'i **bir kez** çağırıp satırlar için numarayı lokalde `currentSequence++` ile artırıyor. DB sequence'ı yalnızca 1 ilerliyor. 50 satırlık import sonrası manuel oluşturulan ilk kayıt `NEXTVAL` ile **import'un 2. satırıyla aynı numarayı** alır → unique constraint hatası ya da duplicate fiş numarası. Eşzamanlı iki import'ta da aynı çakışma yaşanır.
**Çözüm:** Her satır için `NEXTVAL` çağırın ya da `setval`/`nextval(..., n)` ile bloğu atomik rezerve edin.

### W2. Entry sequence tenant-scoped DEĞİL — yorum yanıltıcı
`FinancialEntryFactory.generateEntryNumber()` üzerindeki yorum "TENANT ISOLATION: Sequence is tenant-scoped (auto-filtered)" diyor; oysa `SELECT NEXTVAL(...)` **global tek sequence** ve native query'lere Hibernate tenant filter zaten uygulanmaz. Sonuç: tüm organizasyonlar aynı sayacı paylaşır → tenant başına delikli numaralandırma (FE-2026-005'ten FE-2026-012'ye atlar) + numaradan diğer tenant'ların işlem hacmi tahmin edilebilir. Muhasebe fişi gibi ardışıklık bekleniyorsa yanlış davranıştır.
**Çözüm:** Tenant başına sayaç tablosu (`SELECT ... FOR UPDATE` ile) ya da `(tenant_id, yıl)` bazlı sequence stratejisi.

### ✔ W3. `/auth/me` her çağrıda gizli 401 → refresh → retry döngüsü — GİDERİLDİ (2026-06-10)
> Skip listesi `PUBLIC_AUTH_PATHS` sabitine daraltıldı; `/auth/me` artık ilk istekte token alıyor.
Frontend interceptor `url.includes('/auth/')` olan isteklere **Authorization header eklemiyor**, ama backend `/auth/me` `@AuthenticationPrincipal` bekliyor → her çağrı önce 401 alır, interceptor refresh çalıştırır, retry'da token ekler ve ancak o zaman başarılı olur. Yani çalışıyor *görünür* ama her `refreshCurrentUser()` = 3 HTTP isteği + gereksiz refresh. Refresh token'ı olmayan (session-only) kullanıcıda ise doğrudan logout'a düşürür.
**Çözüm:** Skip listesini `login/refresh/register/forgot/reset` ile sınırlayın; `/auth/me` ve `/auth/logout` token almalı.

### W4. Tek cihaz oturumu — muhtemelen istenmeyen davranış
`RefreshTokenService.createRefreshToken()` login'de `deleteByUserId()` ile kullanıcının **tüm** refresh token'larını siliyor. İkinci cihazdan (ör. telefon) login olan kullanıcının ilk cihazı (bilgisayar) access token süresi dolunca sessizce atılır. Bilinçli "tek oturum" politikası değilse, cihaz başına token saklayın (zaten `ipAddress`/`userAgent` alanları var — kullanılmıyor).

### ✔ W5. Refresh başarısız olursa kuyruktaki istekler sonsuza kadar asılı kalır — GİDERİLDİ (2026-06-10)
> Sentinel (REFRESH_FAILED) pattern'i ile çözüldü; subject.error() bilinçli olarak KULLANILMADI
> (BehaviorSubject kalıcı ölür, sonraki login'lerde kuyruk çalışmazdı).
Interceptor'da eşzamanlı 401'lerde ilk istek refresh'i başlatır, diğerleri `refreshTokenSubject.pipe(filter(t => t !== null))` ile bekler. Refresh **başarısız** olursa subject'e hiçbir değer emit edilmez → bekleyen istekler hiç tamamlanmaz, ilgili ekranlar süresiz loading'de kalır (logout yönlendirmesi sadece ilk isteği kurtarır).
**Çözüm:** Hata durumunda `refreshTokenSubject.error(...)` ya da bekleyenleri reddedecek bir sinyal yayınlayın.

### ✔ W7. Income listesi "hard refresh" sorunu — GİDERİLDİ (2026-06-10)
> Sohbette tespit edildi (raporda başlangıçta yoktu): `EntryService` root singleton ve filtre
> sinyalleri global; Expense sayfasının tarih/arama/kategori filtreleri Income'a taşınıyor,
> liste boş/eksik görünüyordu. Çözüm: `IncomeListStore.initialize()` başında `clearFilters()`.
> Kalıcı mimari çözüm (sayfa başına EntryService instance + olay tabanlı invalidation) TODO.md'de.

### W6. Daha küçük tutarsızlıklar
- **Refresh rotation yok + süre sabit 7 gün** (`plusDays(7)` hardcoded, `refresh.token.expiration` yok sayılıyor): aktif kullanıcı bile 7. gün oturum düşer (sliding expiration yok). UX kararıysa belgelenmeli.
- **`/auth/logout` yorumu "PROTECTED" diyor ama `/api/auth/**` permitAll** — gerçekte korumasız; body'deki refresh token tek doğrulama.
- **`switchTenant()` (frontend) sadece local user objesini değiştiriyor** — JWT'deki tenantId değişmediği için backend eski organizasyondan veri dönmeye devam eder. Şu an çağıran yok (ölü kod) ama gelecekte tuzak; kaldırın ya da yeniden-login akışına bağlayın.
- **TenantFilter `/api/auth`'u atlıyor** — bugün zararsız, ama `/api/auth` altına tenant verisi dönen bir endpoint eklenirse tenant izolasyonu sessizce devre dışı kalır.

---

## ✅ Doğru Yapılanlar

**Backend**
- Profil ayrımı temiz: dev (verbose, create-drop) / prod (validate, minimal log, hata detayı kapalı). Default'lar güvenli tarafta.
- Secrets env-var tabanlı, **default şifre yok** (`DATABASE_PASSWORD`, `JWT_SECRET`, `SYSTEM_ADMIN_PASSWORD` zorunlu) — repo'da hardcoded credential görmedim.
- `ddl-auto=validate` + Flyway migration'ları (V001–V009, Envers audit tabloları dahil) — şema yönetimi production-grade.
- `open-in-view=false`, Hikari tuning, Hibernate batch ayarları — JPA tarafı bilinçli.
- Security header'ları: HSTS, X-Frame-Options DENY, nosniff. Stateless JWT + RBAC + BCrypt.
- JWT'de tenant-mismatch doğrulaması (token'daki tenantId ↔ user'ın org'u) — token replay'e karşı doğru kontrol.
- Rate limiting (login 5/dk, register 3/saat) — trade-off'ları yorumda belgelenmiş.
- Multi-tenant mimari katmanlı ve tutarlı: ThreadLocal context + Hibernate filter + AOP + async task decorator.
- R2 presigned URL ile indirme (bandwidth app sunucusundan geçmiyor), uzantı whitelist'i, key sanitization.
- Modüler paket yapısı (modules/auth, finance, users, organization + shared) — DDD-vari, bakımı kolay.
- GlobalExceptionHandler: errorId korelasyonu, auth path'lerinde "Invalid credentials" genelleme (user enumeration önlenmiş).
- Actuator minimal exposure + Prometheus metrikleri.

**Frontend**
- Angular 20, standalone component'ler, functional interceptor/guard'lar — güncel idiom.
- `environments` + `fileReplacements` doğru kurulmuş; prod build default.
- Auth interceptor'da refresh queue pattern'i (`isRefreshing` + `BehaviorSubject`) — eşzamanlı 401'lerde tek refresh, doğru implementasyon.
- `authGuard` + `permissionGuard` ile route koruması.
- Transloco i18n + `Accept-Language` header'ı backend'e iletiliyor.
- Output hashing, budget tanımları, persistent/session storage ayrımı ("beni hatırla" mantığı).

---

## Önerilen Sıra (Güncel — 2026-06-10)

~~1. CORS fix~~ ✔ · ~~2. R2 + Scheduling~~ ✔ · ~~3. JWT filter sızıntısı~~ ✔ (secret doğrulama açık)

Kalanlar:
1. Backend'i IDE'de derle + TEST-CHECKLIST'i koş (bugünkü değişikliklerin doğrulaması)
2. Maven wrapper onarımı + Dockerfile'lar + `.env.example` + basit CI — yarım gün
3. Refresh token hash'leme + JWT secret doğrulama — yarım gün
4. Tenant izolasyon integration testi — yarım gün
5. Kalan orta öncelikliler (TODO.md) — sprint içinde

Kritik config engelleri kapandı; deploy artefaktları (madde 2) tamamlanmadan prod'a çıkılamaz.
