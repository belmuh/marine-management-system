# Marine Management System — Deployment Guide & Yol Haritası

> Pilot canlıya alma için kanonik referans dokümanı.
> Bu doküman okuyan birinin hiçbir başka yere bakmadan A'dan Z'ye deploy yapabilmesi için yazıldı.
> Tarih: 2026-05-06
> Varsayım: Domain alındı (örnek olarak `maritar.com` kullanılacak; sen kendi domain'inle değiştir).

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
 │ Cloudflare   │             │   Fly.io         │
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
| Backend hosting | Fly.io | https://fly.io | 3× shared-cpu (256 MB RAM) | $0 |
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

---

## 3. Yol Haritası — Kronolojik Akış

Toplam ~5 iş günü. Sırayla yap, atlayama, çünkü birbirini bekleyen adımlar var.

### Gün 0 — Hazırlık (1-2 saat)
- [ ] GitHub'da private repo'lar var mı kontrol et (backend + frontend)
- [ ] Yerel makinede Docker Desktop kurulu mu doğrula (`docker --version`)
- [ ] Tek bir password manager'da tüm credential'ları toplayacak boş bir bölüm aç (1Password, Bitwarden, KeePassXC)
- [ ] Bir şifre yöneticisinde "Marine Production" diye bir vault aç

### Gün 1 — Hesaplar ve domain (yarım gün)
- [ ] Cloudflare hesabı aç → domain satın al (Bölüm 4.1)
- [ ] Neon hesabı aç → production database oluştur (Bölüm 4.2)
- [ ] Fly.io hesabı aç → CLI kur (Bölüm 4.3)
- [ ] Brevo hesabı aç → sender email doğrula (Bölüm 4.4)
- [ ] Sentry hesabı aç → 2 proje oluştur (backend + frontend) (Bölüm 4.5)
- [ ] UptimeRobot hesabı aç (Bölüm 4.6)
- [ ] Cloudflare R2 → 2 bucket oluştur (`marine-attachments` + `marine-backups`) (Bölüm 4.7)

### Gün 2 — Backend Docker + Fly.io (1 gün)
- [ ] `Dockerfile` ekle (Bölüm 5)
- [ ] `.dockerignore` ekle
- [ ] Lokal `docker build` + `docker run` ile test
- [ ] `fly.toml` oluştur (Bölüm 6.1)
- [ ] Fly.io'ya secret'ları yükle (Bölüm 6.2)
- [ ] İlk deploy: `flyctl deploy`
- [ ] Health check doğrula: `curl https://marine-backend.fly.dev/actuator/health`

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
- [ ] `LAUNCH_CHECKLIST.md`'deki çıkış-günü smoke testlerini koş
- [ ] İlk pilot tenant manuel oluştur (DB'de veya `/api/onboarding/register` üzerinden)
- [ ] Pilot kullanıcıya giriş bilgileri güvenli kanaldan gönder
- [ ] İlk hafta için günlük 30 dk Sentry kontrol slot'u takvimine ekle

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
4. Auto-renew → açık tut (yoksa unutursan kaybedersin)

**DNS'i Cloudflare'de kullan:**
Cloudflare Registrar'dan aldıysan zaten otomatik Cloudflare DNS kullanıyor — ek iş yok. Başka registrar'dan aldıysan name server'ları Cloudflare'inkiyle değiştir.

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
1. https://console.neon.tech/signup → GitHub ile giriş (en kolayı)
2. **Create project**:
   - Project name: `marine-production`
   - Postgres version: 16 (veya en güncel stable)
   - Region: kullanıcılarına en yakın (Avrupa için Frankfurt)
3. Connection string'i kopyala (formato: `postgresql://user:pass@host/db?sslmode=require`)

**PITR'ı aktif et:**
1. Project → **Settings → Storage**
2. **Point-in-time restore** → enable, retention 7 gün

**Önemli notlar:**
- Connection string'i **password manager'a kaydet**, repo'ya asla commit etme
- Neon free tier 5 dakika idle'dan sonra compute'u suspend eder ama **DB veri kaybolmaz**, ilk istekte birkaç saniye gecikme olur (cold start). Pilot için kabul edilebilir.
- DB boyutu 3 GB sınırına yaklaşırsa Neon Pro'ya ($19/ay) geç.

---

### 4.3 Fly.io — Backend Hosting

**Hesap aç:**
1. https://fly.io/app/sign-up
2. Kredi kartı istenir (free tier'da kullanım için bile, abuse koruması). Free tier limiti aşmadıkça **0 ücretlendirme**.

**CLI kur (macOS):**
```bash
brew install flyctl
flyctl auth login
```

**Lokal makinede oturum kontrolü:**
```bash
flyctl auth whoami
```

---

### 4.4 Brevo — E-posta gönderimi

**Hesap aç:**
1. https://onboarding.brevo.com/account/register
2. **Sender → Senders & IP → Add a Sender**:
   - From name: `Marine System`
   - From email: `noreply@maritar.com` (henüz domain DNS hazır olmasa bile sırayla yap)
3. **SMTP & API → SMTP**:
   - Login (SMTP key) ve relay host adresini al → password manager'a kaydet
4. Daha sonra (Gün 3'te DNS hazır olunca):
   - **Senders → Authenticate your domain**
   - Brevo sana SPF + DKIM + DMARC için TXT record'ları verir
   - Cloudflare DNS'e bu kayıtları ekle (Bölüm 8)

---

### 4.5 Sentry — Error tracking

**Hesap aç:**
1. https://sentry.io/signup/ → GitHub ile
2. Organization name: `marine-system`
3. **Create project**: `marine-backend`, platform → Java / Spring Boot. DSN'i kopyala.
4. **Create project**: `marine-frontend`, platform → Angular. DSN'i kopyala.
5. Her iki DSN'i de password manager'a kaydet

---

### 4.6 UptimeRobot — Uptime monitoring

**Hesap aç:**
1. https://uptimerobot.com/signUp
2. Free plan, 50 monitor / 5 dakika aralık
3. Daha sonra (deploy bittikten sonra) **Add New Monitor**:
   - Type: HTTP(s)
   - URL: `https://api.maritar.com/actuator/health`
   - Interval: 5 minutes
   - Alert contacts: e-posta (kendi adresin)
4. Bir tane de frontend için: `https://maritar.com`

---

### 4.7 R2 (Cloudflare) — özet

Bu zaten Bölüm 4.1'de hallolduysa burada tekrar etmeye gerek yok. Sadece şunu doğrula:
- Account ID elinde
- Access Key ID + Secret Access Key elinde
- Endpoint URL formatı: `https://<account_id>.r2.cloudflarestorage.com`

---

## 5. Backend Dockerization

### 5.1 `Dockerfile`

Bu dosyayı projenin köküne ekle (`pom.xml`'in yanına):

```dockerfile
# Multi-stage build: küçük final image
# Stage 1: Build
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app
COPY mvnw mvnw.cmd pom.xml ./
COPY .mvn .mvn
RUN ./mvnw dependency:go-offline -B

COPY src src
RUN ./mvnw clean package -DskipTests -B

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine

# Non-root user (security best practice)
RUN addgroup -S marine && adduser -S marine -G marine

WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
RUN chown -R marine:marine /app

USER marine

# Healthcheck
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

EXPOSE 8080

# JVM tuning for low-memory containers (Fly.io 256 MB)
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
# Build
docker build -t marine-backend:local .

# Run (env değişkenlerini bir .env dosyasına koyarak)
docker run --rm -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=dev \
  -e DATABASE_URL="..." \
  -e JWT_SECRET="..." \
  marine-backend:local

# Test
curl http://localhost:8080/actuator/health
```

`{"status":"UP"}` dönerse başarılı.

---

## 6. Fly.io Backend Deploy

### 6.1 `fly.toml`

Projenin köküne ekle:

```toml
app = "marine-backend"
primary_region = "fra"  # Frankfurt — Avrupa kullanıcıları için

[build]
  dockerfile = "Dockerfile"

[env]
  SPRING_PROFILES_ACTIVE = "prod"
  PORT = "8080"
  SERVER_PORT = "8080"

[http_service]
  internal_port = 8080
  force_https = true
  auto_stop_machines = "stop"      # idle'da durdur (free tier)
  auto_start_machines = true       # istek gelince başlat
  min_machines_running = 0
  processes = ["app"]

  [http_service.concurrency]
    type = "requests"
    hard_limit = 200
    soft_limit = 100

  [[http_service.checks]]
    interval = "30s"
    timeout = "5s"
    grace_period = "60s"
    method = "GET"
    path = "/actuator/health"

[[vm]]
  cpu_kind = "shared"
  cpus = 1
  memory_mb = 256  # Free tier limiti
```

**Not:** `auto_stop_machines = "stop"` ücretsiz tier'da kritik. Trafik yoksa makine durur, fatura işlemez. İstek gelince ~5 saniyede uyanır. Pilot kullanım için kabul edilebilir.

### 6.2 İlk deploy

```bash
# Projeyi Fly'a tanıt (sadece ilk seferde)
flyctl launch --no-deploy --copy-config --name marine-backend --region fra

# Secret'ları yükle (production env-var'ları)
flyctl secrets set \
  DATABASE_URL="postgresql://..." \
  JWT_SECRET="$(openssl rand -base64 64)" \
  SYSTEM_ADMIN_PASSWORD="..." \
  R2_ACCOUNT_ID="..." \
  R2_ACCESS_KEY="..." \
  R2_SECRET_KEY="..." \
  R2_BUCKET="marine-attachments" \
  MAIL_USERNAME="..." \
  MAIL_PASSWORD="..." \
  MAIL_FROM="noreply@maritar.com" \
  CORS_ALLOWED_ORIGINS="https://maritar.com" \
  SENTRY_DSN="https://...@sentry.io/..." \
  --app marine-backend

# Deploy
flyctl deploy --app marine-backend

# Doğrula
flyctl status --app marine-backend
curl https://marine-backend.fly.dev/actuator/health
```

### 6.3 Log izleme

```bash
flyctl logs --app marine-backend
flyctl logs --app marine-backend --since 10m
```

---

## 7. Frontend Cloudflare Pages Deploy

### 7.1 Repo bağla

1. Cloudflare dashboard → **Workers & Pages → Create → Pages → Connect to Git**
2. GitHub'a yetki ver, frontend repo'sunu seç
3. Build configuration:
   - **Framework preset**: Angular
   - **Build command**: `npm run build`
   - **Build output directory**: `dist/marine-managment-angular/browser`
   - **Root directory**: (boş bırak)
   - **Environment variables**: gerekirse `NODE_VERSION=20`

### 7.2 `environment.prod.ts` güncelle

```typescript
export const environment = {
  production: true,
  apiUrl: 'https://api.maritar.com/api',  // ← gerçek backend URL'in
  endpoints: { /* mevcut yapıyla aynı */ }
};
```

Bu dosyayı commit + push et. Cloudflare Pages otomatik build alır.

### 7.3 Custom domain bağla

1. Cloudflare Pages projesi → **Custom domains → Set up a custom domain**
2. `maritar.com` ve `www.maritar.com` ekle
3. DNS otomatik kurulur (zaten Cloudflare'desin)

---

## 8. DNS Konfigürasyonu

Cloudflare DNS panelinde şu kayıtları kur. (Cloudflare → seçilen domain → DNS → Records)

| Type | Name | Content | Proxy | Açıklama |
|---|---|---|---|---|
| `CNAME` | `@` | `marine-frontend.pages.dev` | 🟠 Proxied | Frontend |
| `CNAME` | `www` | `marine-frontend.pages.dev` | 🟠 Proxied | www → ana domain |
| `CNAME` | `api` | `marine-backend.fly.dev` | 🟠 Proxied | Backend API |
| `TXT` | `@` | (Brevo'nun verdiği SPF değeri) | — | E-posta SPF |
| `TXT` | `mail._domainkey` | (Brevo DKIM değeri) | — | E-posta DKIM |
| `TXT` | `_dmarc` | `v=DMARC1; p=quarantine; rua=mailto:dmarc@maritar.com` | — | DMARC |
| `MX` | `@` | (yalnızca e-posta alacaksan) | — | İnbound mail (atla) |

**Önemli:**
- Proxy 🟠 (turuncu bulut) açık olduğunda Cloudflare CDN/WAF/SSL devreye girer. Backend için açık olması ideal — DDoS koruması ve TLS termination Cloudflare'de yapılır.
- Brevo SPF/DKIM kayıtları Brevo panelinde "Authenticate your domain" altında. DNS kaydı sonrasında "Verify" butonuna basıp doğrulanmasını bekle.

---

## 9. CI/CD — GitHub Actions

### 9.1 Backend deploy workflow

`backend repo`'sunda `.github/workflows/deploy.yml`:

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
      - uses: actions/checkout@v4

      - uses: superfly/flyctl-actions/setup-flyctl@master

      - name: Deploy to Fly.io
        run: flyctl deploy --remote-only
        env:
          FLY_API_TOKEN: ${{ secrets.FLY_API_TOKEN }}
```

GitHub repo → **Settings → Secrets → Actions → New repository secret**:
- Name: `FLY_API_TOKEN`
- Value: `flyctl auth token` komutu ile alınan token

### 9.2 Frontend deploy

Cloudflare Pages otomatik git push'a tepki verir, ekstra workflow gerekmez.

---

## 10. Backup Otomasyonu

### 10.1 GitHub Actions cron — günlük pg_dump → R2

Backend repo'sunda `.github/workflows/backup.yml`:

```yaml
name: Daily DB Backup

on:
  schedule:
    - cron: "0 3 * * *"   # Her gün UTC 03:00 (TR saatiyle 06:00)
  workflow_dispatch:       # Manuel tetikleme

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
# Lokalde test DB'ye restore
aws s3 cp "s3://marine-backups/backup_YYYYMMDD_HHMMSS.dump" ./backup.dump \
  --endpoint-url "https://${R2_ACCOUNT_ID}.r2.cloudflarestorage.com"

createdb marine_restore_test
pg_restore --no-owner --no-acl --dbname=marine_restore_test backup.dump

# Veriler doğru mu kontrol
psql marine_restore_test -c "SELECT COUNT(*) FROM users; SELECT COUNT(*) FROM financial_entries;"
```

**Test edilmemiş backup, backup değildir.** Bunu unutma.

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

`GlobalExceptionHandler.handleGenericException` içinde errorId'yi Sentry'ye tag olarak ekle:
```java
import io.sentry.Sentry;
// ...
Sentry.configureScope(scope -> scope.setTag("errorId", errorId));
Sentry.captureException(ex);
```

### 11.2 Frontend (Angular)

```bash
npm install --save @sentry/angular
```

`src/main.ts` (bootstrap'ten önce):
```typescript
import * as Sentry from "@sentry/angular";
import { environment } from './environments/environment';

if (environment.production) {
  Sentry.init({
    dsn: "https://...@sentry.io/...",  // env-var'a koyman daha iyi
    environment: "production",
    tracesSampleRate: 0.1,
    integrations: [Sentry.browserTracingIntegration()],
  });
}
```

`error.interceptor.ts`'e errorId'yi Sentry'ye gönder:
```typescript
import * as Sentry from "@sentry/angular";
// ...
Sentry.captureException(error, { tags: { errorId: error.errorId } });
```

---

## 12. Env-Var Envanteri

Tek bir tabloda tüm secret'lar. Bunu password manager'da bu tabloyu yansıtan bir entry olarak tut.

| Anahtar | Nereye konur? | Kaynak |
|---|---|---|
| `DATABASE_URL` | Fly.io secret + GitHub Actions secret | Neon → connection string |
| `JWT_SECRET` | Fly.io secret | `openssl rand -base64 64` ile lokal üret |
| `SYSTEM_ADMIN_PASSWORD` | Fly.io secret | Güçlü şifre, sen üret |
| `R2_ACCOUNT_ID` | Fly.io + GitHub Actions secret | Cloudflare R2 panel |
| `R2_ACCESS_KEY` | Fly.io + GitHub Actions secret | Cloudflare R2 API token |
| `R2_SECRET_KEY` | Fly.io + GitHub Actions secret | Cloudflare R2 API token |
| `R2_BUCKET` | Fly.io secret | `marine-attachments` |
| `MAIL_USERNAME` | Fly.io secret | Brevo SMTP login |
| `MAIL_PASSWORD` | Fly.io secret | Brevo SMTP key |
| `MAIL_FROM` | Fly.io secret | `noreply@maritar.com` |
| `MAIL_ENABLED` | Fly.io secret | `true` |
| `CORS_ALLOWED_ORIGINS` | Fly.io secret | `https://maritar.com,https://www.maritar.com` |
| `SENTRY_DSN` | Fly.io secret | Sentry projesi DSN |
| `FLY_API_TOKEN` | GitHub Actions secret | `flyctl auth token` |

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

Bu listeyi sırayla aktif et. Her adım bir önceki sıkıntıyı çözünce gelsin, peşin yapma.

| Sıra | Yükseltme | Yeni maliyet | Tetikleyici |
|---|---|---|---|
| 1 | Cloudflare → custom email routing veya Workers Email | $0 | Profesyonel `info@`, `support@` adresleri |
| 2 | Neon Free → **Neon Pro** | +$19/ay | DB 3 GB'a yaklaştı veya 7 günden uzun PITR gerekti |
| 3 | Fly.io free → **Fly.io paid** (1 GB RAM, dedicated CPU) | +$15-25/ay | Cold start kullanıcıyı yorduğunda veya RAM yetmediğinde |
| 4 | Brevo Free → **Brevo Lite** | +$9/ay | Günlük 300 mail yetmediğinde |
| 5 | Sentry Free → **Sentry Team** | +$26/ay | 5K event/ay yetmediğinde veya 90+ gün retention gerektiğinde |
| 6 | UptimeRobot Free → **Better Stack** | +$25/ay | İncident management + status page gerektiğinde |
| 7 | Stripe entegrasyonu | %2.9 + 30¢ / işlem | İlk gerçek müşteri ödediği gün |
| 8 | Sözleşme/ToS/KVKK metni | Avukat $200-500 (tek seferlik) | Pilotu aşıp ticari müşteri gelmeden ÖNCE |

**Sıralı maliyet kümülatifi:**
- 1-2 yükseltme sonrası: ~$20/ay
- 1-4 yükseltme sonrası: ~$50/ay
- Tam ticari (1-6): ~$100-130/ay

İlk müşteri $50/ay öderse 2. seviye çıktın. İki müşteri = 4. seviye. Bu boyutta operasyonel maliyet revenue'nun küçük bir yüzdesi olarak kalır.

---

## 15. Sık Karşılaşılan Sorunlar (Troubleshooting)

### Backend deploy başarılı ama health check 503 dönüyor
- DATABASE_URL secret yanlış set edildi → `flyctl secrets list` ile kontrol et
- Neon idle'dan kalkamadı → 30 saniye bekle, tekrar dene
- `application-prod.properties`'te env-var referansı yanlış → `${VAR_NAME:-default}` syntaxını kontrol et

### Frontend açılıyor ama API çağrısı CORS hatası
- Backend'in CORS_ALLOWED_ORIGINS env-var'ı set edilmedi → set + redeploy
- Hâlâ hard-coded localhost → `SecurityConfig.java`'yı `LAUNCH_CHECKLIST.md` 1. madde gereği düzelt

### Mail gitmedi / Spam'e düştü
- DNS SPF/DKIM kayıtları henüz propagate olmadı → 1-24 saat bekle
- Brevo dashboard → Senders → "Verify" tıklanmadı
- `from` adresi domain'le eşleşmiyor (`noreply@gmail.com` Brevo üzerinden gönderilemez)

### Fly.io machine sürekli yeniden başlıyor
- 256 MB RAM yetersiz → JVM `-Xmx128m` olarak sınırla, veya makineyi 512 MB'a çıkar (paid)
- `flyctl logs` ile OutOfMemoryError var mı kontrol

### Backup workflow başarısız
- `DATABASE_URL` secret eksik → GitHub Actions secret ekle (Fly.io secret değil!)
- R2 endpoint URL'inde Account ID hatası → `https://<ACCOUNT_ID>.r2.cloudflarestorage.com` formatı

---

## 16. Bu Doküman Nasıl Kullanılır

- **İlk deploy:** Bölüm 3'teki "Yol Haritası"nı sırayla takip et, atlama.
- **Sonraki feature deploy'ları:** GitHub'a push, otomatik. Manuel adım yok.
- **Yeni bileşen eklerken:** Bölüm 12'deki env-var envanterini güncel tut. Yeni secret eklenince hem Fly.io hem GitHub Actions hem password manager hem bu doküman güncellensin.
- **Sorun çıktığında:** Önce Bölüm 15'e bak. Yoksa Sentry → log → Fly.io logs sırasıyla aşağı in.
- **Provider değiştirirken (gelecek):** Sadece ilgili bölümü değiştir. Örn. Fly.io'dan Render'a geçince Bölüm 6'yı güncelle, gerisi aynı kalır.

---

## 17. Final Sözü

Bu setup, **yatırımı sıfıra yakın, taşınabilirliği maksimum, operasyonel yükü minimum** noktada dengelendi. Her bileşen kendi kategorisinde ya en iyisi ya da ikinci en iyisi; hiçbiri sana lock-in yapacak özellik kullanmıyor. Pilot başarılı olur ve gerçek müşteri gelirse Bölüm 14'teki sıralı upgrade yolu açık.

İlerleyen dönemde bu dokümanı güncel tut — provider URL'i, fiyat değişikliği, yeni secret eklenmesi, yeni adım — hepsini bu tek dosyaya yansıt. Repo köküne `git add DEPLOYMENT_GUIDE.md` ile commit edilirse takım büyüdüğünde de canlı bir yol haritası kalır.

İlk pilot canlıya çıkınca repo'da `git tag v0.1.0-pilot.1` koy, ilk milestone'unu işaretle. Bu tag bir gün senin için "burası başladığım yer"di anısı olur.
