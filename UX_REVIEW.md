# Yeni Kullanıcı Akışı İncelemesi + Kayıt Sihirbazı Önerileri

**Tarih:** 2026-06-11 · **Kapsam:** Kayıt → e-posta doğrulama → setup sihirbazı → ilk harcama → onay → raporlar
**Hedef kullanıcı:** Bilgisayarla arası iyi olmayan, lüks segment yat kaptanı/sahibi — *ucuz durmamalı, kolay olmalı*

---

## 🔴 KRİTİK — Akışı kıran / raporu bozan bulgular

### K1. Yeni tenant'ta hiç harcama kategorisi oluşmuyor → ilk harcama girilemiyor

- `TenantReferenceDataInitializer` yalnızca MainCategory + WHO seçimlerini kaydediyor; **`FinancialCategory` seed'i yok**.
- Kategori seed'i sadece `DemoDataInitializer`'da var, o da `@Profile("dev")` — prod'da çalışmaz.
- Sonuç: Sihirbazı bitiren kullanıcı ilk harcamasını girmeye gelir, **Kategori alanı zorunlu ama dropdown boş**. Kategori ekranına gidip önce kategori yaratması gerektiğini bilemez → çıkmaz sokak, "program çalışmıyor" algısı.
- **Öneri:** Setup tamamlanırken seçilen MainCategory'lere göre başlangıç kategori seti otomatik oluşturulsun (örn. Marina, Yakıt, Bakım-Onarım, Market, Mürettebat Maaş, Sigorta...). Kullanıcı sonra kategori ekranından düzenler. Tek seferlik, küçük iş; etkisi en yüksek madde.

### K2. Harcama formunda Ana Kategori ve WHO opsiyonel → kayıtlar drill-down raporlarda kaybolur

- `expense-form.store.ts`: `whoId` ve `mainCategoryId` validator'sız (opsiyonel).
- Ana kategori rapor sorgusu `JOIN e.tenantMainCategory` (**inner join**) — bu alan boş olan kayıtlar rapora **hiç girmez**.
- Bilgisayarla arası iyi olmayan kullanıcı opsiyonel alanı atlar → girdiği harcamalar listede var, raporda yok → "raporlar yanlış" algısı. (Excel import için konuştuğumuz sorunun aynısı, günlük akışta.)
- **Öneri (katmanlı):**
  1. WHO seçimini zorunlu yap; WHO→MainCategory otomatik önerisi zaten kodda var (`suggestedMainCategoryId`, AUTO/MANUAL ayrımı yapılmış) — kullanıcı tek seçimle iki alanı doldurmuş olur.
  2. Ek güvenlik: rapor sorgularını `LEFT JOIN` + "Atanmamış" satırına çevir — veri asla kaybolmasın, eksik atama görünür olsun.

### K3. TR kullanıcıya İngilizce metinler sızıyor (i18n delikleri)

- `setup.ts`: ay isimleri `toLocaleString('en', ...)` ile **her zaman İngilizce**; yat tipleri ("Motor Yacht", "Gulet"), ülke adları, placeholder'lar ("e.g. 42", "e.g. Bodrum Marina") hardcoded İngilizce.
- `register.html`: e-posta placeholder'ı **"captain@demo.com"** — demo/bitirilmemiş ürün izlenimi, premium hedefle çelişiyor.
- **Öneri:** Bu sabit metinleri transloco'ya taşı; ay isimlerinde aktif dili kullan; placeholder'ı `kaptan@teknadi.com` gibi nötr bir örnekle değiştir veya kaldır.

---

## 🟠 YÜKSEK — İlk izlenimi zedeleyen noktalar

### Y1. Harcama formu günlük kullanım için fazla kalabalık (13 alan)

Kaptanın günde belki 5 kez kullanacağı form: kategori, tutar, para birimi, tarih, ödeme yöntemi, açıklama, alıcı, WHO, ana kategori, fiş no, ülke, şehir, konum, satıcı.

- **Öneri:** İki katman — üstte "hızlı giriş" (Tutar · Kategori · WHO · Tarih · Açıklama), altta kapalı bir "Detay ekle ▾" bölümünde gerisi (fiş no, satıcı, konum...). Varsayılanlar zaten iyi (bugünün tarihi, EUR, kredi kartı). Mobilde de işe yarayacak yapı.

### Y2. İlk girişte boş dashboard yönlendirmesiz

Boş durum (noData) ekranları mevcut — iyi. Ama yeni kullanıcı dashboard'a indiğinde "şimdi ne yapacağım?" sorusunun cevabı yok.

- **Öneri:** Boş dashboard'da 3 adımlı karşılama kartı: ① Kategorilerini gözden geçir → ② İlk harcamanı ekle → ③ Raporunu gör. Her adım tamamlandıkça işaretlensin. Non-savvy kullanıcıyı elinden tutarak ilk başarı anına götürür.

### Y3. E-posta doğrulama — kopma riski en yüksek an

Kayıt sonrası "e-postanı kontrol et" ekranı var, yeniden gönder butonu var — iyi. Eksikler:

- Spam klasörü uyarısı yok ("Gelmedi mi? Spam/Junk klasörüne bakın").
- Doğrulama linkine tıklayınca kullanıcıyı otomatik login + `/setup`'a taşıyan akışın pürüzsüzlüğü test edilmeli — kullanıcı burada kaybolursa bir daha gelmez.

---

## 🟡 SİHİRBAZ ÖNERİLERİ — "Premium ama kolay"

Mevcut yapı sağlam: 3 adım, ilerleme çubuğu, geri dönülebilir adımlar, mantıklı varsayılanlar (TR bayrak, EUR, tüm kategoriler seçili). Öneriler bunun üstüne:

### S1. Karar sayısını azalt — özellikle Adım 3

Adım 3'teki "Main Categories" ve "WHO" kavramları, sistemi hiç bilmeyen biri için soyut. Hepsinin seçili gelmesi doğru karar; o halde:

- Adım 3'ü **"Önerilen ayarlarla devam et"** tek butonuna indir; "Özelleştirmek istiyorum ▾" diyenlere mevcut seçim ızgarası açılsın. Non-savvy kullanıcı için en iyi sihirbaz, en az karar sorandır.
- Jargon yerine soru dili: "WHO" yerine *"Teknede harcamaları kimler yapar?"*, "Main Categories" yerine *"Hangi harcama gruplarını takip etmek istersiniz?"*

### S2. Onay ayarını sade dille anlat (Adım 2)

`managerApprovalEnabled` / `approvalLimit` açıklamasız duruyor. Tek cümlelik yardım metni: *"Belirli bir tutarın üstündeki harcamalar için ikinci bir onaycı (yönetici/sahip) isterseniz açın."* Varsayılan kapalı — doğru.

### S3. Premium görsel kimlik

Şu an: standart indigo Tailwind görünümü, logo yok, marka adı başlıkta "Yacht Management". Lüks segment algısı için:

- **Marka:** Logo + isim (maritar.com alınmışsa "Maritar"?) tutarlı kullanılsın.
- **Palet:** Indigo yerine denizcilik premium paleti — koyu lacivert (navy) zemin + altın/bronz vurgu, bol beyaz alan. Login/register/setup'ta split-screen: solda tam ekran yat/deniz görseli + tek cümle değer önerisi, sağda sade form.
- **Tipografi:** Başlıklarda serif (örn. Playfair Display / Cormorant) + gövdede mevcut sans — "yat kulübü" hissi.
- Form alanları büyük ve ferah kalsın (mevcut boyutlar iyi) — premium = sıkışık değil, ferah.

### S4. Sihirbaz sonu "başarı anı"

Setup bitince doğrudan dashboard'a atmak yerine kısa bir tamamlama ekranı: *"M/Y [Yat Adı] hazır ⚓ İlk harcamanızı ekleyerek başlayın"* + tek CTA. Kullanıcıya ne başardığını ve sıradaki tek adımı söyler.

---

## Önerilen sıralama (etki/efor)

| # | İş | Etki | Efor |
|---|---|---|---|
| 1 | K1 — Setup'ta başlangıç kategorileri oluştur | Akış kırığını giderir | Düşük |
| 2 | K2 — WHO zorunlu + LEFT JOIN/"Atanmamış" | Rapor doğruluğu | Düşük-Orta |
| 3 | K3 — i18n delikleri + demo placeholder | Güven/premium algı | Düşük |
| 4 | Y2 — Boş dashboard karşılama kartı | İlk başarı anı | Orta |
| 5 | Y1 — Hızlı giriş formu | Günlük kullanım | Orta |
| 6 | S1-S4 — Sihirbaz sadeleştirme + görsel kimlik | Premium algı | Orta-Yüksek |

Not: 1-3 pilot öncesi yapılmalı; 4-6 pilot geri bildirimiyle şekillenebilir.
