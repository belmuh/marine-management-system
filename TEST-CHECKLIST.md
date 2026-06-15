# Test Checklist — Düzeltme Doğrulama
DEPLOY-REVIEW.md'deki bulgular ve uygulanan düzeltmeler için manuel test listesi.
Her maddeyi `[ ]` → `[x]` işaretleyerek ilerleyin.

---

## 1. Income Filtre Düzeltmesi (UYGULANDI — income-list.store.ts)

> Kök neden: EntryService root singleton; Expense sayfasının filtreleri Income'a taşınıyordu.

- [ ] **1.1 Miras filtre senaryosu (asıl bug):** Expense sayfasında tarih aralığı + arama terimi + kategori filtresi uygula → Income sayfasına geç → liste filtresiz ve eksiksiz gelmeli.
- [ ] **1.2 Anlık güncelleme:** Income'da yeni kayıt oluştur → modal kapandığında kayıt listede hard refresh olmadan görünmeli.
- [ ] **1.3 Silme:** Income'da kayıt sil → listeden anında düşmeli.
- [ ] **1.4 Regresyon — Expense:** Income → Expense geçişi → expense listesi kendi filtreleriyle normal çalışmalı.
- [ ] **1.5 Regresyon — gidip gelme:** Income → Expense → Income art arda 3 kez → her seferinde doğru liste, konsol hatası yok.
- [ ] **1.6 Sayfalama:** Expense'te 2. sayfaya git → Income'a geç → Income 1. sayfadan başlamalı.

## 2. W5 — Interceptor Kuyruğu (HENÜZ UYGULANMADI)

> Uygulandıktan sonra test edin.

- [ ] **2.1 Başarısız refresh:** DevTools → Application → localStorage'daki refresh token'ı boz → birden çok istek atan bir sayfa aç (ör. dashboard) → tüm istekler sonlanmalı (asılı spinner kalmamalı) ve login'e yönlenmeli.
- [ ] **2.2 Tekrar login:** 2.1 sonrası yeniden login ol → uygulama normal çalışmalı (kuyruk mekanizması ölmemiş olmalı — BehaviorSubject tuzağı kontrolü).
- [ ] **2.3 Başarılı refresh:** Access token süresini bekle (veya kısalt) → herhangi bir sayfada işlem yap → istek otomatik refresh ile başarılı olmalı, kullanıcı hiçbir şey fark etmemeli.

## 3. W3 — /auth/me Token Düzeltmesi (HENÜZ UYGULANMADI)

- [ ] **3.1** Login ol → Network sekmesinde `/auth/me` çağrısını izle → **tek istekte 200** dönmeli (önceki davranış: 401 → refresh → retry).
- [ ] **3.2** Rol değişikliği sonrası header'daki kullanıcı bilgisi güncellenebilmeli (`refreshCurrentUser` akışı).
- [ ] **3.3 Regresyon:** Login, logout, register, forgot/reset password akışları çalışmaya devam etmeli (skip listesi daraltıldı).

## 4. W1 — Excel Import Sequence (HENÜZ UYGULANMADI)

- [ ] **4.1** 5+ satırlık Excel import et → tüm satırlar farklı entry numarası almalı.
- [ ] **4.2 Asıl bug:** Import'tan hemen sonra manuel yeni kayıt oluştur → **hata almadan**, import'takilerle çakışmayan yeni numara almalı.
- [ ] **4.3** İki import art arda → numaralar devam etmeli, duplicate olmamalı.

## 5. Backend Config Düzeltmeleri (HENÜZ UYGULANMADI)

- [ ] **5.1 CORS:** Prod benzeri ortamda `CORS_ALLOWED_ORIGINS` env ile başlat → frontend origin'inden istek başarılı, başka origin'den preflight reddi.
- [ ] **5.2 R2:** Prod profilde dosya yükle → R2 bucket'ında dosya **gerçekten var mı** kontrol et (sessiz no-op bug'ı). İndirme presigned URL ile çalışmalı.
- [ ] **5.3 Scheduling:** `@EnableScheduling` sonrası loglarda gece 02:00 cleanup job'ının koştuğunu doğrula (test için cron'u geçici kısaltabilirsiniz).
- [ ] **5.4 JWT filter:** Geçersiz/bozuk token ile istek at → response'ta iç exception detayı **görünmemeli**, generic mesaj dönmeli.

## 6. Genel Regresyon (her şey bittikten sonra bir tur)

- [ ] **6.1** Login → dashboard → income oluştur → expense oluştur → onay akışı → ödeme kaydet → raporlar — uçtan uca tek tur.
- [ ] **6.2** İki farklı organizasyon hesabıyla login olup birinin verisinin diğerinde görünmediğini doğrula (tenant izolasyonu).
- [ ] **6.3** "Beni hatırla" ile login → tarayıcıyı kapat/aç → oturum devam etmeli; işaretsiz login → tarayıcı kapatınca oturum düşmeli.
- [ ] **6.4** Tarayıcı konsolu: kritik akışlarda kırmızı hata olmamalı.

---

**Geri alma komutları** (bir düzeltme sorun çıkarırsa):
- Frontend: `git checkout -- <dosya>` veya `git diff` ile incele
- Backend: aynı şekilde; tüm düzeltmeler ayrı commit'lerde tutulursa `git revert <commit>` yeterli
