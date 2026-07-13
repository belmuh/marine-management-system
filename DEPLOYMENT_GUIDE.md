# Marine Management System — Deployment Guide & Yol Haritası

> ⚠️ **GÜNCELLEME (2026-07-13):** Sistem VPS'e taşınıyor. Güncel as-built durum için
> **Bölüm 17 (VPS Geçişi)**, açık işler için `TODO.md` → "🟠 VPS Geçişi". Bölüm 1–16'daki
> servis kurulum adımları (Cloudflare, R2, Sentry vb.) geçerli referanstır; Render'a
> özgü bölümler (6, 9) tarihsel kayıttır.

> Pilot canlıya alma için kanonik referans dokümanı.
> Bu doküman okuyan birinin hiçbir başka yere bakmadan A'dan Z'ye deploy yapabilmesi için yazıldı.
> Son güncelleme: 2026-06-23
> Platform kararı: **Render.com** (Fly.io ücretli olduğu için elendi)

---

## Kaldığımız Yer

- Render.com'da GitHub repo bağlandı (`marine-management-system`)
- Environment variables girişi ve deploy aşamasında **durdu**
- R2, airport WiFi'da ödeme sorunu yüzünden aktive edilemedi
- `.env`'e `APP_R2_ENABLED=false` eklendi → R2 olmadan deploy edilebilir (dosya yükleme çalışmaz, geri kalan sistem çalışır)
- Neon şifresi airport WiFi'da girildi → **güvenli ağa geçince resetlenmeli**
- **Sonraki adım:** Render dashboard'da environment variables'ları gir ve "Manual Deploy" tetikle

---

## 1. Mimari Özet

```
┌──────────────────────────────────────────────────────────────┐
│   Pilot Kullanıcı (Browser)                                  │
└──────────────────────┬───────────────────────────────────────┘
                       │ https://maritar.com
                       ▼
            ┌─────────────────────┐
            │  Cloudflare Proxy   │  ← WAF, CDN, SSL, DDoS koruması
            └──────────┬──────────┘
                       │
        ┌──────────────┴──────────────┐
        ▼                             ▼
 ┌──────────────┐             ┌──────────────────┐
 │ Cloudflare   │             │   Render.com     │
 │ Pages        │             │   (Docker)       │
 │ (Frontend)   │  ──API──►   │   Spring Boot    │
 │ Angular dist │             │   Backend        │
 └──────────────┘             └────────┬─────────┘
                                       │
                       ┌───────────────┼───────────────┐
                       ▼               ▼               ▼
                ┌──────────┐   ┌────────────┐   ┌──────────┐
                │  Neon    │   │ Cloudflare │   │  Brevo   │
                │ Postgres │   │    R2      │   │  (SMTP)  │
                │   3 GB   │   │  10 GB     │   │ 300/gün  │
                └──────────┘   └────────────┘   └──────────┘

         Yan servisler:                Backup:
         • Sentry (error tracking)     • GitHub Actions cron
         • UptimeRobot (ping)          • pg_dump → R2 (günlük)
         • GitHub Actions (CI/CD)
```

Tek artifact (Spring Boot Docker container) + tek static site (Angular dist) + dış servisler. Tüm bileşenler bağımsız taşınabilir; bir provider değişimi tüm sistemin taşınmasını gerektirmez.

---

## 2. Deploy Ortam Listesi

| Bileşen | Servis | URL | Free tier sınırı | Aylık maliyet |
|---|---|---|---|---|
| Domain + DNS + CDN + WAF | Cloudflare | https://dash.cloudflare.com | Sınırsız bandwidth | $0.83 (`.com` $10/yıl) |
| Frontend hosting | Cloudflare Pages | https://pages.cloudflare.com | Sınırsız build, sınırsız bandwidth | $0 |
| Backend hosting | Render.com | https://render.com | 512 MB RAM, 0.1 CPU, aylık 750 saat | $0 |
| PostgreSQL | Neon | https://neon.tech | 3 GB DB, 7-gün PITR, branching | $0 |
| File storage | Cloudflare R2 | (Cloudflare panel) | 10 GB storage, sıfır egress | $0 |
| E-posta gönderimi | Brevo | https://www.brevo.com | 300 mail/gün | $0 |
| Error tracking | Sentry | https://sentry.io | 5K event/ay (backend), 5K (frontend) | $0 |
| Uptime monitoring | UptimeRobot | https://uptimerobot.com | 50 monitor, 5 dk aralık | $0 |
| CI/CD | GitHub Actions | (GitHub repo) | 2.000 dk/ay | $0 |
| Source control | GitHub | https://github.com | Sınırsız private repo | $0 |
| Backup hedefi | Cloudflare R2 (ayrı bucket) | (Cloudflare panel) | 10 GB içinde dahil | $0 |
| **TOPLAM** | | | | **~$1/ay** |

Pilot dönem aylık nakit harcaması: domain dışında $0.

**Not:** Render free tier 15 dakika idle'dan sonra servisi uyutur, ilk istekte ~30 saniye cold start olur. Pilot kullanım için kabul edilebilir. UptimeRobot ping'i bunu önlemek için kullanılabilir (5 dk aralıkla `keep-alive`).

---

## 3. Yol Haritası — Kronolojik Akış

### Gün 0 — Hazırlık (1-2 saat)
- [ ] GitHub'da private repo'lar var mı kontrol et (backend + frontend)
- [ ] Tek bir password manager'da tüm credential'ları toplayacak boş bir bölüm aç (1Password, Bitwarden, KeePassXC)

### Gün 1 — Hesaplar ve domain (yarım gün)
- [ ] Cloudflare hesabı aç → domain satın al (Bölüm 4.1)
- [ ] Neon hesabı aç → production database oluştur (Bölüm 4.2) — **Neon şifresini güvenli ağda resetle**
- [ ] Render.com hesabı aç → web service oluştur (Bölüm 4.3)
- [ ] Brevo hesabı aç → sender email doğrula (Bölüm 4.4)
- [ ] Sentry hesabı aç → 2 proje oluştur (backend + frontend) (Bölüm 4.5)
- [ ] UptimeRobot hesabı aç (Bölüm 4.6)
- [ ] Cloudflare R2 → 2 bucket oluştur (`marine-attachments` + `marine-backups`) (Bölüm 4.7)

### Gün 2 — Backend Render Deploy (1 gün)
- [ ] `Dockerfile` projenin kökünde var mı kontrol et (Bölüm 5)
- [ ] Render dashboard'da environment variables'ları gir (Bölüm 6.2) ← **kaldığımız yer**
- [ ] "Manual Deploy" tetikle → deploy loglarını izle
- [ ] Health check doğrula: `curl https://marine-backend.onrender.com/actuator/health`

### Gün 3 — Frontend + DNS (yarım gün)
- [ ] Frontend `environment.prod.ts`'ı gerçek API URL'iyle güncelle
- [ ] GitHub repo'yu Cloudflare Pages'e bağla (Bölüm 7)
- [ ] DNS kayıtlarını oluştur (Bölüm 8)
- [ ] Frontend'in domain'den açıldığını doğrula
- [ ] CORS env-var'ını backend'e set et, redeploy

### Gün 4 — CI/CD + Backup + Monitoring (yarım gün)
- [ ] GitHub Actions deploy workflow ekle (Bölüm 9)
- [ ] GitHub Actions backup workflow ekle (Bölüm 10)
- [ ] Sentry SDK'yı backend ve frontend'e entegre et (Bölüm 11)
- [ ] UptimeRobot'a `/actuator/health` ve `https://maritar.com`'u ekle
- [ ] Bir kere kasıtlı 500 fırlat, Sentry'de göründüğünü doğrula

### Gün 5 — Smoke test + pilot davet (yarım gün)
- [ ] Bölüm 13'teki smoke testleri koş
- [ ] İlk pilot tenant manuel oluştur
- [ ] Pilot kullanıcıya giriş bilgileri güvenli kanaldan gönder

---

## 4. Servis Kurulumları

### 4.1 Cloudflare — Domain + DNS + Pages + R2

**Hesap aç:**
1. https://dash.cloudflare.com/sign-up
2. E-posta + güçlü şifre, 2FA'yı **mutlaka aç**

**Domain satın al:**
1. Sol menü → **Domain Registration → Register Domains**
2. İstediğin domain'i ara, sepete ekle
3. WHOIS Privacy → **otomatik açık** (Cloudflare'de bedava)
4. Auto-renew → açık tut

**R2 bucket'ları oluştur:**
1. Sol menü → **R2 Object Storage**
2. **Create bucket**: `marine-attachments` (production attachment'ları için)
3. **Create bucket**: `marine-backups` (DB backup'ları için)
4. **Manage R2 API Tokens** → **Create API Token**:
   - Permission: `Object Read & Write`
   - Specify bucket: `marine-attachments` ve `marine-backups`
   - **Access Key ID + Secret Access Key**'i password manager'a kaydet (bir daha gösterilmez)
5. Account ID'yi de not al (R2 panelinin sağ tarafında görünür)

---

### 4.2 Neon — PostgreSQL

**Hesap aç ve project oluştur:**
1. https://console.neon.tech/signup → GitHub ile giriş
2. **Create project**:
   - Project name: `marine-production`
   - Postgres version: 16
   - Region: **Frankfurt (EU Central)** — Render ile aynı region
3. Connection string'i kopyala (`postgresql://user:pass@host/db?sslmode=require`)

**PITR'ı aktif et:**
1. Project → **Settings → Storage**
2. **Point-in-time restore** → enable, retention 7 gün

**Önemli:** Airport WiFi'da girilen şifre **güvenli ağa geçince resetlenmeli**.

---

### 4.3 Render.com — Backend Hosting

**Hesap aç:**
1. https://render.com → GitHub ile sign up (en kolayı)
2. 2FA'yı aç

**Web Service oluştur:**
1. Dashboard → **New → Web Service**
2. **Connect a repository** → `marine-management-system` repo'sunu seç
3. Ayarlar:
   - **Name**: `marine-backend`
   - **Region**: Frankfurt (EU Central)
   - **Branch**: `main`
   - **Runtime**: Docker
   - **Dockerfile Path**: `./Dockerfile`
   - **Instance Type**: Free

4. Environment Variables bölümünde (Bölüm 6.2) tüm secret'ları gir
5. **Create Web Service** → deploy başlar

**Deploy hook al (CI/CD için):**
- Service → **Settings → Deploy Hook** → URL'i kopyala → GitHub Actions secret'ına ekle

**Log izleme:**
- Service → **Logs** sekmesi (canlı log akışı)

**Not:** Free tier'da servis 15 dk idle'dan sonra uyur. UptimeRobot 5 dk'da bir ping atarsa uyanık kalır — bu kabul edilebilir bir trade-off, agresif ping yerine cold start'ı tolere etmek daha sağlıklı.

---

### 4.4 Brevo — E-posta gönderimi

1. https://onboarding.brevo.com/account/register
2. **Sender → Senders & IP → Add a Sender**:
   - From name: `Marine System`
   - From email: `noreply@maritar.com`
3. **SMTP & API → SMTP**:
   - Login ve relay host adresini al → password manager'a kaydet
4. DNS hazır olunca (Gün 3):
   - **Senders → Authenticate your domain**
   - SPF + DKIM + DMARC TXT record'larını Cloudflare DNS'e ekle (Bölüm 8)

---

### 4.5 Sentry — Error tracking

1. https://sentry.io/signup/ → GitHub ile
2. Organization name: `marine-system`
3. **Create project**: `marine-backend`, platform → Java / Spring Boot. DSN'i kopyala.
4. **Create project**: `marine-frontend`, platform → Angular. DSN'i kopyala.
5. Her iki DSN'i password manager'a kaydet

---

### 4.6 UptimeRobot — Uptime monitoring

1. https://uptimerobot.com/signUp → Free plan
2. Deploy bittikten sonra **Add New Monitor**:
   - Type: HTTP(s)
   - URL: `https://api.maritar.com/actuator/health`
   - Interval: 5 minutes
3. Frontend için: `https://maritar.com`

---

### 4.7 R2 (Cloudflare) — özet

Bu zaten Bölüm 4.1'de hallolduysa burada tekrar etmeye gerek yok. Sadece şunu doğrula:
- Account ID elinde
- Access Key ID + Secret Access Key elinde
- Endpoint URL formatı: `https://<account_id>.r2.cloudflarestorage.com`

---

## 5. Backend Dockerization

### 5.1 `Dockerfile`

Projenin kökünde zaten var. Kontrol et, yoksa ekle:

```dockerfile
# Multi-stage build: küçük final image
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app
COPY mvnw mvnw.cmd pom.xml ./
COPY .mvn .mvn
RUN ./mvnw dependency:go-offline -B

COPY src src
RUN ./mvnw clean package -DskipTests -B

FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S marine && adduser -S marine -G marine

WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
RUN chown -R marine:marine /app

USER marine

HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

EXPOSE 8080

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=50.0"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

### 5.2 `.dockerignore`

```
target/
.git/
.idea/
.vscode/
*.md
HELP.md
.gitattributes
node_modules/
.env*
.DS_Store
```

### 5.3 Lokal test

```bash
docker build -t marine-backend:local .

docker run --rm -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=dev \
  -e DATABASE_URL="..." \
  -e JWT_SECRET="..." \
  marine-backend:local

curl http://localhost:8080/actuator/health
```

`{"status":"UP"}` dönerse başarılı.

---

## 6. Render.com Backend Deploy

### 6.1 `render.yaml` (opsiyonel, Infrastructure as Code)

Projenin köküne eklenirse Render konfigürasyonu kod olarak takip edilir:

```yaml
services:
  - type: web
    name: marine-backend
    runtime: docker
    region: frankfurt
    plan: free
    branch: main
    dockerfilePath: ./Dockerfile
    healthCheckPath: /actuator/health
    envVars:
      - key: SPRING_PROFILES_ACTIVE
        value: prod
```

### 6.2 Environment Variables (Render Dashboard)

Render Dashboard → Service → **Environment** sekmesine şunları gir:

| Key | Value |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `DATABASE_URL` | Neon connection string |
| `DATABASE_USERNAME` | `neondb_owner` |
| `DATABASE_PASSWORD` | Neon'dan alınan şifre |
| `JWT_SECRET` | `openssl rand -base64 64` ile üretilmiş değer |
| `SYSTEM_ADMIN_PASSWORD` | Güçlü şifre |
| `CORS_ALLOWED_ORIGINS` | `https://maritar.com,https://www.maritar.com` |
| `APP_R2_ENABLED` | `false` (R2 kurulana kadar) |
| `R2_ACCOUNT_ID` | Cloudflare Account ID |
| `R2_ACCESS_KEY` | Cloudflare R2 Access Key |
| `R2_SECRET_KEY` | Cloudflare R2 Secret Key |
| `R2_BUCKET` | `marine-attachments` |
| `MAIL_USERNAME` | Brevo SMTP login |
| `MAIL_PASSWORD` | Brevo SMTP key |
| `MAIL_FROM` | `noreply@maritar.com` |
| `MAIL_ENABLED` | `true` |
| `SENTRY_DSN` | Sentry backend DSN |
| `COOKIE_SECURE` | `true` |

Kaydettikten sonra **Manual Deploy → Deploy latest commit**.

### 6.3 Deploy doğrula

```bash
curl https://marine-backend.onrender.com/actuator/health
```

`{"status":"UP"}` dönmeli. İlk deploy 5-10 dk sürebilir (Docker build).

---

## 7. Frontend Cloudflare Pages Deploy

### 7.1 Repo bağla

1. Cloudflare dashboard → **Workers & Pages → Create → Pages → Connect to Git**
2. GitHub'a yetki ver, `marine-managment-angular` repo'sunu seç
3. Build configuration:
   - **Framework preset**: Angular
   - **Build command**: `npm run build`
   - **Build output directory**: `dist/marine-managment-angular/browser`
   - **Environment variables**: `NODE_VERSION=20`

### 7.2 `environment.prod.ts` güncelle

```typescript
export const environment = {
  production: true,
  apiUrl: 'https://api.maritar.com/api',
};
```

Commit + push → Cloudflare Pages otomatik build alır.

### 7.3 Custom domain bağla

1. Cloudflare Pages projesi → **Custom domains → Set up a custom domain**
2. `maritar.com` ve `www.maritar.com` ekle
3. DNS otomatik kurulur (zaten Cloudflare'desin)

---

## 8. DNS Konfigürasyonu

Cloudflare DNS paneli → seçilen domain → DNS → Records:

| Type | Name | Content | Proxy | Açıklama |
|---|---|---|---|---|
| `CNAME` | `@` | `marine-frontend.pages.dev` | 🟠 Proxied | Frontend |
| `CNAME` | `www` | `marine-frontend.pages.dev` | 🟠 Proxied | www → ana domain |
| `CNAME` | `api` | `marine-backend.onrender.com` | 🟠 Proxied | Backend API |
| `TXT` | `@` | (Brevo'nun verdiği SPF değeri) | — | E-posta SPF |
| `TXT` | `mail._domainkey` | (Brevo DKIM değeri) | — | E-posta DKIM |
| `TXT` | `_dmarc` | `v=DMARC1; p=quarantine; rua=mailto:dmarc@maritar.com` | — | DMARC |

**Önemli:** Proxy 🟠 açık olduğunda Cloudflare CDN/WAF/SSL devreye girer.

---

## 9. CI/CD — GitHub Actions

### 9.1 Backend deploy workflow

`.github/workflows/deploy.yml`:

```yaml
name: Deploy Backend

on:
  push:
    branches: [main]

jobs:
  deploy:
    runs-on: ubuntu-latest
    concurrency: deploy-backend
    steps:
      - name: Trigger Render Deploy
        run: |
          curl -X POST "${{ secrets.RENDER_DEPLOY_HOOK_URL }}"
```

GitHub repo → **Settings → Secrets → Actions → New repository secret**:
- Name: `RENDER_DEPLOY_HOOK_URL`
- Value: Render dashboard → Service → Settings → Deploy Hook URL

### 9.2 Frontend deploy

Cloudflare Pages otomatik git push'a tepki verir, ekstra workflow gerekmez.

---

## 10. Backup Otomasyonu

### 10.1 GitHub Actions cron — günlük pg_dump → R2

`.github/workflows/backup.yml`:

```yaml
name: Daily DB Backup

on:
  schedule:
    - cron: "0 3 * * *"   # Her gün UTC 03:00 (TR saatiyle 06:00)
  workflow_dispatch:

jobs:
  backup:
    runs-on: ubuntu-latest
    steps:
      - name: Install postgresql-client
        run: sudo apt-get update && sudo apt-get install -y postgresql-client

      - name: Install AWS CLI
        run: |
          curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
          unzip awscliv2.zip
          sudo ./aws/install --update

      - name: Dump database
        env:
          DATABASE_URL: ${{ secrets.DATABASE_URL }}
        run: |
          TIMESTAMP=$(date -u +"%Y%m%d_%H%M%S")
          pg_dump --format=custom --no-owner --no-acl "$DATABASE_URL" \
            > "backup_${TIMESTAMP}.dump"
          echo "BACKUP_FILE=backup_${TIMESTAMP}.dump" >> $GITHUB_ENV

      - name: Upload to R2
        env:
          AWS_ACCESS_KEY_ID: ${{ secrets.R2_ACCESS_KEY }}
          AWS_SECRET_ACCESS_KEY: ${{ secrets.R2_SECRET_KEY }}
          R2_ACCOUNT_ID: ${{ secrets.R2_ACCOUNT_ID }}
        run: |
          aws s3 cp "$BACKUP_FILE" "s3://marine-backups/$BACKUP_FILE" \
            --endpoint-url "https://${R2_ACCOUNT_ID}.r2.cloudflarestorage.com"

      - name: Cleanup old backups (keep last 30)
        env:
          AWS_ACCESS_KEY_ID: ${{ secrets.R2_ACCESS_KEY }}
          AWS_SECRET_ACCESS_KEY: ${{ secrets.R2_SECRET_KEY }}
          R2_ACCOUNT_ID: ${{ secrets.R2_ACCOUNT_ID }}
        run: |
          aws s3api list-objects-v2 \
            --bucket marine-backups \
            --endpoint-url "https://${R2_ACCOUNT_ID}.r2.cloudflarestorage.com" \
            --query 'sort_by(Contents,&LastModified)[*].Key' \
            --output text | tr '\t' '\n' | head -n -30 | \
            xargs -I {} aws s3 rm "s3://marine-backups/{}" \
            --endpoint-url "https://${R2_ACCOUNT_ID}.r2.cloudflarestorage.com"
```

### 10.2 Restore testi

İlk backup düştükten sonra **test et:**

```bash
aws s3 cp "s3://marine-backups/backup_YYYYMMDD_HHMMSS.dump" ./backup.dump \
  --endpoint-url "https://${R2_ACCOUNT_ID}.r2.cloudflarestorage.com"

createdb marine_restore_test
pg_restore --no-owner --no-acl --dbname=marine_restore_test backup.dump

psql marine_restore_test -c "SELECT COUNT(*) FROM users; SELECT COUNT(*) FROM financial_entries;"
```

**Test edilmemiş backup, backup değildir.**

---

## 11. Sentry Entegrasyonu

### 11.1 Backend (Spring Boot)

`pom.xml`'e ekle:
```xml
<dependency>
    <groupId>io.sentry</groupId>
    <artifactId>sentry-spring-boot-starter-jakarta</artifactId>
    <version>7.14.0</version>
</dependency>
```

`application-prod.properties`'e ekle:
```properties
sentry.dsn=${SENTRY_DSN}
sentry.environment=production
sentry.traces-sample-rate=0.1
sentry.send-default-pii=false
```

### 11.2 Frontend (Angular)

```bash
npm install --save @sentry/angular
```

`src/main.ts`:
```typescript
import * as Sentry from "@sentry/angular";
import { environment } from './environments/environment';

if (environment.production) {
  Sentry.init({
    dsn: "https://...@sentry.io/...",
    environment: "production",
    tracesSampleRate: 0.1,
    integrations: [Sentry.browserTracingIntegration()],
  });
}
```

---

## 12. Env-Var Envanteri

| Anahtar | Nereye konur? | Kaynak |
|---|---|---|
| `DATABASE_URL` | Render + GitHub Actions secret | Neon → connection string |
| `DATABASE_USERNAME` | Render | `neondb_owner` |
| `DATABASE_PASSWORD` | Render | Neon şifresi |
| `JWT_SECRET` | Render | `openssl rand -base64 64` |
| `SYSTEM_ADMIN_PASSWORD` | Render | Sen belirle |
| `R2_ACCOUNT_ID` | Render + GitHub Actions secret | Cloudflare R2 panel |
| `R2_ACCESS_KEY` | Render + GitHub Actions secret | Cloudflare R2 API token |
| `R2_SECRET_KEY` | Render + GitHub Actions secret | Cloudflare R2 API token |
| `R2_BUCKET` | Render | `marine-attachments` |
| `APP_R2_ENABLED` | Render | `false` (R2 kurulana kadar) |
| `MAIL_USERNAME` | Render | Brevo SMTP login |
| `MAIL_PASSWORD` | Render | Brevo SMTP key |
| `MAIL_FROM` | Render | `noreply@maritar.com` |
| `MAIL_ENABLED` | Render | `true` |
| `CORS_ALLOWED_ORIGINS` | Render | `https://maritar.com,https://www.maritar.com` |
| `SENTRY_DSN` | Render | Sentry projesi DSN |
| `COOKIE_SECURE` | Render | `true` |
| `RENDER_DEPLOY_HOOK_URL` | GitHub Actions secret | Render → Service → Settings → Deploy Hook |

**Asla repo'ya commit etme.** `.gitignore`'da `.env*`'in olduğunu doğrula.

---

## 13. Çıkış-Günü Smoke Test

Pilot kullanıcı davet etmeden önce manuel doğrulamalar (15 dk):

1. [ ] `https://maritar.com` açılıyor, login ekranı geliyor
2. [ ] Test hesabıyla login başarılı
3. [ ] Dashboard yükleniyor, API çağrıları 200 dönüyor
4. [ ] Yeni entry oluştur → kaydet
5. [ ] Approval flow: CREW oluşturdu, CAPTAIN onayladı
6. [ ] Payment kaydet, status PAID oldu
7. [ ] Logout sonrası DevTools → Application → localStorage boş
8. [ ] `https://api.maritar.com/actuator/health` → 200 + `{"status":"UP"}`
9. [ ] Bilerek 500 fırlat (örn. var olmayan ID'yi sil), Sentry'de göründü
10. [ ] Test mailini al (forgot password tetikle), Inbox'ta (Spam değil) göründü
11. [ ] Backup workflow manuel tetikle (`workflow_dispatch`), R2'de dosya göründü
12. [ ] UptimeRobot dashboard yeşil

Hepsi yeşil → pilot kullanıcılara giriş bilgileri gönder.

---

## 14. Upgrade Path — İlk gerçek revenue gelince

| Sıra | Yükseltme | Yeni maliyet | Tetikleyici |
|---|---|---|---|
| 1 | Cloudflare → custom email routing | $0 | Profesyonel `info@`, `support@` adresleri |
| 2 | Neon Free → **Neon Pro** | +$19/ay | DB 3 GB'a yaklaştı veya 7 günden uzun PITR gerekti |
| 3 | Render Free → **Render Starter** ($7/ay) | +$7/ay | Cold start kullanıcıyı yorduğunda veya uptime garantisi gerektiğinde |
| 4 | Brevo Free → **Brevo Lite** | +$9/ay | Günlük 300 mail yetmediğinde |
| 5 | Sentry Free → **Sentry Team** | +$26/ay | 5K event/ay yetmediğinde |
| 6 | UptimeRobot Free → **Better Stack** | +$25/ay | İncident management + status page gerektiğinde |
| 7 | Stripe entegrasyonu | %2.9 + 30¢ / işlem | İlk gerçek müşteri ödediği gün |
| 8 | Sözleşme/ToS/KVKK metni | Avukat $200-500 (tek seferlik) | Ticari müşteri gelmeden ÖNCE |

**Sıralı maliyet kümülatifi:**
- 1-2 yükseltme sonrası: ~$20/ay
- 1-4 yükseltme sonrası: ~$55/ay
- Tam ticari (1-6): ~$90/ay

---

## 15. Sık Karşılaşılan Sorunlar (Troubleshooting)

### Backend deploy başarılı ama health check 503 dönüyor
- Environment variables eksik veya yanlış → Render dashboard → Environment sekmesini kontrol et
- Neon bağlantısı timeout → `DATABASE_URL`'deki şifreyi kontrol et, resetlendiyse güncelle
- `application-prod.properties`'te env-var referansı yanlış → `${VAR_NAME}` syntaxını kontrol et
- Render loglarını izle: Service → Logs

### Render servisi uyanmıyor / çok yavaş
- Free tier cold start ~30 saniye normal
- UptimeRobot 5 dk'da bir ping atıyorsa uyumamalı
- RAM yetmiyorsa Render loglarında OOM hatası görünür → Starter'a geç

### Frontend açılıyor ama API çağrısı CORS hatası
- `CORS_ALLOWED_ORIGINS` env-var'ı set edilmedi → Render'a ekle + redeploy
- `environment.prod.ts`'deki `apiUrl` yanlış domain kullanıyor

### Mail gitmedi / Spam'e düştü
- DNS SPF/DKIM kayıtları henüz propagate olmadı → 1-24 saat bekle
- Brevo dashboard → Senders → "Verify" tıklanmadı

### Backup workflow başarısız
- `DATABASE_URL` secret GitHub Actions'a eksik (Render secret değil, ayrı ekle)
- R2 endpoint URL'inde Account ID hatası → `https://<ACCOUNT_ID>.r2.cloudflarestorage.com`

---

## 16. Bu Doküman Nasıl Kullanılır

- **İlk deploy:** Bölüm 3'teki "Yol Haritası"nı sırayla takip et, atlama.
- **Sonraki feature deploy'ları:** GitHub'a push → GitHub Actions tetikler → Render otomatik deploy eder.
- **Yeni secret eklenince:** Render dashboard + GitHub Actions + bu doküman aynı anda güncellenmeli.
- **Sorun çıktığında:** Önce Bölüm 15'e bak, sonra Sentry → Render logs sırasıyla.
- **Provider değiştirince:** Sadece ilgili bölümü güncelle, geri kalan mimari aynı kalır.

---


## 17. VPS Geçişi — Güncel Durum ve As-Built Kayıt (2026-07-13)

> Bu bölüm **as-built** kayıttır: sistemin bugün gerçekte nasıl kurulu olduğunu anlatır.
> Kurulum adımlarının tarihsel rehberi: yukarıdaki Bölüm 1–16.
> Açık işler için: `TODO.md` → "🟠 VPS Geçişi" bölümü.

### Genel Resim

İki ortam paralel yaşıyor:

| | Part 1 — Render/Neon (ESKİ, hâlâ canlı) | Part 2 — VPS (YENİ, hazır, trafik bekliyor) |
|---|---|---|
| Trafik | `api.maritar.com` DNS'i **burayı** gösteriyor | Henüz trafik yok |
| Backend | Render.com (free tier, cold start ~30 sn) | Hetzner CX22, Docker Compose |
| Veritabanı | Neon Postgres (Frankfurt) | VPS içinde postgres:16 konteyneri |
| Veri | Eski test verisi (taşınmayacak — **sıfırdan başlama kararı**, 2026-07-13) | Temiz DB + seed + SYSTEM tenant |
| CI deploy | ❌ Artık pipeline'dan güncelleme almıyor (12d1101 ile kesildi) | ✅ Her `git push origin main` otomatik deploy |

Cutover (DNS çevirme) yapıldığında Part 1 emekliye ayrılacak.

---

### Part 1 — Render/Neon Düzeni (mevcut canlı)

#### Mimari

```
Kullanıcı → Cloudflare (DNS/CDN/WAF)
             ├── maritar.com      → Cloudflare Pages (Angular dist)
             └── api.maritar.com  → Render.com (Spring Boot, Docker)
                                        └── Neon Postgres (Frankfurt, PITR 7 gün)
Yan servisler: R2 (attachments), Brevo (SMTP), Sentry, UptimeRobot
```

Detaylı kurulum adımları ve free-tier sınırları: yukarıdaki Bölüm 1–8.

#### İşleyiş (2026-07-12'ye kadar geçerli olan akış)

- **Deploy:** `git push main` → GitHub Actions: test → imaj build → `RENDER_DEPLOY_HOOK_URL` secret'ına `curl POST` → Render kendi build/deploy'unu yapar.
  `12d1101` commit'iyle bu adım kaldırıldı; Render artık son deploy edilen sürümde donmuş durumda.
- **Yedekleme:** `.github/workflows/backup.yml` — her gece 02:00 UTC, `pg_dump` (Neon) → gzip → R2 `db-backups/`, 30 günden eski dosyalar silinir.
  ⚠️ Bu workflow hâlâ **Neon'u** yedekliyor. Cutover'da VPS postgres'ine çevrilmeli (TODO V1) — yoksa canlı veri yedeksiz kalır.
- **İlgili GitHub Secrets:** `NEON_DATABASE_URL`, `RENDER_DEPLOY_HOOK_URL`, `R2_BACKUP_ACCESS_KEY`, `R2_BACKUP_SECRET_KEY`, `R2_BACKUP_BUCKET`, `R2_ACCOUNT_ID`.

#### Emeklilik planı (cutover sonrası — TODO V2)

1. Render servisini durdur (silme — 1 hafta rollback payı)
2. Neon'daki eski veri taşınmayacak (sıfırdan başlama kararı); 1 hafta sorunsuz geçince Neon projesi kapatılabilir
3. `NEON_DATABASE_URL` ve `RENDER_DEPLOY_HOOK_URL` secret'ları silinir

---

### Part 2 — VPS Düzeni (yeni, 2026-07-13 itibarıyla hazır)

#### Sunucu

| | |
|---|---|
| Sağlayıcı / plan | Hetzner CX22 (2 vCPU, 4 GB RAM, 40 GB disk) |
| OS | Ubuntu 24.04 LTS |
| IP / hostname | `91.98.37.251` / `maritar-prod` |
| Kullanıcı | `deploy` (docker grubunda, sudo'lu) |
| Docker | Engine 29.x + Compose v5.x |
| Çalışma dizini | `/opt/maritar` |

`/opt/maritar` içeriği:

- `docker-compose.yml` — **CI tarafından senkronlanır**, elle düzenleme (bir sonraki deploy ezer); kaynak: repo `deploy/docker-compose.yml`
- `Caddyfile` — aynı şekilde CI senkronlar; kaynak: repo `deploy/Caddyfile`
- `.env` — **elle kurulur, git'te ve CI'da YOKTUR** (aşağıya bak)

#### Konteyner stack'i

```
                    internet
                       │  (dışa açık yalnız 80/443)
                 ┌─────▼─────┐
                 │   caddy   │  caddy:2-alpine — TLS (Let's Encrypt), reverse proxy
                 └─────┬─────┘
        frontend_network│
                 ┌─────▼─────┐
                 │  backend  │  ghcr.io/belmuh/marine-management-system:latest
                 └─────┬─────┘  JVM: -Xmx1280m, SerialGC · limit 2 GB
        backend_network │       healthcheck: curl → /actuator/health (start_period 90s)
                 ┌─────▼─────┐
                 │ postgres  │  postgres:16-alpine · limit 768 MB
                 └───────────┘  veri: `pgdata` Docker volume
```

Ağ segmentasyonu kasıtlı: Caddy, DB'ye erişemez (lateral movement engeli).
Postgres ve backend host'a port açmaz — dışarıdan yalnız 80/443 görünür (doğrulandı: `ss -tlnp`).

#### `.env` yönetimi

- Şablon: repo kökündeki `.env.example`
- Prod dosyası local'de `.env.prod.local` adıyla tutulur (`.gitignore`'daki `.env.*.local` kalıbına uyar, commit edilemez)
- Sunucuya gönderme: `scp .env.prod.local deploy@91.98.37.251:/opt/maritar/.env` + `chmod 600`
- Env değişikliği sonrası **restart yetmez**: `docker compose up -d backend` (recreate gerekir)
- Prod secret'ları dev'den ayrı üretildi (DB şifresi, `JWT_SECRET`, `SYSTEM_ADMIN_PASSWORD`) — değerler `.env.prod.local`'de + şifre yöneticisinde
- Superadmin: `superadmin@marine.com` (şifre `.env.prod.local` → `SYSTEM_ADMIN_PASSWORD`)

**Öğrenilen ders (2026-07-13):** Spring prod profili `SENTRY_DSN` (büyük harf, env formatı) bekler; dev dosyasındaki `sentry.dsn=` satırı bu ihtiyacı karşılamaz. Eksikti → backend açılışta crash + restart döngüsü. Belirti: loglarda `Could not resolve placeholder 'SENTRY_DSN'`.

#### CI/CD zinciri (`.github/workflows/ci.yml`)

```
git push origin main
  → build-and-test    ./mvnw verify (Testcontainers dahil) — kırmızıysa zincir durur
  → docker-publish    imaj → GHCR: `latest` + commit SHA tag'i
  → deploy
      1. scp: deploy/docker-compose.yml + Caddyfile → /opt/maritar  (.env'e dokunmaz)
      2. ssh: docker compose pull backend && docker compose up -d
      3. caddy graceful reload (Caddyfile bind-mount olduğu için elle tetiklenir)
      4. docker image prune -f
```

- **GitHub Secrets:** `VPS_HOST` = 91.98.37.251, `VPS_SSH_KEY` = `~/.ssh/maritar_ci` private key'i
- SSH anahtarı CI'a özeldir (`maritar_ci`, ed25519); kişisel anahtardan bağımsız, gerekirse tek başına iptal edilir. Public yarısı sunucuda `deploy` kullanıcısının `authorized_keys`'inde.
- İmaj çekme GHCR üzerinden: GitHub → GHCR (`GITHUB_TOKEN` ile push) ← VPS (`docker compose pull`)

#### Doğrulama komutları (deploy sonrası ~2 dk)

```bash
ssh deploy@91.98.37.251 "cd /opt/maritar && docker compose ps"
# backend: Up (healthy) — ilk açılışta 90 sn'ye kadar 'health: starting' normal

docker inspect --format='{{.State.Health.Status}}' $(docker ps -qf name=backend)   # healthy
docker network ls | grep maritar        # maritar_frontend_network + maritar_backend_network
ss -tlnp | grep -E ':(80|443|5432|8080)\b'   # yalnız 80/443
docker compose exec backend curl -s localhost:8080/actuator/health   # {"status":"UP"}
docker compose logs backend --tail 30   # crash şüphesinde ilk bakılacak yer
```

#### Bilinen durumlar

- **Caddy TLS hataları normaldir** (cutover'a kadar): `api.maritar.com` DNS'i hâlâ Render'da olduğundan Let's Encrypt doğrulaması bu sunucuya ulaşamıyor. Caddy sessizce yeniden dener; DNS çevrilince sertifika kendiliğinden alınır (`deploy/Caddyfile` içindeki cutover notu).
- **`latest` tag riski:** deploy `latest` çeker; rollback için SHA tag'ine geçilecek (TODO V3).
- **VPS DB'sini yedekleyen henüz hiçbir şey yok** — canlıya almadan önce kapatılmalı (TODO V1).

#### Zaman çizelgesi (nasıl buraya geldik)

| Tarih | Olay |
|---|---|
| 2026-07-10/11 | VPS hazırlık: sunucu, deploy kullanıcısı, Docker; compose+Caddyfile ilk kez elle kopyalandı |
| 2026-07-12 | `3b82f36`: healthcheck wget→curl (temurin-jre'de wget yok — sessiz bug) + ağ segmentasyonu |
| 2026-07-12 | `12d1101`: CI deploy hedefi Render→VPS + config senkron adımı |
| 2026-07-12 | CI #36: secrets eksik → fail; `VPS_HOST`/`VPS_SSH_KEY` + `.env` kuruldu → re-run yeşil |
| 2026-07-13 | `SENTRY_DSN` düzeltmesi → backend healthy, stack uçtan uca doğrulandı |
| 2026-07-13 | Karar: Neon verisi taşınmayacak, VPS sıfırdan başlayacak |
