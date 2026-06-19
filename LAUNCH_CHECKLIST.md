# Marine Management System — Pilot Launch Checklist

> 2 yat işletmecisini pilot olarak alıp gerçek kullanım verisi toplamak için canlıya çıkış kontrol listesi.
> Tarih: 2026-05-06
> Hedef: 5-7 iş gününde tüm P0 maddelerin kapatılması.

---

## 🚦 Renk kodu

- 🔴 **P0 — Blocker.** Bu maddeler kapanmadan canlıya çıkma. Veri sızıntısı, sessiz hata veya immediate prod outage riski var.
- 🟡 **P1 — Launch haftası.** Canlıya çıkışın ilk 7 günü içinde kapat. Olmadan da çıkılabilir ama erken adres gerek.
- 🟢 **P2 — İlk 30 gün.** Pilotun verisiyle birlikte yapacağın iyileştirmeler. Aciliyeti yok.

---

## ✅ P0 — Tamamlananlar

- ~~**1. Backend — CORS env-var'a bağla**~~ → `SecurityConfig` `cors.allowed-origins` property'sini okuyor, trim + filter ile temiz. *(2026-06-10)*
- ~~**3. Frontend — `environment.prod.ts` gerçek API URL**~~ → `https://api.maritar.com/api` set edildi. *(2026-06-18)*

---

## 🔴 P0 — Kalan Blockerlar

### [ ] 1. Backend — SYSTEM tenant fallback'ı kapat *(1-2 gün)*

**Sorun:** `HibernateTenantIdentifierResolver` context yokken `"SYSTEM"` döndürüyor; `TenantEntityListener` aynı durumda `IllegalStateException` atıyor. Bu iki kontrat çelişiyor — tek bir senaryo cross-tenant veri sızıntısı riski yaratıyor.

**Aksiyon seçenekleri (birini seç):**

**Seçenek A (önerilen, hızlı):** Resolver'ı sertleştir — context yoksa exception at, sadece açıkça izin verilen path'ler için bypass.
```java
@Override
public String resolveCurrentTenantIdentifier() {
    if (TenantContext.hasTenantContext()) {
        return TenantContext.getCurrentTenantId().toString();
    }
    if (TenantBypassContext.isBypassActive()) {  // explicit opt-in
        return SYSTEM_TENANT;
    }
    throw new IllegalStateException("Tenant context required");
}
```

**Seçenek B (daha temiz, biraz daha uzun):** `@CrossTenantOperation` annotation tanımla, sadece bu annotation'lı service metodlarında SYSTEM tenant kullanılabilsin. AOP ile zorlat.

**Kabul kriteri:** Authenticated olmayan herhangi bir endpoint'te tenant entity insert/update denenirse `IllegalStateException` patlar, log'da kayıt kalır.

---

### [ ] 2. Backend — Error tracking bağla *(yarım gün)*

**Sorun:** Pilotun *tüm amacı* hatalardan öğrenmek. Sentry / Bugsnag / GlitchTip yoksa hatadan haberin olmaz, kullanıcı bildirmeden önce göremezsin.

**Aksiyon (Sentry örneği):**
```xml
<!-- pom.xml -->
<dependency>
    <groupId>io.sentry</groupId>
    <artifactId>sentry-spring-boot-starter-jakarta</artifactId>
    <version>7.14.0</version>
</dependency>
```
```properties
# application-prod.properties
sentry.dsn=${SENTRY_DSN}
sentry.environment=production
sentry.traces-sample-rate=0.1
sentry.send-default-pii=false
```
- `GlobalExceptionHandler`'daki `errorId` Sentry tag'i olarak gönderilsin (`Sentry.setTag("errorId", errorId)`).
- Frontend tarafında da `@sentry/angular` ile error.interceptor'dan event gönder.

**Kabul kriteri:** Bilerek bir 500 hatası tetiklediğinde Sentry dashboard'da event görünüyor + frontend'in attığı errorId backend'inkiyle eşleşiyor.

---

### [ ] 3. PostgreSQL otomatik yedekleme *(yarım gün)*

**Sorun:** Pilot verisini kaybetmek hukuki ve güven sorunudur.

**Aksiyon:** Hangi hosting kullanıyorsan ona göre:
- **Managed (RDS, Supabase, Neon, DigitalOcean Managed PG):** Daily automated backup'ı aç, retention 7-30 gün, PITR varsa aç.
- **Self-hosted:** `pg_dump` cron job + S3/R2'ye encrypted upload.

**Kabul kriteri:** Bir test backup'tan staging veritabanına restore deneme yap, başarılı olduğunu doğrula. (Test edilmemiş backup, backup değildir.)

---

### [ ] 4. Smoke test suite — happy path *(2 gün)*

**Sorun:** Şu an test yok denecek kadar az; her deploy'da manuel regresyon yapmak yavaşlatır ve hata atlar.

**Aksiyon:** Aşağıdaki minimum 8 entegrasyon testi (Spring Boot `@SpringBootTest` + Testcontainers Postgres):

1. Onboarding: yeni org register → setup → first user login.
2. CREW: entry oluştur → submit.
3. CAPTAIN: kendi entry'sini oluştur → auto-approve.
4. CAPTAIN: CREW'in entry'sini approve.
5. MANAGER: pending entry'yi partial approve.
6. CAPTAIN: approved entry'ye payment kaydet → status PAID.
7. Cross-tenant izolasyon: Org A user'ı Org B entry'sini görememeli.
8. JWT expired → refresh akışı.

**Kabul kriteri:** `mvn test` her deploy öncesi koşuluyor, 8 testin hepsi yeşil.

---

## 🟡 P1 — Launch Haftası İçinde

### [ ] 7. Frontend — CSP header ekle *(1 saat)*

`index.html`'e veya reverse proxy (nginx/caddy) seviyesinde:
```html
<meta http-equiv="Content-Security-Policy"
      content="default-src 'self';
               script-src 'self';
               style-src 'self' 'unsafe-inline';
               img-src 'self' data: https://*.r2.cloudflarestorage.com;
               connect-src 'self' https://api.marine-domain.com;
               frame-ancestors 'none';">
```
JWT localStorage'da olduğu için XSS savunmasının ilk hattı CSP'dir.

---

### [ ] 8. `attachment.store.ts:179` — bypassSecurityTrustResourceUrl audit *(15 dakika)*

URL'in kaynağının yalnızca backend presigned URL veya `URL.createObjectURL(blob)` olduğunu doğrula. User input'tan gelmiyorsa OK; kod review yap.

---

### [ ] 9. Pilot kullanıcılar için feedback kanalı *(yarım gün)*

- Slack Connect channel veya Discord (her iki yat işletmecisi için ayrı).
- In-app bir "Geri bildirim" butonu (zaten varsa atla).
- İlk 30 gün haftalık 30 dk video call commit.

---

### [ ] 10. Release versiyonlama + rollback planı *(yarım gün)*

- Git tag (`v0.1.0-pilot.1`).
- Docker image tag aynı.
- Önceki sürüme tek komutla rollback dökümante.
- Database migration'lar pilotta **forward-only** kabul et — rollback için snapshot restore.

---

### [ ] 11. Health check + uptime monitoring *(15 dakika)*

`/actuator/health` zaten açık. UptimeRobot, Better Uptime veya Pingdom ile 1-5 dk aralıkla ping. Down olunca SMS/email/push.

---

## 🟢 P2 — İlk 30 Gün İçinde

### [ ] 12. `Money` + `FinancialEntry` unit testleri

Sentry'den ilk hafta gelen verilerle hangi alanların gerçekten kırılganlaştığını gör, **gerçek bug'ları cover eden** testler yaz. Spekülatif testlere zaman harcama.

### [ ] 13. `RoleHierarchy` Spring bean

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

### [ ] 14. Domain → application bağımlılık ihlalini düzelt

`FinancialEntry.calculateBaseAmount(ExchangeRateService, ...)` → domain'de `ExchangeRateProvider` interface tanımla, `application.ExchangeRateService` bunu implement etsin. Domain'in application'ı import etmesi gerekmesin.

### [ ] 15. dev profilinde Flyway'i aç

`application-dev.properties`:
```properties
spring.flyway.enabled=true
spring.jpa.hibernate.ddl-auto=validate
```
Bu, "bende çalışıyor" sendromunu önler.

### [ ] 16. OpenAPI/Swagger dependency ekle

`SecurityConfig`'te `/swagger-ui/**` permitAll var ama springdoc yok. Ya kaldır ya ekle:
```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.6.0</version>
</dependency>
```

### [ ] 17. JWT — refresh token reuse detection

Refresh token kullanıldığında DB'de `used=true` işaretle, aynı refresh token tekrar gelirse "token theft" detection — tüm session'ları invalidate et.

---

## 📊 Pilot Dönemi Başarı Metrikleri (ilk 30 gün)

Bu metrikler kod kalitesinden değil, kullanıcı davranışından konuşur — pilotun gerçek değeri burada.

| Metrik | Hedef | Ölçüm |
|---|---|---|
| Sentry'deki unique error sayısı (haftalık) | < 5 | Sentry dashboard |
| Kullanıcının kendi başına tamamlayabildiği entry oluşturma oranı | > %80 | UX feedback + log |
| Login → ilk anlamlı aksiyon süresi (TTV — time to value) | < 3 dk | Frontend timing |
| Pilot kullanıcı hangi feature'a hiç dokunmadı? | Listele | Backend access log |
| Hangi flow'da en çok destek talebi geldi? | Listele | Slack/Discord history |
| Cross-tenant isolation violation log'u | **0** | Backend `TenantFilterMetrics` |

**Son kuralı tekrar et:** Pilotun amacı bug düzeltmek değil, *neyin önemli olduğunu öğrenmek*. P2 listesine ekleyeceğin yeni maddeler kullanıcıdan gelmeli, kod review'dan değil.

---

## ✅ Çıkış Günü Final Smoke Check (manuel, 15 dakika)

Pilot kullanıcılar ilk login'i atmadan önce sen bunları sırasıyla doğrula:

1. [x] Production domain'inden login başarılı dönüyor (CORS ✓).
2. [ ] Login sonrası dashboard yükleniyor.
3. [ ] Yeni entry oluşturuldu, kaydedildi.
4. [ ] Approval flow çalışıyor (test hesabıyla CAPTAIN onayı verildi).
5. [ ] Logout sonrası storage temiz (DevTools → Application → localStorage boş).
6. [ ] `/actuator/health` 200 dönüyor.
7. [ ] Sentry'ye test hatası düştü ve görünüyor.
8. [ ] Backup planı çalıştı (en son backup timestamp güncel).
9. [ ] Production env değişkenleri set edildi: `JWT_SECRET`, `DATABASE_PASSWORD`, `R2_*`, `CORS_ALLOWED_ORIGINS`, `SENTRY_DSN`, `MAIL_*`.
10. [ ] Pilot kullanıcılara hesap oluşturuldu, onlara giriş bilgileri *güvenli kanaldan* gönderildi (email değil — şifre değiştirme link'i).

---

## Bir kez daha hatırlat: bu liste pilot için.

Üretim seviyesi B2B SaaS olarak satışa açacağın gün için ayrı bir checklist gerekir (Postgres RLS, Redis-backed rate limiter, distributed tracing, %80 test coverage, JWT key rotation, Spring Modulith, GDPR uyumluluk, ToS+Privacy Policy, Stripe entegrasyonu, vs.). Pilot başarılı olduktan sonra konuşalım.
