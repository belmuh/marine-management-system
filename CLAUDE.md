# Maritar — Claude Çalışma Kılavuzu

## Proje Yapısı

**marine-management-system** — Spring Boot 3.5 backend (Java 21)
- Multi-tenant SaaS: her organization kendi tenant'ı, Hibernate row-level filter ile izole
- JWT (access token, 1 saat) + refresh token (httpOnly cookie, 7 gün)
- SHA-256 hashed refresh tokens in DB
- Flyway migration, ddl-auto=validate (prod)
- `application-dev.properties` → local dev overrides

**marine-managment-angular** — Angular 17+ frontend (signals, standalone components)
- Transloco i18n (tr/en)
- `environment.ts` / `environment.prod.ts` config
- `auth.interceptor.ts` → Bearer token + withCredentials, 401 refresh queue
- `storage.service.ts` → access token localStorage/sessionStorage, refresh token httpOnly cookie'de

## Tenant İzolasyonu

- `TenantFilter` → `/api/auth/**` ve `/actuator/**` hariç her istekte TenantContext set eder
- `TenantContext` → ThreadLocal Long tenantId
- `JwtAuthenticationFilter` → JWT'deki tenantId claim'ini user.organizationId ile çapraz kontrol eder
- Auth endpoint'leri (`/api/auth/**`) tenant context'siz çalışır — tasarım gereği

## Çalışma Kuralları

### Kod yazmadan önce

Her değişiklik öncesinde şunları sesli düşün ve listele:

1. **Edge case'ler** — "Bu kod ne zaman bozulur?" (null, boş, timeout, her iki token da süresi dolmuş, vb.)
2. **Etkilenen diğer dosyalar** — Değiştirdiğim şeyi başka kimler kullanıyor?
3. **Tenant izolasyonu** — Bu değişiklik cross-tenant sızıntısına yol açar mı?
4. **Geri alma** — Deploy sonrası sorun çıksa rollback nasıl yapılır?

Bu adım atlanamaz. "Hızlıca yazayım, sonra review ederiz" modu yoktur.

### Commit stratejisi

- Her değişikliği tek tek commit etme — 3-4 ilgili değişikliği bir arada commit et
- Belma onay vermeden `git push` yapma
- Commit mesajı formatı: `type(scope): açıklama` (feat, fix, refactor, chore)

### Önce sor, sonra yaz

- Birden fazla dosya etkileyecek değişikliklerde önce planı yaz, Belma onay versin
- Silme işlemlerinde (metod, endpoint, tablo) mutlaka sor
- "Hızlıca halledelim" diye geçiştirme — küçük görünen değişiklikler büyük etki yaratabilir

### Test altyapısı

Test altyapısı mevcut. Değişiklik sonrası:
- Backend: `./mvnw test -q` (Testcontainers ile gerçek DB)
- Frontend: `npx tsc --noEmit`
- Smoke testler (`SmokeIntegrationTest`) deploy öncesi mutlaka geçmeli

## Bilinen Kararlar

- **Single session per user**: Login yeni cihazda eski tüm refresh token'ları siliyor (`deleteByUserId`) — kasıtlı
- **`/api/auth/logout` protected**: Bearer token gerektirir; JWT yoksa HTTP call yapılmaz, sadece local state temizlenir
- **`SameSite=Strict`**: `app.maritar.com` + `api.maritar.com` same-site (aynı eTLD+1) — doğru seçim
- **`withCredentials: true`** sadece `environment.apiUrl`'e yapılan isteklere eklenir
- **`app.cookie.secure=false`** dev profile'da, prod'da `COOKIE_SECURE=true` env zorunlu

## Pending (Yapılacaklar)

- [x] Testler: MoneyTest, TenantIsolationIntegrationTest, SmokeIntegrationTest (T1-T8 auth flow)
- [x] Sentry entegrasyonu: backend (SENTRY_DSN env) + frontend (@sentry/angular)
- [x] Demo data review: S/Y Maritar, uluslararası ekip, manager approval 500 EUR
