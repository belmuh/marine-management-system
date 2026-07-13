# Canlıya Alma — Mevcut Durum ve Kurulum Kaydı

> Son güncelleme: 2026-07-13
> Bu doküman **as-built** kayıttır: sistemin bugün gerçekte nasıl kurulu olduğunu anlatır.
> Kurulum adımlarının tarihsel rehberi için: `DEPLOYMENT_GUIDE.md` (Render dönemi, arşiv niteliğinde).
> Açık işler için: `TODO.md` → "🟠 VPS Geçişi" bölümü.

## Genel Resim

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

## Part 1 — Render/Neon Düzeni (mevcut canlı)

### Mimari

```
Kullanıcı → Cloudflare (DNS/CDN/WAF)
             ├── maritar.com      → Cloudflare Pages (Angular dist)
             └── api.maritar.com  → Render.com (Spring Boot, Docker)
                                        └── Neon Postgres (Frankfurt, PITR 7 gün)
Yan servisler: R2 (attachments), Brevo (SMTP), Sentry, UptimeRobot
```

Detaylı kurulum adımları ve free-tier sınırları: `DEPLOYMENT_GUIDE.md` Bölüm 1–8.

### İşleyiş (2026-07-12'ye kadar geçerli olan akış)

- **Deploy:** `git push main` → GitHub Actions: test → imaj build → `RENDER_DEPLOY_HOOK_URL` secret'ına `curl POST` → Render kendi build/deploy'unu yapar.
  `12d1101` commit'iyle bu adım kaldırıldı; Render artık son deploy edilen sürümde donmuş durumda.
- **Yedekleme:** `.github/workflows/backup.yml` — her gece 02:00 UTC, `pg_dump` (Neon) → gzip → R2 `db-backups/`, 30 günden eski dosyalar silinir.
  ⚠️ Bu workflow hâlâ **Neon'u** yedekliyor. Cutover'da VPS postgres'ine çevrilmeli (TODO V1) — yoksa canlı veri yedeksiz kalır.
- **İlgili GitHub Secrets:** `NEON_DATABASE_URL`, `RENDER_DEPLOY_HOOK_URL`, `R2_BACKUP_ACCESS_KEY`, `R2_BACKUP_SECRET_KEY`, `R2_BACKUP_BUCKET`, `R2_ACCOUNT_ID`.

### Emeklilik planı (cutover sonrası — TODO V2)

1. Render servisini durdur (silme — 1 hafta rollback payı)
2. Neon'daki eski veri taşınmayacak (sıfırdan başlama kararı); 1 hafta sorunsuz geçince Neon projesi kapatılabilir
3. `NEON_DATABASE_URL` ve `RENDER_DEPLOY_HOOK_URL` secret'ları silinir

---

## Part 2 — VPS Düzeni (yeni, 2026-07-13 itibarıyla hazır)

### Sunucu

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

### Konteyner stack'i

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

### `.env` yönetimi

- Şablon: repo kökündeki `.env.example`
- Prod dosyası local'de `.env.prod.local` adıyla tutulur (`.gitignore`'daki `.env.*.local` kalıbına uyar, commit edilemez)
- Sunucuya gönderme: `scp .env.prod.local deploy@91.98.37.251:/opt/maritar/.env` + `chmod 600`
- Env değişikliği sonrası **restart yetmez**: `docker compose up -d backend` (recreate gerekir)
- Prod secret'ları dev'den ayrı üretildi (DB şifresi, `JWT_SECRET`, `SYSTEM_ADMIN_PASSWORD`) — değerler `.env.prod.local`'de + şifre yöneticisinde
- Superadmin: `superadmin@marine.com` (şifre `.env.prod.local` → `SYSTEM_ADMIN_PASSWORD`)

**Öğrenilen ders (2026-07-13):** Spring prod profili `SENTRY_DSN` (büyük harf, env formatı) bekler; dev dosyasındaki `sentry.dsn=` satırı bu ihtiyacı karşılamaz. Eksikti → backend açılışta crash + restart döngüsü. Belirti: loglarda `Could not resolve placeholder 'SENTRY_DSN'`.

### CI/CD zinciri (`.github/workflows/ci.yml`)

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

### Doğrulama komutları (deploy sonrası ~2 dk)

```bash
ssh deploy@91.98.37.251 "cd /opt/maritar && docker compose ps"
# backend: Up (healthy) — ilk açılışta 90 sn'ye kadar 'health: starting' normal

docker inspect --format='{{.State.Health.Status}}' $(docker ps -qf name=backend)   # healthy
docker network ls | grep maritar        # maritar_frontend_network + maritar_backend_network
ss -tlnp | grep -E ':(80|443|5432|8080)\b'   # yalnız 80/443
docker compose exec backend curl -s localhost:8080/actuator/health   # {"status":"UP"}
docker compose logs backend --tail 30   # crash şüphesinde ilk bakılacak yer
```

### Bilinen durumlar

- **Caddy TLS hataları normaldir** (cutover'a kadar): `api.maritar.com` DNS'i hâlâ Render'da olduğundan Let's Encrypt doğrulaması bu sunucuya ulaşamıyor. Caddy sessizce yeniden dener; DNS çevrilince sertifika kendiliğinden alınır (`deploy/Caddyfile` içindeki cutover notu).
- **`latest` tag riski:** deploy `latest` çeker; rollback için SHA tag'ine geçilecek (TODO V3).
- **VPS DB'sini yedekleyen henüz hiçbir şey yok** — canlıya almadan önce kapatılmalı (TODO V1).

### Zaman çizelgesi (nasıl buraya geldik)

| Tarih | Olay |
|---|---|
| 2026-07-10/11 | VPS hazırlık: sunucu, deploy kullanıcısı, Docker; compose+Caddyfile ilk kez elle kopyalandı |
| 2026-07-12 | `3b82f36`: healthcheck wget→curl (temurin-jre'de wget yok — sessiz bug) + ağ segmentasyonu |
| 2026-07-12 | `12d1101`: CI deploy hedefi Render→VPS + config senkron adımı |
| 2026-07-12 | CI #36: secrets eksik → fail; `VPS_HOST`/`VPS_SSH_KEY` + `.env` kuruldu → re-run yeşil |
| 2026-07-13 | `SENTRY_DSN` düzeltmesi → backend healthy, stack uçtan uca doğrulandı |
| 2026-07-13 | Karar: Neon verisi taşınmayacak, VPS sıfırdan başlayacak |
