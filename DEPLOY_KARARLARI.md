# Deploy Kararları — Maritar Projesi

## Seçilen Platform: Render (render.com)

- Fly.io ücretli olduğu için elendi
- Render **Free tier** seçildi (512MB RAM, $0)
- GitHub ile bağlandı, repo: `marine-management-system`
- Docker ile deploy — Java listede olmadığı için Dockerfile yazıldı
- Region: **Frankfurt (EU Central)** — Neon veritabanıyla aynı region

## Altyapı

| Bileşen | Servis | Notlar |
|---|---|---|
| Backend | Render (Free) | Spring Boot, Docker |
| Frontend | Angular | repo: `marine-managment-angular` |
| Veritabanı | Neon (PostgreSQL) | Frankfurt region |
| Domain | `maritar.com` | Cloudflare'de kayıtlı |
| Dosya depolama | Cloudflare R2 | Henüz aktive edilmedi |

## Kaldığımız Yer

- Render'da environment variables girişi ve deploy aşamasında durdu
- R2, airport WiFi'da ödeme sorunu yüzünden aktive edilemedi
- `.env`'e `APP_R2_ENABLED=false` eklendi → R2 olmadan deploy edilebilir (dosya yükleme çalışmaz, geri kalan sistem çalışır)
- **Sonraki adım:** Güvenli bir ağda Render'a env variable'ları gir ve deploy et

## Environment Variables (.env)

Doldurulması gerekenler:
- `DATABASE_URL` → Neon bağlantısı (dolu)
- `DATABASE_USERNAME` → `neondb_owner`
- `DATABASE_PASSWORD` → Neon'dan alınan şifre
- `JWT_SECRET` → üretildi (dolu)
- `SYSTEM_ADMIN_PASSWORD` → belirlendi
- `CORS_ALLOWED_ORIGINS` → `https://maritar.com`
- `R2_*` → Cloudflare R2 kurulunca eklenecek
- `APP_R2_ENABLED` → şimdilik `false`

## Notlar

- `.env` dosyası git'e commit edilmemeli (`.gitignore`'da var)
- Neon şifresi airport WiFi'da girildi → güvenli ağa geçince resetlenmeli
- SYSTEM_ADMIN_PASSWORD ilk login'de değiştirilmeli
