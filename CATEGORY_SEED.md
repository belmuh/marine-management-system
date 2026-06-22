# Başlangıç Kategori Seti (Seed) Önerisi

**Kaynak:** Piyasadaki yat yönetim yazılımının strict kategori listesi (4 ekran görüntüsü, 2026-06)
karşılaştırıldı: mevcut `MainCategoryDataLoader` (ISS standardı, 7 ana kategori) + `WhoDataLoader` (16 WHO).
**Amaç:** K1 düzeltmesi — setup sihirbazı tamamlanırken tenant'a otomatik oluşturulacak `FinancialCategory` seti.

---

## Kaynak listenin çıkarımı (piyasa yazılımı)

| Ana Grup | Alt Kategoriler |
|---|---|
| Administration | Administration & Miscellaneous · Bank Charges · Management Fees · Management Travel & Expenses |
| Charter | *(tarihli, tek seferlik kayıtlar: "Federman charter 2025" vb. — anti-pattern, seed'e alınmadı)* · Charter Preparation |
| Communication | Internet · Miscellaneous · Phones |
| Crew | Crew Wages · Food and Beverages · Medical · Travel · Uniform · Visas |
| Insurance | Crew & Guests medical Insurance · Hull and Machinery |
| Operational | Agency fees, taxes & Formalities · Dockage & Marina · Fuel for Tender, Jet ski & Ribco · Fuel for Yacht · Gratuities · Water & electricity |
| Owner and Guest | Food and Beverages · Owner Capital equipment · Transport |
| Repair & Maintanance *(sic)* | Repair and Maintenance |

### Tasarım notları

1. **İsim çakışması:** Kaynak listede "Food and Beverages" iki ana grupta tekrar ediyor. Bizim modelde bu ayrım **WHO ekseni** ile çözülür (tek kategori + WHO=Personel/Misafir/Sahip) — `findByName` benzersizlik varsayımı da korunur.
2. **Charter anti-pattern'i:** Kullanıcıların sefer başına kategori açması listenin kirlenmesine yol açmış. İhtiyaç gerçek (sefer bazlı maliyet); çözüm kategori değil, ileride "sefer/etiket" özelliği (yol haritası adayı).
3. **MainCategory eksiği:** Communication bizde karşılıksız → **8. ana kategori önerisi: İletişim / Communication** (technical=false, displayOrder=8).
4. Operational dağılımı: Dockage&Marina, Water&Electricity → *Liman ve Bağlama*; Fuel → *Yakıt*; Agency/Formalities, Gratuities → *İdari Giderler*.
5. Owner and Guest dağılımı: F&B → *Kumanya* + WHO; Transport → *İdari* + WHO; Capital Equipment → *Bakım ve Onarım* (Yedek Parça & Ekipman) + WHO=Sahip.

---

## Önerilen seed: GİDER kategorileri (sadeleştirilmiş, 2026-06 revizyonu)

**Sadeleştirme ilkesi:** WHO ekseniyle ayrıştırılabilen alt kırılımlar ayrı kategori olmaz.
İlk 22'lik liste 17'ye indirildi; birleştirmeler:

| Eski (22) | Yeni (17) | Ayrım nasıl yapılır |
|---|---|---|
| İnternet & Uydu + Telefon | İnternet & Telefon | gerek yok (aynı ana kategori: İletişim) |
| Transfer & Ulaşım + Personel Seyahat | Seyahat & Ulaşım | WHO = Personel / Misafir / Tekne Sahibi |
| Tekne & Makine Sigortası + Sağlık Sigortası | Sigorta Poliçeleri | WHO = Tekne Gövdesi / Personel / Misafir |
| Yat Yakıtı + Tender & Jetski Yakıtı | Yakıt | WHO = Ana Makine / Jeneratör / Tender / Jetski |
| Bakım & Onarım + Yedek Parça & Ekipman | Bakım & Onarım | WHO teknik listesi sistemi belirler |
| Personel Sağlık | Sağlık Giderleri *(ad değişti)* | WHO = Kaptan / Personel / Misafir |

| # | Kategori (TR) | Category (EN) | Ana Kategori | Technical |
|---|---|---|---|---|
| 1 | Banka Masrafları | Bank Charges | İdari Giderler | – |
| 2 | Yönetim Ücretleri | Management Fees | İdari Giderler | – |
| 3 | Acente, Vergi & Resmi İşlemler | Agency Fees, Taxes & Formalities | İdari Giderler | – |
| 4 | Bahşişler | Gratuities | İdari Giderler | – |
| 5 | Diğer Giderler | Other Expenses | İdari Giderler | – |
| 6 | Seyahat & Ulaşım | Travel & Transfers | İdari Giderler | – |
| 7 | İnternet & Telefon | Internet & Phones | İletişim | – |
| 8 | Personel Maaşları | Crew Wages | Personel Giderleri | – |
| 9 | Sağlık Giderleri | Medical Expenses | Personel Giderleri | – |
| 10 | Üniforma | Uniforms | Personel Giderleri | – |
| 11 | Vize | Visas | Personel Giderleri | – |
| 12 | Sigorta Poliçeleri | Insurance Policies | Sigorta | – |
| 13 | Marina & Bağlama | Dockage & Marina | Liman ve Bağlama | – |
| 14 | Su & Elektrik | Water & Electricity | Liman ve Bağlama | – |
| 15 | Yakıt | Fuel | Yakıt | ✓ |
| 16 | Bakım & Onarım | Repair & Maintenance | Bakım ve Onarım | ✓ |
| 17 | Yiyecek & İçecek | Food & Beverages | Kumanya / Erzak | – |

> WHO ekseni kullanım örnekleri: Yiyecek & İçecek + WHO=Misafir → misafir ikramı; Bakım & Onarım + WHO=Jeneratör → jeneratör parçası/servisi; Seyahat & Ulaşım + WHO=Tekne Sahibi → sahip transferi; Yakıt + WHO=Jetski → jetski yakıtı.

## Önerilen seed: GELİR kategorileri

| # | Kategori (TR) | Category (EN) | Not |
|---|---|---|---|
| 1 | Sahip Fonu | Owner's Funds | Özel yatta ana gelir: sahibin işletme hesabına aktardığı para |
| 2 | Charter Geliri | Charter Income | Charter yapan tekneler için |
| 3 | Diğer Gelir | Other Income | İade, sigorta tazminatı vb. |

---

## Uygulama planı (K1)

1. `MainCategoryDataLoader`'a **İletişim / Communication** eklenir (8. sıra). *(Mevcut DB'lerde "already exist, skipping" koruması var — yeni kurulumlar alır, mevcut DB için küçük migration/manuel ekleme gerekir.)*
2. `TenantReferenceDataInitializer.initializeTenantReferenceData(...)` genişletilir: tenant'ın **seçtiği** ana kategorilere bağlı seed kategorileri oluşturur (seçmediği ana kategorinin kategorileri atlanır). Gelir kategorileri her zaman oluşur.
3. Kategori açıklaması: `"Başlangıç seti"` / display order: yukarıdaki sıra.
4. İleride kategori→ana kategori varsayılan eşlemesi forma da taşınabilir (kategori seçilince ana kategori otomatik dolar) — K2'yi pratikte tamamen çözer.
