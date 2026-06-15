# Marine Management System — Girişim Planı ve SWOT Analizi

**Hazırlayan:** Belma Unsal · **Tarih:** Haziran 2026
**Durum:** Solo kurucu · Ürün geliştirme aşaması (MVP tamamlanmak üzere) · Öz kaynak (bootstrap)

> Not: `[?]` ile işaretli yerler tahmini/öneri niteliğindedir; kendi rakamlarınızla güncelleyin.

---

## 1. MEVCUT DURUM ANALİZİ

**Şu anda en acil çözülmesi gereken problem nedir?**
Ürün teknik olarak büyük ölçüde hazır, ancak henüz canlıda tek bir gerçek kullanıcı yok. En acil problem ürünü deploy edip (Docker, CI/CD ve deploy altyapısı eksikleri TODO listesinde) 1-2 pilot yat işletmesiyle sahada doğrulamak. İkinci acil konu: tek kurucu olarak geliştirme, satış ve operasyonun aynı kişide toplanması.

**Zihinde net olan ama henüz sahaya yansımamış planlar var mı?**
Evet: (a) Excel import/export'un aktive edilmesi, (b) tenant bazlı kayıt numaralandırma tasarımı, (c) pilot programı — ilk pilot belli: kurucunun abisi profesyonel kaptan ve ürünü sahada deneyecek, (d) marina/yat acenteleri üzerinden satış kanalı kurgusu. Kod tarafında self-service kayıt sihirbazı (yacht registration wizard) tamamlandı ama pazara açılmadı.

**Ekipte hangi rollerde eksikler var?**
Ekip = 1 kişi (kurucu, full-stack geliştirici) + sektör danışmanı olarak kurucunun abisi (profesyonel kaptan — alan bilgisi, pilot kullanıcı ve kaptan ağına erişim sağlıyor). Eksik roller: satış/iş geliştirme, pazarlama, ve orta vadede ikinci bir geliştirici/DevOps.

**Finansal olarak ne kadar runway kaldı?**
Öz kaynakla ilerleniyor; düzenli yatırımcı parası yok. Sabit maliyetler düşük (sunucu + alan adı, aylık ~10-30 € `[?]`). Runway, kurucunun kişisel geçim süresine bağlı — net rakam: `[doldurun]`.

**Gıda üretimi tarafında yasal süreçler?**
Bu soru şablondan geliyor, bu girişim için geçerli değil (gıda değil, B2B SaaS). Bizim eşdeğer yasal konularımız: KVKK/GDPR uyumu (çok kiracılı veri izolasyonu teknik olarak kuruldu), kullanım sözleşmesi/gizlilik politikası (henüz yazılmadı), şirketleşme (şahıs/limited kararı verilmedi `[?]`).

**Ürün geliştirme dışında en çok zaman harcanan konu nedir?**
Deploy/altyapı hazırlığı, güvenlik sıkılaştırma (JWT, refresh token, tenant izolasyon testleri) ve dokümantasyon. Müşteri keşfi ve satışa neredeyse hiç zaman ayrılamıyor — asıl risk bu.

**Değer önerisi nedir?**
Yat ve tekne işletmeleri için, Excel ve dağınık fişlerle yürüyen finansal takibi tek platformda toplayan sistem: çoklu para birimi (EUR/USD/TRY) ve kur takibi, kaptan→yönetici onay akışları, fiş/fatura ekleri, rol bazlı erişim, tam denetim izi (audit trail) ve detaylı raporlama (drill-down, yıl karşılaştırma). Genel muhasebe programlarından farkı: denizcilik operasyonunun gerçek akışına (tekne, kaptan, mürettebat, sezon) göre tasarlanmış olması; TR/EN iki dilli olması.

---

## 2. VİZYON: 1-2-3 YIL

**Pazar tanımı (coğrafya değil, segment):** Hedef pazar bir ülke değil — Akdeniz'de gezen **özel yat kaptanları ve tekne sahipleri**. Özel yatlar mobil: kışın anlaşmalı marinada, yazın seyirde. Kaptan ağı da uluslararası. Bu yüzden strateji segment bazlı: dil EN, para birimi EUR, ilk ağ = abinin kaptan çevresi. Türkiye pasif kanal (ürün TR hazır, gelen müşteri alınır ama aktif satış eforu harcanmaz).

**1. yıl sonu (Haziran 2027):** Ürün canlıda, 5-10 yat aktif kullanıyor (abinin ağı üzerinden Akdeniz özel yat segmenti), en az 2-3'ü ödeme yapıyor. Aylık abonelik modeli (yat başına ~50-150 €/ay `[?]` — özel yat segmentinde ödeme istekliliği daha yüksek) doğrulanmış.

**2. yıl sonu (Haziran 2028):** 30-50 ödeyen yat, aylık yinelenen gelir (MRR) kurucunun yarı geçimini karşılıyor. İkinci segment: yat yönetim şirketleri (filo yöneten) — tek satışla 5-10 tekne. İlk yarı zamanlı satış/destek kişisi. Mobil arayüz — gezen kaptan için sahada telefondan giriş kritik.

**3. yıl sonu (Haziran 2029):** 100+ yat, sürdürülebilir gelir, 3-4 kişilik çekirdek ekip. Segment genişlemesi: charter operasyonları ve yönetim şirketleri; Türkiye'nin aktif pazara dönüştürülmesi. Marina/charter yazılımlarıyla entegrasyonlar. İsteğe bağlı: tohum yatırım ya da kârlı bootstrap olarak devam kararı.

### Genel Çerçeve

1. **Ne sunuyorum?** Yat/tekne işletmelerine yönelik bulut tabanlı finansal yönetim ve onay akışı SaaS'i.
2. **Kim için değerli?** Birincil: Akdeniz'de çalışan özel yat kaptanları ve yat sahipleri — kaptan, sahibinin parasını harcar ve hesap vermek zorundadır; sistem bu hesap verme ilişkisini (harcama girişi → onay → fiş eki → rapor) dijitalleştirir. İkincil: yat yönetim/charter şirketleri, filo yöneten profesyonel yönetim şirketleri.
3. **Neden benden alsınlar?** Sektöre özel (genel muhasebe yazılımı değil), çoklu para birimi + onay akışı + denetim izi tek pakette, iki dilli, hafif ve uygun fiyatlı. Mevcut alternatif çoğunlukla Excel — rakip "hiçbir şey kullanmamak".
4. **Mevcut kaynaklar:** Çalışan, modern mimarili ürün (Angular 20 + Spring Boot, multi-tenant); 10+ yıllık `[?]` yazılım deneyimi; sektörün içinden bir pilot kullanıcı ve danışman (kaptan abi); düşük işletme maliyeti; tam zamanlı emek.

### Ürün / Ürün Geliştirme

- **İlk versiyon ne zaman?** MVP fiilen hazır; kalan işler deploy altyapısı + güvenlik sıkılaştırma. Hedef: **3 ay içinde canlı** (Eylül 2026), pilotlarla.
- **Geri bildirim nasıl toplanacak?** İlk pilot: kurucunun abisi (profesyonel kaptan) — ürünü gerçek tekne operasyonunda kullanacak; harcama girişi, onay akışı ve raporları sahada test edecek. Haftalık birebir görüşme + kullanım metrikleri. Bu, hedef kullanıcıya (kaptan) sınırsız ve dürüst geri bildirim erişimi demek — çoğu girişimin sahip olmadığı bir avantaj. İlk 6 ay yol haritasını pilot geri bildirimi belirleyecek.
- **Dış destek?** Çekirdek geliştirme bende kalacak. Dışarıdan: UI/UX gözden geçirme (freelance), güvenlik/penetrasyon testi (canlıya çıkmadan), sektör danışmanlığı için kaptan/işletmeci mentor.
- **Ürün yol haritası (sıralı):**
  1. **v1 canlı** (web, mevcut ürün) — pilot ve ilk müşteriler.
  2. **Mobil uygulama** — gezen kaptanın sahada harcama girişi; v1 doğrulandıktan sonra.
  3. **Fiş OCR API'si (Python)** — fişin fotoğrafından tutar/tarih/satıcı/kategorinin otomatik çıkarılması. Mobil ile birleşince ana akış "fotoğraf çek → onayla → gönder" olur; manuel girişin en büyük sürtünmesini kaldırır ve net rekabet farkı yaratır. Çoklu dil/para birimli fişler (her ülkenin fişi farklı) bu segmentin özel zorluğu — gerçek pilot fişleriyle test edilmeli.

### İş Geliştirme / Ticarileşme

- **İlk müşterilere hangi kanaldan?** Birinci kanal: kaptan abinin uluslararası meslek ağı — özel yat kaptanları marina kışlamalarında ve sezonda sürekli birbirleriyle temas halinde; memnun bir kaptanın tavsiyesi en güçlü satış aracı. İkinci: uluslararası kaptan/crew Facebook-WhatsApp grupları, kışlama marinaları (kaptanların toplandığı yer), deniz fuarları. B2B'de ilk 10 müşteri referans ve birebir ilişkiyle gelir.
- **1 yıl içinde gelir için yol?** Eylül 2026 canlı → 3 ay ücretsiz pilot (2-3 işletme) → Ocak 2027'den itibaren ücretli abonelik. Pilotlara kurucu-müşteri indirimi ile dönüşüm.
- **2. yılda sürdürülebilir model için adımlar?** Fiyatlandırmayı tekne sayısına göre kademelendir, self-service onboarding'i (hazır) aktif kullan, churn'ü ölç, filo yöneten şirketlere odaklan (daha yüksek sözleşme değeri), yıllık ödeme teşviki.
- **Birincil müşteri / erken kabullenici:** Erken kabullenici #1 belli: kurucunun abisi (Akdeniz'de özel yat kaptanı). Profili genelleştirilebilir: teknolojiye açık özel yat kaptanları. Ödemeyi yapan taraf çoğu zaman yat sahibi olacağı için değer önerisi iki yönlü anlatılmalı: kaptana "işini kolaylaştırır", sahibe "paranın nereye gittiğini gösterir". Birincil müşteri (ölçekte) = 5+ tekne yöneten yat yönetim şirketleri.

### Finansal Planlama

- **Başlangıç bütçesi:** Düşük — yıl 1 için ~3.000-5.000 € `[?]`: sunucu/altyapı (~500 €), şirketleşme + muhasebeci (~1.500 €), fuar/pazarlama (~1.000 €), güvenlik testi/tasarım (~1.000 €). En büyük maliyet kurucunun zamanı (fırsat maliyeti).
- **Yıl 1 giderler:** Sabit: hosting, alan adı, e-posta servisi, muhasebeci. Değişken: pazarlama, yol (marina ziyaretleri), freelance destek.
- **Yatırım planı:** Yıl 1: hayır — bootstrap + hibe başvuruları (TÜBİTAK BİGG, KOSGEB Ar-Ge `[?]` değerlendirilecek). Yıl 2: traksiyon iyiyse tohum yatırım opsiyonel; hedef yatırım zorunluluğu olmadan büyümek.

### İnsan Kaynağı / Ekip

- **Yıl 1:** Solo + freelance noktasal destek (tasarım, güvenlik). **Yıl 2:** Yarı zamanlı satış/müşteri başarısı (tercihen sektörden). **Yıl 3:** 3-4 kişi — geliştirici, satış, destek.
- **Kritik rol (yıl 1):** Satış/iş geliştirme — sektörü tanıyan bir ortak ya da komisyon bazlı iş geliştirici en büyük kaldıraç.
- **Strateji:** Önce komisyon/freelance modeliyle dene; kültür ve traksiyon oturursa hisse opsiyonlu ilk çalışan. Teknik tarafta uzun süre tek kalmak sürdürülebilir değil — yıl 2'de "bus factor" riskine karşı en az dokümantasyon + bir yedek geliştirici ilişkisi.

### Satış / Pazarlama

- **Kanal:** Kaptan ağı (abinin çevresi, kışlama marinaları, crew grupları) + dijital (İngilizce sektörel içerik/SEO: "yacht expense management", "captain expense report"; yatçılık Instagram/forum hesapları). B2B'de içerik + referans, soğuk reklamdan önce gelir. Pazarlama dili birincil olarak İngilizce.
- **İlk müşteri ne zaman?** İlk pilot (kaptan abi): canlıya çıkar çıkmaz — sezon içindeyken (yaz 2026) başlaması ideal, gerçek operasyon verisiyle test edilir. İlk dış pilot: Eylül-Ekim 2026. İlk ödeyen: Ocak 2027.
- **Yıl 1 sonu hedef:** 5-10 aktif tekne, 2-3 ödeyen müşteri `[?]`.
- **Yıl 2 stratejisi:** Referans programı (mevcut müşteri yeni tekne getirirse indirim), filo segmentine yönelme, fuar varlığı, vaka çalışması içerikleri, marina işletmeleriyle ortaklık.

### SMART Hedef (Yıl 1)

| Kriter | Hedef |
|---|---|
| **S**pesifik | Ürünü canlıya alıp 5 aktif tekne işletmesine kullandırmak, en az 2'sini ödeyen müşteriye çevirmek |
| **M**easurable (Ölçülebilir) | 5 aktif tenant, 2 ödeyen abonelik, aylık ≥%60 aktif kullanım |
| **A**chievable (Ulaşılabilir) | Ürün hazır; kalan iş deploy (3 ay) + satış. Solo kurucu için 5 müşteri gerçekçi |
| **R**elevant (İlgili) | 3 yıllık "100+ tekne, bölgesel oyuncu" vizyonunun doğrulama adımı |
| **T**ime-bound (Zamanlı) | Canlı: Eylül 2026 · İlk ödeyen: Ocak 2027 · 5 aktif: Haziran 2027 |

---

## 3. SWOT ANALİZİ (Güncel — Haziran 2026)

### Güçlü Yönler (Strengths)

- **Ürün/Teknoloji:** MVP fiilen tamam ve teknik kalitesi yüksek — multi-tenant izolasyon, onay akışları, denetim izi (Envers), çoklu para birimi, TR/EN i18n, self-service kayıt sihirbazı. Modern ve bakımı kolay yığın (Angular 20, Spring Boot 3, Java 21).
- **Kurucu takım:** Full-stack tek başına ürün çıkarabilen kurucu; ürün kararlarında hız, sıfır koordinasyon maliyeti; sistematik çalışma kültürü (mimari/kod inceleme, test ve deploy kontrol listeleri mevcut). Ailede profesyonel kaptan (abi): alan bilgisi, ilk pilot kullanıcı ve kaptan ağına doğrudan erişim — ürün-pazar uyumu için kritik avantaj.
- **Finans:** Çok düşük sabit maliyet, borç/yatırımcı baskısı yok; SaaS modeli yinelenen gelir potansiyeli taşıyor.
- **Ürün geliştirme:** Geri bildirimi aynı gün ürüne yansıtabilecek çeviklik; teknik borç bilinçli yönetiliyor (TODO/DEPLOY-REVIEW kayıtlı).
- **Pazar konumu:** Niş, sektöre özel çözüm; ana rakip "Excel" — geçiş eşiği değer anlatılırsa düşük.

### Zayıf Yönler (Weaknesses)

- **Kurucu takım/İK:** Geliştirme tek kişide — hastalık/tıkanma durumunda her şey durur (bus factor = 1); satış ve pazarlama rolü hâlâ boş. Sektör bilgisi abiden geliyor ama gayri resmi — danışmanlık rolü netleştirilmemiş.
- **Ticarileşme/Satış:** Sıfır müşteri, sıfır gelir, doğrulanmamış fiyatlandırma; değer önerisi sahada test edilmedi (ilk test pilotla başlayacak); satış kanalı yok. Dikkat: aile içi pilot geri bildirimi fazla nazik olabilir — dış pilotlarla doğrulamak şart.
- **Pazarlama:** Marka, web sitesi, içerik, referans yok; hedef kitleye erişim ağı henüz kurulmadı.
- **Ürün:** Canlı ortam yok (Docker/CI/CD eksik); güvenlik sıkılaştırma maddeleri açık (refresh token hash, tenant izolasyon testi); mobil deneyim yok — gezen kaptan harcamayı anında telefondan girmek isteyecek, bu segmentte mobil "olsa iyi olur" değil zorunluluk.
- **Finans:** Gelir yokken runway tamamen kişisel birikime bağlı; hibe/destek başvurusu yapılmadı.
- **İşletme yönetimi:** Şirketleşme, sözleşmeler, KVKK dokümantasyonu, faturalama süreci kurulmadı.

### Fırsatlar (Opportunities)

- **Pazar:** Akdeniz özel yat segmenti EUR bazlı ve ödeme istekliliği yüksek; sektör dijitalleşmede geride — çoğu kaptan harcama takibini hâlâ Excel/WhatsApp/fiş zarfıyla yapıyor. Türkiye pazarı (ürün TR hazır) ek maliyetsiz ikinci pazar olarak bekliyor.
- **Ticarileşme:** Filo yöneten yönetim şirketleri = tek satışla çok tekne; marina ve acentelerle kanal ortaklığı; charter sezonu (Nisan-Ekim) doğal satış takvimi sağlıyor.
- **Coğrafya:** Ürün İngilizce hazır — Yunanistan/Hırvatistan/Akdeniz charter pazarına genişleme önü açık.
- **Finans:** TÜBİTAK BİGG, KOSGEB gibi geri ödemesiz destekler bootstrap modeline uygun.
- **Ürün:** Planlanan mobil + fiş OCR ("fotoğraf çek → onayla → gönder") akışı gezen kaptan için güçlü bir farklılaştırıcı; çoklu para birimi gezen yatlar için (her limanda farklı para) doğal uyum; bakım/marina/mürettebat modülleriyle "tekne ERP'sine" genişleme alanı.

### Tehditler (Threats)

- **Rekabet:** Avrupa'da süperyat segmentine hizmet veren yerleşik harcama/yat yönetim yazılımları mevcut — bunlar genelde büyük yatlara ve yönetim şirketlerine odaklı/pahalı; konumlanma "küçük-orta özel yat için hafif ve uygun fiyatlı" olmalı. Rakip analizi yapılmalı `[?]`.
- **Mevzuat:** Avrupa müşterisi = GDPR uyumu, AB içi KDV/faturalama ve şirketin nerede kurulacağı sorusu (TR şirketiyle EUR faturalama vs. Estonya e-Residency vb. `[?]`).
- **Pazar:** Sektör ilişki bazlı ve geleneksel — satış döngüsü beklenenden yavaş olabilir; sezonluk nakit akışı müşterilerin yıllık abonelik iştahını düşürebilir.
- **Finans/Makro:** TR'de kur ve enflasyon belirsizliği fiyatlandırmayı zorlaştırır; kurucunun runway'i biterse proje yarıda kalır.
- **İşletme:** Tek kurucu tükenmişlik riski; KVKK/veri güvenliği ihlali çok kiracılı sistemde itibar açısından ölümcül olur.
- **Teknoloji:** Self-service SaaS'lerde destek yükü küçük ekipte hızla büyüyebilir.

### SWOT'tan Çıkan Öncelikli Aksiyonlar

1. **3 ay:** Deploy altyapısını bitir, güvenlik maddelerini kapat, canlıya çık (W→S).
2. **Hemen (yaz 2026):** Abiyi (kaptan) ilk pilot olarak başlat — sezon içinde gerçek operasyon verisiyle test (S→ ticarileşme).
3. **3-6 ay:** Abinin ağı + marina sahası üzerinden 2-3 dış pilot işletme bul — aile dışı dürüst geri bildirim için şart (W: ticarileşme).
3. **Paralel:** TÜBİTAK BİGG/KOSGEB başvurusu (W: finans → O).
4. **6-12 ay:** Sektörden komisyon bazlı satış ortağı (W: ekip/satış).
5. **Sürekli:** Şirketleşme + KVKK + sözleşme altyapısı (W: işletme yönetimi).
