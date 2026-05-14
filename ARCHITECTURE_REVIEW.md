# Marine Management System — Mimari İnceleme

> Senior developer / sistem mimarı bakışıyla detaylı analiz.
> Tarih: 2026-05-06
> Kapsam: `src/main/java` (~22.9K satır), `src/main/resources` (config + 9 Flyway migration), `src/test/java`

---

## 1. Genel Mimari Profili

| Boyut | Değer |
|---|---|
| Teknoloji yığını | Spring Boot 3.5.7, Java 21, PostgreSQL, Flyway, Hibernate Envers, JWT, Caffeine, Cloudflare R2, Prometheus/Actuator |
| Kod hacmi | ~22.911 satır Java, 223 main + 8 test sınıfı |
| Migration sayısı | 9 (V001 → V009) |
| Modüller | `auth`, `finance`, `organization`, `users`, `files` |
| Mimari stil | Modüler Monolit + DDD-esinli katmanlama (`domain` / `application` / `infrastructure` / `presentation`) + Multi-tenant SaaS |

---

## 2. Doğru Kurgulananlar

### 2.1 Modül + Katman Ayrımı
`modules/{auth, finance, organization, users, files}` ayrımı bounded context'leri net ifade ediyor. Her modülde `domain → application → infrastructure → presentation` katmanları doğru izole edilmiş. `domain.entities`, `domain.vo`, `domain.service`, `application.commands`, `application.usecase`, `application.mapper` paketleri DDD vokabülerine uygun. Bu, küçük takımın bile büyütebileceği temiz bir omurga.

### 2.2 Multi-Tenant Defense-in-Depth (En güçlü tarafı)
Tenant izolasyonu sekiz katmanlı kurulmuş:

- `TenantContext` — ThreadLocal, primitive `Long` (entity referansı değil, lazy-load tuzağına düşmemiş).
- `TenantFilter` — servlet filter; JWT auth sonrası context'i kurar, finally'de temizler.
- `TenantInterceptor` — `@PublicEndpoint` farkındalığı + context yokluğu = `AccessDeniedException`.
- `HibernateTenantIdentifierResolver` — Hibernate seviyesi.
- `BaseTenantEntity` + `@Filter` — her query'e otomatik `tenant_id = ?`.
- `TenantEntityListener` — `@PrePersist` enjekte eder, `@PreUpdate` mismatch'i `IllegalStateException` ile durdurur.
- `TenantFilterAspect` — `@Service`/`@Transactional` üzerinde Hibernate filter'i AOP ile aktive eder.
- `TenantAwareTaskDecorator` — async/scheduled job'larda context propagation.
- `TenantFilterMetrics` — security violation metriği (Micrometer).

Bu çok-katmanlı yaklaşım "sessiz veri sızıntısı" riskini ciddi şekilde azaltıyor. Enterprise-grade kabul edilir.

### 2.3 Value Object Disiplini
`Money`, `EntryNumber`, `Period`, `Balance`, `CumulativeBalance`, `MonthlyBalance` immutable VO olarak tasarlanmış. `Money` sınıfı:
- ISO 4217 currency code doğrulaması
- `BigDecimal` scale=2, `RoundingMode.HALF_EVEN`
- Currency-mismatch koruması (`validateSameCurrency`)
- `convertUsing(rate, target)` ile finansal hesaba uygun

"double ile para tutma" gibi klasik tuzaklara düşmemiş.

### 2.4 Aggregate Root + State Machine
`FinancialEntry` aggregate root olarak `addPayment/removePayment`, `addAttachment/removeAttachment`, `submit / approveByCaptain / approveByManager / reject / recordPayment / reversePayment` metodlarıyla tüm yaşam döngüsünü kontrol ediyor. State geçişleri (DRAFT → PENDING_CAPTAIN → PENDING_MANAGER → APPROVED → PARTIALLY_PAID → PAID) entity içinde guard'larla korunmuş. Repository üzerinden `Payment` doğrudan kaydedilmiyor — child entity yaşam döngüsü doğru aggregate'e teslim edilmiş.

### 2.5 Audit & İzlenebilirlik
- Hibernate Envers + custom `CustomRevisionEntity` (user_id, username, source: API/SYSTEM/BATCH, correlation_id) — yalnızca "ne değişti" değil "kim, nasıl, hangi istekte" kayda alınmış.
- `@AuditOverride` ile audit gürültüsü alanları (version, deleted, updatedAt) dışarıda tutulmuş.
- Soft delete (`@Where(is_deleted = false)` + audit alanları).
- `@Version` (optimistic locking) tüm entity'lerde.
- Flyway migration'larıyla envers tablolarının açık tanımlı olması (Hibernate auto-DDL'e bırakılmamış).

### 2.6 Güvenlik Mimarisinin Sıralaması
```
RateLimitFilter (HIGHEST_PRECEDENCE)
  → JwtAuthenticationFilter
    → TenantFilter
      → TenantInterceptor
        → Controller
```
JWT içinde hem `sub` hem `tenantId` claim'i var ve `JwtAuthenticationFilter` JWT tenantId ile DB'deki `user.organizationId`'yi karşılaştırıyor — JWT manipülasyon saldırısına karşı doğru savunma. CSRF disabled (stateless JWT için doğru), HSTS / `frameOptions=deny` / `X-Content-Type-Options: nosniff` aktif. BCrypt password encoder.

### 2.7 RBAC Hiyerarşisi
`Role` enum'unun parent referansıyla hiyerarşik permission inheritance yapması (CREW → MANAGER → CAPTAIN → SUPER_ADMIN), `Permission` enum, double-checked locking ile lazy permission cache — temiz tasarım. `EntryAccessPolicy` ile policy'nin entity'den ayrılması da doğru ayrım.

### 2.8 Performans / Operasyon Tarafı
- HikariCP doğru ayarlanmış (pool=20 prod, 30s timeout).
- Hibernate `batch_size=20`, `order_inserts/updates`, `batch_versioned_data`.
- `spring.jpa.open-in-view=false` (lazy loading tuzağına düşmemiş — kritik).
- `ddl-auto=validate` (prod), Flyway baseline-on-migrate.
- Caffeine cache (exchange-rates).
- Actuator + Prometheus + Micrometer + tag'li metrik export.
- `@Async` task decorator'ı tenant-aware.
- Composite index'ler bilinçli kurulmuş (`tenant_id, entry_date, status` vs.).

### 2.9 Hata Yönetimi & Observability
`GlobalExceptionHandler` her hatada UUID `errorId` üretiyor, log'a koyuyor, response'a koyuyor — destek/debug akışı için altın standart. Domain exception (`EntryValidationException`) → field-level error map'leme yapılmış.

### 2.10 CQRS-İmsi Use Case Ayrımı
`application.usecase` paketinde `GenerateAnnualReport`, `GeneratePivotTree`, `GetCumulativeBalance` gibi okuma odaklı use case'ler ayrı dosyalar; `application.commands` paketinde yazma command'leri var. Bu raporlama yükünü CRUD service'inden ayırıyor.

---

## 3. Yanlış / Zayıf Kurgulananlar

### 3.1 Test Kapsamı Sahte (KRİTİK)
8 test sınıfından beş tanesi 4 satırlık iskelet (`FinancialEntryServiceTest`, `PaymentServiceTest`, `FinancialEntryRepositoryTest`, `MoneyTest`, `FinancialEntryTest` hepsi boş). Yalnızca `ApprovalServiceTest` (589 satır) ve `TestDataBuilder` (468 satır) gerçek. Yani aslında ~%95'i yazılmamış görünüyor. `Money` VO ve aggregate root gibi kritik domain davranışları sıfır test'le production'a gidiyor — bu boyutta bir SaaS için en büyük risk.

### 3.2 Domain → Application Bağımlılık İhlali (DDD ihlali)
`FinancialEntry` (domain entity) içinde:
```java
import com.marine.management.modules.finance.application.ExchangeRateService;
public void calculateBaseAmount(ExchangeRateService exchangeRateService, ...)
```
Domain katmanı application katmanını import ediyor. Hexagonal/Clean Architecture'da bağımlılık yönü tersine; domain bir port (interface) tanımlamalı, infrastructure adapter'ı implement etmeli. Bu satırlar bütün izolasyonu boşa çıkarıyor.

### 3.3 CORS Konfigürasyonu Tutarsız ve Tehlikeli
`application.properties`'te `cors.allowed-origins` env-var olarak tanımlı ama `SecurityConfig.corsConfigurationSource()` metodunda hard-coded `"http://localhost:4200"` listesi var. Yani prod'a deploy edilince env değişkeni kullanılmıyor — production'da Angular front-end CORS'tan dolayı kırılabilir veya geliştirici prod'da bunu fark etmeyip localhost origin'le canlıya çıkar. Dead config.

### 3.4 "God Entity" Anti-Pattern Riski
`FinancialEntry` 811 satır, 50+ alan, 30+ metot, 10 ilişki. Bir aggregate root olarak başlamış ama içine workflow, payment lifecycle, attachment lifecycle, exchange rate hesabı, validation (`validate()` 30 satır) — hepsi karışmış. `WorkflowService`, `PaymentRecorder` gibi domain service'lere bölünmeliydi. Tek dosyaya değişiklik yarışı (merge conflict) çıkarır.

### 3.5 TenantFilterAspect Çok Geniş Pointcut
```java
@within(@Service) || @within(@RestController) || @annotation(@Transactional)
```
Her servis metodu, her controller, her transactional metod için aspect tetikleniyor. İdempotent kontrol (`getEnabledFilter`) var ama performans yükü ve sessiz hata (catch-all) riski yüksek. Bunun yerine sadece `@Transactional` metodlarda veya `EntityManagerFactory` event'lerinde aktivasyon daha temiz olurdu. Üstelik bir AOP istisnası filter'i atlatırsa Hibernate filter aktif olmadan query çalışır — Specifications'ta da tenant clause yok ("filter halleder" varsayımıyla) — sızıntı riski.

### 3.6 SYSTEM Tenant Fallback Tehlikesi
`HibernateTenantIdentifierResolver.resolveCurrentTenantIdentifier()` context yoksa `"SYSTEM"` dönüyor. Ama `TenantEntityListener.@PrePersist` context yoksa `IllegalStateException` atıyor. İki kontrat tutarsız; bootstrap, scheduled job, error log path'leri arasında öngörülemez davranışa yol açabilir. `SYSTEM` tenant DB'de tanımlı değilse veya bir senaryoda bu fallback aktif olursa cross-tenant sızıntı kapısı.

### 3.7 Role Hierarchy Spring Security'ye Bildirilmemiş
`Role` enum'da hierarchy var ama `RoleHierarchy` bean tanımlı değil. Bu yüzden `SecurityConfig`'te:
```java
.requestMatchers("/api/finance/**").hasAnyRole("SUPER_ADMIN", "ADMIN", "MANAGER", "CAPTAIN", "CREW")
```
gibi her endpoint'te tüm rolleri elle saymak zorunda kalınmış. `RoleHierarchyImpl` bean'i ile `SUPER_ADMIN > CAPTAIN > MANAGER > CREW` tanımlanırsa endpoint policy'leri sadeleşir ve domain hiyerarşisiyle Spring Security senkron çalışır.

### 3.8 dev Profilinde Flyway KAPALI
`application-dev.properties`:
```properties
spring.flyway.enabled=false
spring.jpa.hibernate.ddl-auto=create-drop
```
Bu schema-drift kapısı. Geliştirici bilgisayarında çalışan migration prod'da aynı sırada uygulanmıyor → "bende çalışıyor" sendromu garantilenmiş. En azından bir `migration-test` profili veya dev'de Flyway'in açık olması gerekirdi.

### 3.9 JWT'de Yumuşak Noktalar
- Tek bir `jwt.secret`; rotasyon yok, JWK / `kid` yok.
- `userRepository.findByEmail(email)` global tablo unique index'ine güveniyor (yorumda kabul edilmiş). Çok-tenant SaaS'te aynı email'in farklı org'larda olabilmesi B2B'de istenen bir özelliktir; bu tasarım bunu engelliyor.
- Refresh token rotation/family detection açıkça görünmüyor; `RefreshTokenRepository` var ama "reuse detection" kontratı netleşmemiş.

### 3.10 Rate Limiting In-Memory
`RateLimitFilter` Caffeine ile single-instance. Yorum'da "Redis'e geçilecek" denmiş ama horizontal scale'e geçmeden önce production kullanılırsa attacker per-instance rate limit'i baypas eder. Ayrıca `X-Forwarded-For` spoofing'e karşı reverse proxy konfigürasyonu varsayımı sadece yorumda — runtime'da zorlanmıyor.

### 3.11 Modül İzolasyonu Sadece Paket Seviyesinde
"Modüler monolit" deniliyor ama Maven multi-module değil; Spring Modulith / jMolecules / ArchUnit yok. Yarın bir geliştirici `auth.application` içinden `finance.domain.entities.FinancialEntry`'yi import edebilir, derleme buna izin verir. Modül sınırlarını derleme zamanında zorlayan bir mekanizma eklenmeliydi.

### 3.12 Exchange Rate Cache TTL'i Şüpheli
`maximumSize=1000, expireAfterWrite=24h` — döviz kurları gün içi değişir; finansal raporlamada 24 saatlik cache yanlış kuru "doğru" gibi gösterir. En azından TTL kısa + per-date keying gerekir.

### 3.13 Validation Domain Entity'de + JPA Lifecycle
`@PrePersist`/`@PreUpdate`'te `validate()` `IllegalStateException` atıyor. Bu hata flush sırasında patlar (lazımken bir sürü transaction state çıkmıştır), client error mesajı korelasyonu zorlaşır. Validation application/service katmanında olsaydı 400 vs 500 ayrımı net olurdu.

### 3.14 Swagger/OpenAPI Dependency Yok
`SecurityConfig`'te `/swagger-ui/**` ve `/v3/api-docs/**` permitAll ama `pom.xml`'de `springdoc-openapi-starter-webmvc-ui` yok. API dokümantasyonu eksik.

### 3.15 N+1 Riski
`FinancialEntry` 4 `@OneToMany` koleksiyonu (attachments, payments, approvals) + 4 `@ManyToOne` ilişki içeriyor. Listeleme sorgularında `@EntityGraph` ya da fetch join kullanımı sınırlı görünüyor — büyük tenant'larda pagination'lı liste sayfasında patlar.

---

## 4. Ölçek Değerlendirmesi

**Sınıflandırma: Orta ölçekli B2B SaaS (alt-orta — küçükten orta'ya geçiş bandında).**

| Boyut | Değerlendirme |
|---|---|
| Kod hacmi | ~23K satır → Orta-küçük |
| Mimari karmaşıklık | Multi-tenant + DDD + Envers + RBAC + workflow + multi-currency → Orta-büyük |
| Domain karmaşıklığı | Yacht finance + onay zinciri + döviz + ödeme → Orta |
| Operasyonel olgunluk | Migration, observability, profile ayrımı, R2 → Orta |
| Deployment | Tek artifact (modüler monolit) → MVP/SMB |
| Dayanıklılık | Test eksik, RoleHierarchy yok, dev profili schema-drift'e açık → MVP-seviyesi |

### Karşılaştırmalı yer

- **Küçük (startup MVP):** Tek developer, monolit, basit auth → bu projeden daha hafif olurdu (3-5K satır, tek modül).
- **Orta (B2B SaaS):** Multi-tenant, modüler monolit, audit, RBAC, ~20-50K satır, küçük takım → **bu proje burada.** Hedef segment muhtemelen 50-500 yat işletmecisi.
- **Büyük (enterprise/scale-up):** Mikroservis ayrımı, schema-per-tenant veya Postgres RLS, distributed tracing (OpenTelemetry), event-driven (Kafka/Outbox), >%80 test coverage, JWK rotation, BFF pattern → bu proje buraya henüz hazır değil.

### Büyük ölçeğe geçiş için yapılması gerekenler (öncelik sırası)

1. Test kapsamını domain VO + aggregate düzeyinde %80'e çıkarmak (en kritik gap).
2. CORS hard-code'unu env-var'a çevirmek.
3. Domain → Application bağımlılığını port/adapter ile ters çevirmek.
4. RoleHierarchy bean'i tanımlamak, endpoint policy'lerini sadeleştirmek.
5. Rate limiting'i Redis (Bucket4j) ile cluster-aware yapmak.
6. dev profilinde Flyway'i zorunlu kılmak.
7. Spring Modulith / ArchUnit ile modül sınırlarını derleme-zamanında zorlamak.
8. Postgres Row-Level Security'ye geçiş düşünmek (Hibernate Filter'ın AOP bağımlılığını ortadan kaldırır).
9. JWT için key rotation + refresh token reuse detection.
10. `FinancialEntry`'yi domain service'lere bölmek (`PaymentRecorder`, `ApprovalWorkflow`, `ExchangeRateAdjuster`).

---

## 5. Tek Cümlelik Özet

Bu proje **iyi okumuş bir yazılımcının elinden çıkmış, mimari prensipleri ciddiye alan, "doğru iş yapma" niyeti net görülen orta ölçekli bir B2B SaaS monolitidir**; multi-tenant savunması ve domain modellemesi sınıfının üstünde, ancak test eksikliği, dev/prod profil tutarsızlıkları, küçük güvenlik konfigürasyon hataları (CORS, RoleHierarchy) ve domain → application sızıntısı production'a çıkmadan önce mutlaka kapatılması gereken olgun bir MVP'ye yakışır eksikliklerdir.
