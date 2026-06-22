# PostgreSQL RLS Uygulama Planı

Hedef: Tenant izolasyonunu uygulama katmanından (Hibernate `@Filter` + AOP) veritabanı katmanına indirmek. Hibernate filtresi üstte kalır (defense in depth); RLS, uygulama kodundaki herhangi bir hata durumunda son savunma hattı olur.

## 1. Kapsam — tablo sınıflandırması

**RLS uygulanacak (tenant_id mevcut):**

| Tablo | Not |
|---|---|
| `financial_entries` | Ana veri, idx_financial_entries_tenant mevcut |
| `financial_categories` | |
| `entry_approvals` | |
| `payments` | |
| `tenant_main_categories` | |
| `tenant_who_selections` | |
| `financial_entries_aud` | Envers audit — tenant_id var, politika eklenecek |

**Önce şema değişikliği gerekli:**

| Tablo | Sorun | Çözüm |
|---|---|---|
| `financial_entry_attachments` | tenant_id kolonu yoktu | ✅ YAPILDI — henüz canlı ortam olmadığı için V001 `CREATE TABLE`'a doğrudan eklendi (ayrı V010 + backfill gereksiz). Entity'ye denormalize `tenantId` + `@Filter` + `@PrePersist` guard eklendi; tenant parent entry'den `associateWithEntry()` sırasında miras alınır |

**RLS dışı (bilinçli muafiyet):**

| Tablo | Gerekçe |
|---|---|
| `users` | `JwtAuthenticationFilter` tenant context oluşmadan ÖNCE global `findByEmail` yapıyor. RLS eklenirse login kırılır. İzolasyon app katmanında kalır (organization_id eşleşme kontrolü filtrede zaten var) |
| `organizations` | Tenant tablosunun kendisi; onboarding ve resolver tenant'sız erişir |
| `main_categories`, `who` | Global referans verisi, tenant'a ait değil |
| `revinfo` | Envers global revizyon tablosu |

## 2. Migration

**Attachments tenant_id:** ✅ YAPILDI — canlı ortam henüz olmadığı için V001'e işlendi (kolon + `idx_attachments_tenant` index). Not: ileride herhangi bir ortama deploy yapıldıktan sonra migration'lar değişmez kabul edilecek; o noktadan itibaren değişiklikler yeni versiyonlu dosyalarla yapılır.

**V002__enable_rls.sql** (her kapsam tablosu için):

```sql
ALTER TABLE financial_entries ENABLE ROW LEVEL SECURITY;
ALTER TABLE financial_entries FORCE ROW LEVEL SECURITY;  -- kritik, bkz. §4

CREATE POLICY tenant_isolation ON financial_entries
    USING (tenant_id = current_setting('app.tenant_id', true)::bigint)
    WITH CHECK (tenant_id = current_setting('app.tenant_id', true)::bigint);
```

Davranış: `app.tenant_id` set edilmemişse `current_setting(..., true)` NULL döner → hiçbir satır eşleşmez → **fail-closed**. Mevcut SYSTEM-fallback ve "filtre açılmadı" senaryoları artık veri sızdırmak yerine boş sonuç döndürür.

## 3. `SET LOCAL app.tenant_id` plumbing

HikariCP bağlantıları paylaştığı için `SET` (session-level) tehlikeli — bağlantı havuza dönünce ayar başka tenant'ın isteğine taşınır. `SET LOCAL` transaction sonunda otomatik sıfırlanır; bu yüzden **yalnızca transaction içinde** çalışır.

Uygulama: transaction başlangıcına bağlanan bir mekanizma —

```java
// Seçenek A (önerilen): @Transactional'ı saran AOP aspect
@Around("@annotation(org.springframework.transaction.annotation.Transactional) || ...")
// tx başladıktan sonra ilk iş:
entityManager.createNativeQuery("SELECT set_config('app.tenant_id', :tid, true)")
```

`set_config(..., true)` = SET LOCAL eşdeğeri, parametre bağlanabilir (SQL injection açısından `SET LOCAL` literal'dan güvenli).

Dikkat edilecekler:

- **Transaction dışı sorgular:** `spring.jpa.open-in-view` açıksa lazy-load'lar tx dışında çalışabilir → setting yok → boş sonuç. Bu fail-closed olduğu için veri sızdırmaz ama davranış değişikliği yaratabilir. Plan: `open-in-view=false` yap (zaten best practice), tüm okuma yolları `@Transactional(readOnly=true)` servislerden geçsin.
- **Background context'ler:** `TenantAwareScheduledTask` zaten tenant döngüsünde `TenantContext.setCurrentTenantId` çağırıyor — aspect bunu otomatik yakalar, ekstra iş yok. `CommandLineRunner`/bootstrap kodu (DemoDataInitializer, data loader'lar) için §4'teki maintenance rolü kullanılır.
- Mevcut `TenantFilterAspect` ile birleştirilebilir: tek aspect hem Hibernate filter hem `set_config` yapar.
- **REQUIRES_NEW / iç transaction'lar:** `SET LOCAL` transaction-scoped olduğu için `REQUIRES_NEW` ile açılan yeni fiziksel transaction (ve muhtemelen yeni connection) temiz başlar — outer tx'in `app.tenant_id`'si orada yoktur → inner tx fail-closed'a düşer, sıfır satır görür. Mekanizma bu yüzden **her** transaction başlangıcına bağlanmalı; method-entry AOP yerine `TransactionSynchronization` / Hibernate transaction callback tercih sebebi budur (REQUIRES_NEW dahil her tx'te tetiklenir). Kodda bugün REQUIRES_NEW kullanımı yok (tarandı) — ama bu test edilmemiş varsayım olarak kalmamalı, bkz. §6.7.
- **Erken uyarı (sinsi-bug dedektörü):** `TenantContext` dolu ama aktif transaction yok (→ `app.tenant_id` set edilemeyecek) durumunda aspect `log.warn` basar. Senaryo: biri `nativeQuery=true` yazıp `@Transactional` koymayı unutursa fail-closed sayesinde veri sızmaz ama "sorgu çalışıyor, sonuç boş" tipi sessiz bug çıkar — bu log onu görünür kılar. Tespit: `TransactionSynchronizationManager.isActualTransactionActive()`.

## 3b. Tenant kaynağı tek-kaynak kuralı

RLS, `set_config`'e ne yazıldıysa onu sadakatle uygular — tenant kaynağındaki bir hatayı düzeltmez. Bu yüzden kural: **tenant_id'nin tek kaynağı JWT ile doğrulanmış principal'dır; header, query param veya request body'den asla okunmaz.**

Mevcut durum (kod taramasıyla doğrulandı): request path'te tek yazma noktası `TenantFilter.establishTenantContext()` — authenticated principal'ın `organization_id`'sinden alıyor; `X-Tenant-Id` benzeri header okuyan hiçbir kod yok. Kural bugün sağlanıyor. Yapılacaklar:

1. `TenantContext.setCurrentTenantId`'nin request-path'teki tek çağrıcısının `TenantFilter` olduğu ArchUnit kuralı veya kod yorumu ile sabitlenir (request dışı meşru çağrıcılar: `TenantAwareScheduledTask`, `TenantAwareTaskDecorator`, onboarding setup, bootstrap)
2. §6'ya regresyon testi: `X-Tenant-Id: <başka tenant>` header'ı ile istek → header yok sayılır, JWT'deki tenant'ın verisi döner

## 4. DB rol ayrımı — FORCE RLS'in nedeni

Tablo sahibi (owner) RLS'i varsayılan olarak **bypass eder**. Flyway ve uygulama aynı kullanıcıyla (`marine_user`) bağlandığı için `FORCE ROW LEVEL SECURITY` şart.

Önerilen hedef durum (V002 ile birlikte veya sonrasında):

- `marine_migrator` — owner, Flyway bunu kullanır
- `marine_app` — owner değil, `BYPASSRLS` yok, uygulama bunu kullanır
- `marine_maintenance` — `BYPASSRLS` var; bootstrap/CLR/cross-tenant admin işleri için ayrı DataSource (veya bu işler tenant döngüsüyle yapılır, üçüncü role hiç gerek kalmaz — tercih edilen bu)

MVP'de tek kullanıcı + FORCE RLS yeterli; rol ayrımı ikinci faz olabilir.

## 5. Dev/test profil sorunu (kritik)

Şu an `application-dev.properties` ve `application-test.properties`: `flyway.enabled=false` + `ddl-auto=create-drop`. Yani **RLS dev ve test ortamında hiç var olmaz** — prod'a ilk kez prod'da denenmiş olur. Kabul edilemez.

Plan:

1. Dev: `ddl-auto=validate` + `flyway.enabled=true` (lokal Postgres'te migration'lar koşar)
2. Test: Testcontainers PostgreSQL'e geçiş (H2/create-drop RLS'i hiç test edemez). `@ServiceConnection` ile tek satır konfigürasyon
3. Hibernate şema üretimi ile Flyway şemasındaki farklar varsa (muhtemel) önce bunlar giderilir — bu, planın en zahmetli kısmı olabilir

## 6. İzolasyon test paketi (CI)

Testcontainers Postgres üstünde, `TenantIsolationIT`:

1. **API seviyesi:** Tenant A token'ı ile Tenant B'nin entry/category/payment/attachment ID'lerine GET/PUT/DELETE → 404 beklenir (403 değil — kaynak varlığı sızdırılmaz)
2. **RLS seviyesi (app bypass simülasyonu):** `app.tenant_id` set etmeden native query → 0 satır; yanlış tenant_id ile INSERT → policy violation hatası. Pozitif yön de doğrulanır: tenant A set → yalnızca A'nın satırları, tenant B set → yalnızca B'nin satırları (sadece fail-closed değil, doğru filtreleme)
3. **findById IDOR testi:** Bulgu #1'in regresyon testi — Tenant A servisi üzerinden Tenant B UUID'si ile findById → boş/404 (RLS sonrası bu otomatik sağlanır, test bunu ispatlar)
4. **Background context testi:** TenantContext olmadan repository çağrısı → boş sonuç (fail-closed doğrulaması). Varyasyon: `TenantAwareScheduledTask` çok-tenant döngüsünde her iterasyonun yalnızca o iterasyonun tenant'ını gördüğü (önceki iterasyondan sızıntı olmadığı) doğrulanır
5. **Tenant kaynağı spoof testi (§3b):** Tenant A JWT'si + `X-Tenant-Id: B` header'ı → header yok sayılır, yalnızca A'nın verisi döner
6. **Envers `_aud` insert testi:** Tenant context'liyken create/update/delete → `financial_entries_aud` insert'i RLS `WITH CHECK`'e takılmadan başarılı olur; Envers insert'i app transaction'ı içinde koştuğu için `app.tenant_id` set olmalı — bu test onu ispatlar
7. **REQUIRES_NEW testi (§3'teki varsayımın ispatı):** Outer `@Transactional` içinden `Propagation.REQUIRES_NEW` ile açılan inner transaction'da repository sorgusu → inner tx'in de doğru `app.tenant_id`'yi gördüğü doğrulanır. Mekanizma method-entry AOP'ye bağlanırsa bu test kırılır — tasarımı `TransactionSynchronization` yönünde tutan guard test budur
8. **Aspect ordering assertion'ı:** Her entegrasyon testinin transaction'ında, ilk repository sorgusundan önce `SELECT current_setting('app.tenant_id', true)` ile setting'in set edilmiş olduğu assert edilir — set_config'in sorgulardan önce koştuğunun ispatı

CI: mevcut pipeline'a `mvn verify` ile entegrasyon test aşaması (Docker gerektirir).

## 7. Rollout sırası

1. ✅ Attachments tenant_id (V001'e işlendi) + entity güncellemesi
2. Dev/test profillerini Flyway + Testcontainers'a geçir
3. `set_config` mekanizması (TransactionSynchronization) + `open-in-view=false`
4. V002 (enable RLS) — önce yalnızca `financial_entries`'te aç, staging'de doğrula, sonra kalan tablolara genişlet
5. İzolasyon test paketi (V002 ile aynı PR'da — RLS'in kanıtı)
6. (Faz 2) DB rol ayrımı, bulgu #2/#8 fix'leri, SYSTEM fallback'in kaldırılması

## 8. Performans notu

`current_setting()` STABLE fonksiyon — planner sorgu başına bir kez değerlendirir, satır başına değil. Mevcut `(tenant_id, ...)` composite index'ler RLS predicate'ini aynen kullanır. Beklenen ek maliyet ihmal edilebilir; yine de adım 4'te staging'de `EXPLAIN ANALYZE` ile ana sorgular doğrulanır.

## Açık sorular

- `users` tablosuna ileride RLS istenirse: auth lookup'ı için `SECURITY DEFINER` fonksiyon veya ayrı auth-rolü gerekir — şimdilik kapsam dışı
- Dev'de `create-drop` ile Flyway şeması arasında drift varsa kapatılması gereken farklar çıkabilir (adım 2'de görülecek)
