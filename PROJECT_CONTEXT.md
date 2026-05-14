# Marine Management System - Project Documentation

**Version 2.1 — April 2026**
**Architecture: Modular Monolith + DDD Lite**

---

## 1. Project Overview and Purpose

The Marine Management System is a multi-tenant, web-based SaaS application designed to manage financial operations, personnel, and organizational data for yacht and marine vessel management companies. The system provides comprehensive financial tracking, multi-level approval workflows, and role-based access control for marine industry operations.

**Key Objectives:**
- Centralized financial entry management (expenses and income) with multi-currency support
- Multi-level approval workflows with role-based routing (Captain + Manager levels)
- Complete tenant isolation via Hibernate `@Filter` (zero cross-tenant data leakage)
- Role-based access control with fine-grained permissions
- Full audit trail via Hibernate Envers
- Clean, layered codebase with DDD tactical patterns and SOLID principles

---

## 2. Tech Stack

### Backend
- **Language:** Java (JDK 21)
- **Build Tool:** Maven
- **Framework:** Spring Boot 3.5.7
- **Architecture:** Modular Monolith + DDD Lite. Kod `modules/` altında bağımsız modüllere (auth, finance, users, organization, files) ayrılmış, her modül içinde `domain → application → infrastructure → presentation` katmanları var. Cross-cutting concerns `shared/` altında. DDD taktik pattern'leri kullanılıyor: Entity, Value Object, Domain Service, Factory Method.
- **Data Access:** Spring Data JPA + Hibernate 6.6
- **Security:** Spring Security + JWT (BCrypt password hashing)
- **Transaction Management:** Spring `@Transactional`
- **Audit:** Hibernate Envers (full entity change history)
- **Logging:** SLF4J with Logback
- **Cache:** Caffeine (max 1000 entries, 24h TTL)
- **Metrics:** Actuator + Prometheus (health, metrics, prometheus endpoints)

### Database
- **Type:** PostgreSQL
- **ORM:** JPA/Hibernate with Hibernate `@Filter` for multi-tenancy
- **Versioning:** Flyway (9 migration scripts: V001–V009)
- **Audit:** Hibernate Envers (revision tables per audited entity)
- **Connection Management:** HikariCP (Spring Boot default)

### Frontend
- **Framework:** Angular 20.0.0 (Standalone Components with new control flow: `@if`, `@for`, `@switch`)
- **Language:** TypeScript 5.8.2 (strict mode)
- **State Management:** Angular Signals (`signal()`, `computed()`, `effect()`) — **No NgRx**
- **Reactivity:** Angular 20 `resource()` API for async data loading with automatic caching
- **i18n:** `@jsverse/transloco 8.2.1` (Turkish/English, browser language auto-detection)
- **Styling:** Tailwind CSS 3.4.18 (glassmorphism design system)
- **Charts:** Chart.js 4.5.1
- **Export:** ExcelJS 4.4.0 + file-saver 2.0.5
- **HTTP:** Built-in HttpClient with custom interceptors (auth + error)

### Testing & Tooling
- **IDE:** IntelliJ IDEA 2025.3.3
- **OS:** macOS
- **Frontend Testing:** Jasmine 5.7.0 + Karma 6.4.0

---

## 3. User Roles and Tenant Structure

### 3.1 User Roles

The system implements a hierarchical role-based access control (RBAC) with the following roles:

**Inheritance chain:** `CREW → MANAGER → CAPTAIN`. Both `ADMIN` and `SUPER_ADMIN` inherit directly from `CAPTAIN` as siblings (i.e., both have `parent = CAPTAIN`).

| Role | Responsibilities | Approval Authority | Key Own Permissions |
|------|------------------|-------------------|---------------------|
| **SUPER_ADMIN** | Developer/system use only. Cross-tenant access | Full access across all tenants | All CAPTAIN permissions + SYSTEM_CONFIG, TENANT_CREATE, TENANT_DELETE, CROSS_TENANT_ACCESS |
| **ADMIN** | Alias for CAPTAIN — backward compatibility only. Functionally identical to CAPTAIN. | Same as CAPTAIN | Inherits all CAPTAIN permissions (empty own-permission set) |
| **CAPTAIN** | Financial oversight, primary approver, creates expense/income entries, crew supervision | Approves at any level. Own entries skip PENDING_CAPTAIN → direct to APPROVED or PENDING_MANAGER | All MANAGER permissions + ENTRY_EDIT_ALL, ENTRY_DELETE_ALL, ENTRY_APPROVE_CAPTAIN, INCOME_CREATE, INCOME_EDIT, INCOME_DELETE, PAYMENT_EDIT, PAYMENT_DELETE, USER_VIEW, USER_MANAGE, CATEGORY_MANAGE, TENANT_MANAGE |
| **MANAGER** | Views all entries, approves high-value entries. **CAN create and submit entries** (inherits CREW permissions via role hierarchy). | Approves PENDING_MANAGER only. Cannot approve PENDING_CAPTAIN. | All CREW permissions (incl. ENTRY_CREATE, ENTRY_SUBMIT) + ENTRY_VIEW_ALL, ENTRY_APPROVE_MANAGER, ENTRY_REJECT, INCOME_VIEW, PAYMENT_VIEW, PAYMENT_CREATE, REPORT_VIEW, REPORT_EXPORT |
| **CREW** | Creates expenses, submits for approval, basic data entry | None — submitting routes to PENDING_CAPTAIN | ENTRY_CREATE, ENTRY_VIEW_OWN, ENTRY_EDIT_OWN, ENTRY_DELETE_OWN, ENTRY_SUBMIT, CATEGORY_VIEW |

### 3.2 Multi-Tenancy Model

- **Tenant Entity:** `Organization` — represents a yacht/vessel company
- **Tenant Isolation:** Hibernate `@Filter(name="tenantFilter", condition="tenant_id = :tenantId")` on `BaseTenantEntity` — automatic, no manual WHERE clauses needed
- **Filter Activation:** `TenantFilter` (servlet `@Component`) activates the Hibernate filter per request after JWT authentication
- **Tenant Context:** `TenantContext` (request-scoped ThreadLocal) set by `JwtAuthenticationFilter`
- **Data Segregation:** All financial data, users, and categories are tenant-specific
- **Cross-Tenant Validation:** All domain entities validate tenant consistency on save

**Key Tenant Configuration Properties:**
- `yachtName`: Unique identifier for the organization
- `flagCountry`: Registration country (ISO 3166-1 alpha-2, e.g., "TR")
- `baseCurrency`: Financial base currency (ISO 4217, e.g., "EUR")
- `managerApprovalEnabled` (boolean): When false, Captain approves everything directly. When true, high-value entries route to Manager.
- `approvalLimit` (BigDecimal): Threshold in base currency (EUR). Entries exceeding this require Manager approval when `managerApprovalEnabled = true`. If null, no limit applies.

---

## 4. Permission and Security Model

### 4.1 Authentication & Authorization

- **JWT Tokens:** Access token (short-lived, 1 hour) + Refresh token (long-lived, 7 days)
- **Password Storage:** BCrypt via Spring `PasswordEncoder`
- **Session Context:** `JwtAuthenticationFilter` sets both SecurityContext and TenantContext per request
- **Role-Based Access:** Custom `Role` enum with `Permission` set per role (hierarchical inheritance)
- **Authorization Layer:** `EntryAccessPolicy` component for all data access decisions
- **Tenant Verification:** All operations verify `user.organization_id = TenantContext.tenantId`
- **Permissions to Frontend:** Login response includes `permissions[]` (all permission names for the user's role). Frontend stores as `Set<string>` and checks against them — zero hardcoded role-to-permission mapping in Angular.

### 4.2 Authorization Layers (in order)

| # | Layer | Implementation |
|---|-------|---------------|
| 1 | Tenant Isolation | Hibernate `@Filter` on `tenant_id`. Enabled per-request by `TenantFilter` (servlet filter, runs after `JwtAuthenticationFilter`) |
| 2 | Endpoint Security | `SecurityConfig` rules (`hasAnyRole`, `hasAuthority`) + `@PreAuthorize` on controller methods |
| 3 | Permission Check | `EntryAccessPolicy` checks `Role.hasPermission()` before any operation |
| 4 | Ownership Check | `createdById` comparison for Crew/Manager users (ensures users only access own entries when applicable) |
| 5 | Status Guard | Entity methods enforce valid transitions (e.g., `submit()` throws if not DRAFT) |

### 4.3 Access Control Rules

#### Entry Read Access
- `ENTRY_VIEW_ALL` (Manager/Captain/Admin): Can view all expenses in organization
- `ENTRY_VIEW_OWN` (Crew): Can view only own entries (checked by `EntryAccessPolicy.checkReadAccess()`)
- `INCOME_VIEW` (Manager/Captain/Admin): Can view all income entries

#### Dashboard Access (Crew-Filtered View)
Crew members can access the dashboard (`/dashboard` route), but all data is automatically scoped to their own entries. This filtering happens entirely in the backend — the frontend requires no changes.

- **Manager / Captain / Admin:** See full organization-wide data (totals, charts, trends across all entries and all users)
- **Crew:** See only their own entries' data — expense totals, category breakdown, monthly trend, and entry status counts are all filtered to `createdById = currentUser.id`
- **Income data for Crew:** `GET /api/finance/dashboard/income-totals` returns an empty list for CREW role (crew has no income entries and no `INCOME_VIEW` permission)

**Implementation:** `DashboardController` injects `@AuthenticationPrincipal User currentUser` on each dashboard endpoint. A private `resolveCrewFilter()` helper returns `currentUser.getId()` if the role is `CREW`, otherwise `null`. The `null` value propagates through `FinancialReportService` to the repository layer where `(:crewMemberId IS NULL OR e.createdById = :crewMemberId)` conditions apply. All existing non-crew calls remain unchanged.

#### Entry Write Access
- `ENTRY_EDIT_ALL` (Captain/Admin): Can edit any non-final expense
- `ENTRY_EDIT_OWN` (Crew/Manager): Can edit own DRAFT entries only
- `INCOME_EDIT` (Captain/Admin only): Can edit income entries. **Manager does NOT have this permission.**

#### Approval Access
- `ENTRY_APPROVE_CAPTAIN` (Captain only): Approve PENDING_CAPTAIN → APPROVED or PENDING_MANAGER
- `ENTRY_APPROVE_MANAGER` (Manager OR Captain): Approve PENDING_MANAGER → APPROVED

#### Rejection Access
- PENDING_CAPTAIN rejection: requires `ENTRY_APPROVE_CAPTAIN` (Captain/Admin only)
- PENDING_MANAGER rejection: requires `ENTRY_REJECT` + `ENTRY_APPROVE_MANAGER` (Manager and Captain both qualify)

#### Deletion
- `ENTRY_DELETE_OWN` (Crew/Manager): Delete own DRAFT entries only
- `ENTRY_DELETE_ALL` (Captain/Admin): Delete any DRAFT entry

#### SecurityConfig Rules (HTTP layer)
- `GET /api/auth/**`, `/api/onboarding/**`, `/swagger-ui/**`, `/v3/api-docs/**`, `/actuator/health` → `permitAll()`
- `GET /api/admin/**` → `hasRole("ADMIN")`
- `/api/users/**` → `hasAnyRole("SUPER_ADMIN", "ADMIN", "MANAGER", "CAPTAIN")`
- `/api/finance/**` → `hasAnyRole("SUPER_ADMIN", "ADMIN", "MANAGER", "CAPTAIN", "CREW")`
- `/api/reports/**` → `hasAnyRole("SUPER_ADMIN", "ADMIN", "MANAGER", "CAPTAIN")` *(note: report controller base path is `/api/finance/reports/`, not `/api/reports/`)*
- `anyRequest()` → `authenticated()`

---

## 5. Core Business Rules

### 5.1 Entry Status Lifecycle

**Status Flow:**
```
DRAFT
  ↓ (submit — routed by ApprovalService.routeByRole())
  ├─ Crew submit → PENDING_CAPTAIN
  ├─ Manager submit → PENDING_CAPTAIN
  ├─ Captain submit (≤ limit OR !managerEnabled) → APPROVED
  ├─ Captain submit (> limit AND managerEnabled) → PENDING_MANAGER
  └─ Admin submit → APPROVED

PENDING_CAPTAIN
  ├─ Captain approve (≤ limit OR !managerEnabled) → APPROVED
  ├─ Captain approve (> limit AND managerEnabled) → PENDING_MANAGER
  ├─ Captain reject → REJECTED
  └─ (Only Captain/Admin can act)

PENDING_MANAGER
  ├─ Manager/Captain approve → APPROVED
  ├─ Manager/Captain reject → REJECTED
  └─ (Both Manager and Captain can act)

APPROVED
  ├─ Record partial payment → PARTIALLY_PAID
  └─ Record full payment → PAID

PARTIALLY_PAID
  └─ Record final payment → PAID

PAID → Final state
REJECTED → Final state (no resubmit — user must create a new entry)
```

**Important Business Rules:**
- **Manager CAN create entries.** Manager inherits `ENTRY_CREATE` and `ENTRY_SUBMIT` from CREW. When Manager submits, the entry routes to `PENDING_CAPTAIN` (same default path as CREW).
- **Captain creates entries** and submits them. Captain's own entries skip PENDING_CAPTAIN (self-approval is meaningless).
- **REJECTED is final.** If an entry is rejected, the user must create a new entry. There is no resubmit/reopen flow.
- **Income is Captain-only.** Only Captain (and Admin/SuperAdmin) can create income entries via `INCOME_CREATE`. Crew cannot create incomes. Income entries do not go through approval — they are auto-approved on submit since Captain is already the financial authority.

### 5.2 Submit Routing Rules (ApprovalService.routeByRole)

This is the core routing logic. When a user clicks "Submit", `ApprovalService.routeByRole()` determines the target status:

| Submitter | Condition | Target Status | Entity Method |
|-----------|-----------|---------------|--------------|
| **CREW** | Always | PENDING_CAPTAIN | `entry.submit()` |
| **MANAGER** | Always (default case) | PENDING_CAPTAIN | `entry.submit()` |
| **CAPTAIN** | amount ≤ limit OR !managerEnabled | APPROVED | `entry.submitAndApprove()` |
| **CAPTAIN** | amount > limit AND managerEnabled | PENDING_MANAGER | `entry.submitToManager()` |
| **ADMIN** | Always (bypass) | APPROVED | `entry.submitAndApprove()` |

**Important:** Captain's own entries NEVER go to PENDING_CAPTAIN — it would be meaningless for Captain to approve their own submission.

### 5.3 Approval Rules

| Current Status | Approver | Condition | Result |
|---------------|----------|-----------|--------|
| PENDING_CAPTAIN | Captain/Admin | amount ≤ limit OR !managerEnabled | APPROVED |
| PENDING_CAPTAIN | Captain/Admin | amount > limit AND managerEnabled | PENDING_MANAGER |
| PENDING_MANAGER | Manager or Captain | Always | APPROVED |

`isManagerApprovalRequired()` logic:
```
if (!tenant.isManagerApprovalEnabled()) → false
if (tenant.approvalLimit == null) → false
return entry.baseAmount > tenant.approvalLimit
```

### 5.4 Pending List Rules (EntryAccessPolicy.getPendingSpecification)

Each role sees different entries in their Pending Actions view:

| Role (Permission Check) | Sees | Purpose |
|-------------------------|------|---------|
| **Captain/Admin** (`ENTRY_APPROVE_CAPTAIN`) | All DRAFT + PENDING_CAPTAIN + PENDING_MANAGER | Full visibility — Captain oversees everything |
| **Manager** (`ENTRY_APPROVE_MANAGER`) | PENDING_MANAGER entries only | High-value entries awaiting their approval |
| **Crew** (`ENTRY_SUBMIT`) | Own DRAFT entries only | Reminder to submit unfinished entries |

> **Note:** Manager's own DRAFT entries are NOT shown in their Pending view (Manager hits the `ENTRY_APPROVE_MANAGER` branch). DRAFTs are visible via the expense/income list (Manager has `ENTRY_VIEW_ALL`).

### 5.5 Amount Calculations

- **Original Amount:** Immutable requested amount in original currency
- **Base Amount (EUR):** Calculated using exchange rate at entry date. If original = EUR, no conversion.
- **Approved Amount:** Set when entry is approved (`approvedBaseAmount`). null until approval.
- **Paid Amount:** Cumulative payment. Incremental only. Cannot exceed approved amount.
- **Remaining Amount:** `approvedBaseAmount - paidBaseAmount` (calculated in `EntryResponseDto.from()`)

**Validation Rules:**
- Paid amount cannot exceed approved amount
- Entry amount must be positive
- Category must be active (`is_enabled = true`)
- All linked objects (category, WHO, main category) must belong to same tenant

### 5.6 Exchange Rate Handling

- **Rate Date:** Captured at entry creation (entry date)
- **Conversion:** `baseAmount = originalAmount × rate`
- **Updates:** Exchange rate can be recalculated manually via `PATCH /api/finance/entries/{id}/exchange-rate`
- **Service:** `ExchangeRateService` — implementation details (API source, caching, fallback strategy) [UNKNOWN/NOT_FOUND_IN_CODE]
- **Error Handling:** `ExchangeRateCalculationException` if rate unavailable

### 5.7 Payment Rules

- Only entries in `APPROVED` or `PARTIALLY_PAID` status can receive payments (`isPayable()`)
- `recordPayment()` adds to `paidBaseAmount` and updates entry status via cascade
- `recordFullPayment()` uses `getRemainingAmount()` — throws if already fully paid
- Payment deletion reverses the transaction (`entry.removePayment()` → reverses paidBaseAmount + status)
- Payment edits only update metadata (reference, method, notes) — amount and date are immutable after recording

---

## 6. Domain Design

### 6.0 Aggregate Root: FinancialEntry

`FinancialEntry` bu modülün Aggregate Root'u. `Payment`, `FinancialEntryAttachment` ve `EntryApproval` tek başlarına anlam taşımaz — hepsi `FinancialEntry` bağlamında var olur ve onun yaşam döngüsüne tabidir.

**Cascade kuralları:**
- `Payment` → `CascadeType.ALL` + `orphanRemoval = true`. Entry yaşarken payment eklenip çıkarılabilir.
- `FinancialEntryAttachment` → `CascadeType.ALL` + `orphanRemoval = true`.
- `EntryApproval` → `CascadeType.PERSIST` + `CascadeType.MERGE`. Kalıcı audit trail, fiziksel olarak silinmez.

**Soft Delete:**
Silme işlemi fiziksel değil. `BaseAuditedEntity`'de `is_deleted`, `deleted_at`, `deleted_by_id` field'ları mevcut. Hibernate `@Where(clause = "is_deleted = false")` anotasyonu tüm query'lere otomatik uygulanır — her zaman aktiftir, `enableFilter()` gibi manuel çağrı gerekmez.

Entry soft delete olduğunda child entity'lere (`Payment`, `Attachment`, `Approval`) ayrıca `deleted` flag koymaya gerek yok — entry'ye erişilemeyen child'lara da erişilemez. Aggregate Root'un doğal koruması.

`CascadeType.REMOVE` hiçbir yerde kullanılmaz — fiziksel silme yoktur.

> **Not:** `@Where` Hibernate 6'da deprecated, yerine `@SQLRestriction` önerilmektedir. Mevcut kod çalışmaya devam eder ancak ileride geçiş yapılabilir.

**FinancialEntry Business Methods:**
- `submit()` → DRAFT → PENDING_CAPTAIN
- `submitToManager()` → DRAFT → PENDING_MANAGER
- `submitAndApprove()` → DRAFT → APPROVED
- `approveByCaptain(boolean managerRequired)` → PENDING_CAPTAIN → APPROVED or PENDING_MANAGER
- `approveByManager()` → PENDING_MANAGER → APPROVED
- `reject(String reason)` → PENDING_* → REJECTED
- `recordPayment(Money)` / `reversePayment(Money)` — updates paidBaseAmount + status
- `addPayment(Payment)` / `removePayment(Payment)` — cascade-managed collection
- `addAttachment()` / `removeAttachment()` — cascade-managed collection
- `getRemainingAmount()` — calculated: approvedBaseAmount - paidBaseAmount
- `hasAttachments()`, `isIncome()`, `isExpense()`, `isDraft()`, `isFullyPaid()`, `isPartiallyPaid()`

**PaymentRepository:**
Mutation için kullanılmaz — `Payment` ekleme/silme işlemleri `FinancialEntry.addPayment()` / `removePayment()` üzerinden yapılır. Repository sadece read query'ler için tutulur.

**Audit:**
Hibernate Envers tüm entity değişikliklerini otomatik kayıt altına alıyor. Soft delete bir `UPDATE` (`deleted = true`) olarak Envers tarafından da loglanır.

**FinancialEntryAttachment:**
`BaseTenantEntity`'den değil doğrudan temel entity'den extends eder — tenant izolasyonu parent `FinancialEntry` üzerinden sağlanır. Fields: `fileName`, `originalFileName`, `filePath`, `fileSize`, `contentType`, `attachmentType` (AttachmentType enum), `uploadedBy`, `uploadedAt`.

**EntryApproval:**
`BaseTenantEntity`'yi extend eder. Fields: `approvalLevel`, `approvalStatus`, `requestedAmount`, `approvedAmount`, `rejectionReason`, `approver`, `approvalDate`. Factory: `createPending()`. Supports partial approval via `approvePartialAmount()`.

---

## 7. Data Model

### 7.1 EntryResponseDto (Primary API Response)

| Field | Type | Notes |
|-------|------|-------|
| `id` | UUID | Primary key |
| `entryNumber` | String | Formatted: FE-2026-001 |
| `status` | EntryStatus | DRAFT, PENDING_CAPTAIN, PENDING_MANAGER, APPROVED, REJECTED, PARTIALLY_PAID, PAID |
| `entryType` | RecordType | EXPENSE or INCOME |
| `categoryId` | UUID | Financial category reference |
| `categoryName` | String | Category display name |
| `originalAmount` | MoneyDto | Immutable. Original request (amount String + currency String) |
| `baseAmount` | MoneyDto | EUR equivalent via exchange rate |
| `approvedBaseAmount` | MoneyDto | null until approved |
| `paidBaseAmount` | MoneyDto | null until first payment. Cumulative |
| `remainingAmount` | MoneyDto | Calculated: approved - paid. null until approved |
| `exchangeRate` | BigDecimal | Rate used for base amount calculation |
| `exchangeRateDate` | LocalDate | Date of exchange rate capture |
| `receiptNumber` | String | Optional receipt reference |
| `description` | String | Free-text description |
| `entryDate` | LocalDate | Date of the transaction |
| `paymentMethod` | PaymentMethod | CASH, BANK_TRANSFER, CREDIT_CARD, DEBIT_CARD |
| `whoId` | Long | TenantWhoSelection reference (global numeric ID) |
| `mainCategoryId` | Long | TenantMainCategory reference (global numeric ID) |
| `recipient` | String | Counterparty/recipient name |
| `country` | String | Location: country |
| `city` | String | Location: city |
| `specificLocation` | String | Location: specific place |
| `vendor` | String | Vendor/supplier name |
| `createdById` | UUID | User who created the entry. **IMPORTANT: JSON serializes as `createdById`, NOT `createdBy`** |
| `createdByName` | String | Display name. Populated via `fromWithUser()`. null in `from()` (no user lookup) |
| `createdAt` | LocalDateTime | Creation timestamp |
| `updatedAt` | LocalDateTime | Last update timestamp |
| `hasAttachments` | Boolean | Whether entry has attached files |

**CRITICAL Frontend Note:** The field is `createdById` (UUID). Frontend `EntryResponse` model MUST use `createdById` — not `createdBy`. A mismatch causes `EntryPermissionService.isOwner()` to always return false, hiding edit/delete buttons for Crew/Manager users on their own entries.

**MoneyDto:** `{ amount: String (plain decimal), currency: String (ISO 4217) }`

**Factory Methods:**
- `EntryResponseDto.from(FinancialEntry)` → createdByName = null
- `EntryResponseDto.fromWithUser(FinancialEntry, String createdByName)` → full DTO

### 7.2 Core Database Tables

#### `organizations`
- Primary key: `id` (BIGSERIAL)
- Key fields: `yacht_name` (unique), `company_name`, `flag_country`, `base_currency`
- Yacht info: `yacht_type`, `yacht_length`, `home_marina`, `current_location`
- Config: `timezone` (default: "Europe/Istanbul"), `financial_year_start_month` (default: 1)
- Approval config: `manager_approval_enabled` (BOOLEAN), `approval_limit` (NUMERIC 15,2)
- Subscription: `subscription_status`, `subscription_expires_at`
- Flags: `active` (BOOLEAN), `onboarding_completed` (BOOLEAN)

#### `users`
- Primary key: `id` (UUID)
- Key fields: `email` (unique), `first_name`, `last_name`, `password_hash`, `role`
- Status: `is_active` (BOOLEAN), `email_verified` (BOOLEAN), `last_login_at`
- Tokens: `verification_token`, `verification_token_expires_at`, `password_reset_token`, `password_reset_token_expires_at`
- FK: `organization_id` → organizations

#### `financial_entries` ⭐ Core Table
- Primary key: `id` (UUID)
- Status: `status` VARCHAR(20) — DRAFT, PENDING_CAPTAIN, PENDING_MANAGER, APPROVED, REJECTED, PARTIALLY_PAID, PAID
- Amounts: `original_amount`/`original_currency` (immutable), `base_amount`/`base_currency` (EUR), `approved_base_amount`/`approved_base_currency`, `paid_base_amount`/`paid_base_currency`
- Context: `category_id`, `tenant_who_id`, `tenant_main_category_id`
- Location: `country`, `city`, `specific_location`, `vendor`, `recipient`
- Extra: `frequency`, `priority`, `tags`, `receipt_number`, `exchange_rate`, `exchange_rate_date`, `rejection_reason`
- Audit: `created_by_id`, `updated_by_id` (via AuditingEntityListener)
- Soft delete: `is_deleted`, `deleted_at`, `deleted_by_id`
- Key indexes: `(tenant_id, status)`, `(tenant_id, entry_date, status)`, `(tenant_id, created_by_id)`

#### `financial_categories`
- Primary key: `id` (UUID)
- Key fields: `name`, `category_type` (EXPENSE/INCOME), `is_technical` (BOOLEAN), `is_enabled` (BOOLEAN), `display_order`
- Tenant-scoped via `tenant_id`. Unique constraint: `(tenant_id, name)`
- Soft delete inherited from BaseAuditedEntity

#### `financial_entry_attachments`
- Primary key: `id` (UUID)
- FK: `entry_id` → financial_entries
- Fields: `file_name`, `original_file_name`, `file_path`, `file_size`, `content_type`, `attachment_type` (FATURA/FIS/PROMO/DIGER), `uploaded_by_id`, `uploaded_at`

#### `payments`
- Primary key: `id` (UUID)
- FK: `entry_id` → financial_entries
- Fields: `amount`/`currency`, `payment_date`, `payment_reference`, `payment_method`, `notes`, `recorded_by_id`, `recorded_at`
- Amount and date are immutable after recording

#### `entry_approvals`
- Primary key: `id` (UUID)
- FK: `entry_id` → financial_entries
- Fields: `approval_level` (CAPTAIN/MANAGER), `approval_status` (PENDING/APPROVED/PARTIAL/REJECTED), `requested_amount`/`requested_currency`, `approved_amount`/`approved_currency`, `rejection_reason`, `approver_id`, `approval_date`

---

## 8. Frontend Architecture

### 8.1 Module Structure and Routing

**Public Routes:** (no auth required)

| Route | Component |
|-------|-----------|
| `/login` | Login |
| `/register` | Register |
| `/verify-email` | VerifyEmail |
| `/forgot-password` | ForgotPassword |
| `/reset-password` | ResetPassword |
| `/access-denied` | AccessDenied |

**Protected Routes:** (under Layout wrapper with `authGuard`)

| Route | Component | Required Permission (OR logic) |
|-------|-----------|-------------------------------|
| `/setup` | Setup (onboarding) | authGuard only — no permissionGuard |
| `/dashboard` | Dashboard | — (all authenticated) |
| `/expenses` | ExpenseList | ENTRY_CREATE \| ENTRY_VIEW_OWN \| ENTRY_VIEW_ALL |
| `/expense/new` | ExpenseForm | ENTRY_CREATE |
| `/expense/:id/edit` | ExpenseForm | ENTRY_EDIT_OWN \| ENTRY_EDIT_ALL |
| `/action-center` | ActionCenter | ENTRY_SUBMIT |
| `/incomes` | IncomeList | INCOME_VIEW |
| `/categories` | CategoryList | CATEGORY_MANAGE |
| `/users` | UserList | USER_MANAGE |
| `/reports` | Reports | REPORT_VIEW |
| `/tree` | TreeReport | REPORT_VIEW |
| `/pivot-tree` | PivotTreeReport | REPORT_VIEW |
| `/data-import` | DataImport | ENTRY_EDIT_ALL |

**Lazy Loading:** All components loaded via `loadComponent()` on demand.

**Auth Guard Logic:** Checks `isAuthenticated`. If not → `/login?returnUrl=...`. If authenticated but `onboardingCompleted = false` → `/setup`.

### 8.2 Core Module (src/app/core/)
- **guards/:** `authGuard` (checks isAuthenticated on Layout wrapper), `permissionGuard` (checks specific permissions per route, OR logic)
- **interceptors/:** `authInterceptor` (adds Bearer token + Accept-Language header, handles 401 refresh with request queuing), `errorInterceptor` (global error standardization)
- **services:** `AuthService` (signals-based auth state), `StorageService` (localStorage/sessionStorage persistence), `NotificationService` (toast notifications), `EntryPermissionService` (root singleton, UI permission mirror)
- **i18n:** `LanguageService` (language switching), `TranslocoLoader` (translation file loader)
- **shared/services:** `TenantContextService` (multi-tenant context signal)

### 8.3 State Management: Signals-Based Stores

Each feature has a dedicated store using Angular Signals. No NgRx dependency. `EntryService` uses the `resource()` API for reactive data loading tied to tenant context.

| Store | Scope | Key Signals |
|-------|-------|-------------|
| `ExpenseListStore` | Component-level | entries, filters, pagination, bulk selection |
| `ExpenseFormStore` | Component-level | form, isEditMode, isFormReadOnly, canSubmitForApproval |
| `ActionCenterStore` | Component-level | approvalEntries, paymentEntries, activeTab (approvals/payments), bulk selection, reject modal |
| `PendingApprovalsStore` | Component-level | pendingEntries grouped by status |
| `ExpenseDetailModalStore` | Component-level | modal state, entry detail |
| `AttachmentStore` | Component-level | attachment list, upload state |
| `TreeReportStore` | Component-level | tree data, filters, expanded nodes |
| `PivotTreeReportStore` | Component-level | pivot data, year/currency filters |
| `EntryPermissionService` | Root (singleton) | canEdit, canDelete, canSubmit, canApprove, canReject (reactive computed signals) |

### 8.4 EntryPermissionService (Frontend Permission Mirror)

Frontend mirror of backend `EntryAccessPolicy`. Reads permissions from `AuthService.permissions()` (Set<string> from login response). **Backend remains the ultimate authority** — this service only prevents unnecessary UI actions.

| Method | Rules (from actual code) |
|--------|--------------------------|
| `can(permission)` | Checks `authService.permissions().has(permission)` — single source of truth |
| `canEdit(entry)` | `ENTRY_EDIT_ALL` → any non-final. `ENTRY_EDIT_OWN` → own DRAFT only |
| `canDelete(entry)` | DRAFT only. `ENTRY_DELETE_ALL` → any. `ENTRY_DELETE_OWN` → own only |
| `canSubmit(entry)` | DRAFT only, requires `ENTRY_SUBMIT`, AND (isOwner OR `ENTRY_EDIT_ALL`) |
| `canApprove(entry)` | PENDING_CAPTAIN → `ENTRY_APPROVE_CAPTAIN`. PENDING_MANAGER → `ENTRY_APPROVE_MANAGER` |
| `canReject(entry)` | Same rules as `canApprove()` |
| `canRecordPayment(entry)` | Requires `PAYMENT_CREATE` AND status in (APPROVED, PARTIALLY_PAID) |
| `isReadOnly(entry)` | `!canEdit(entry)` |
| `isOwner(entry)` | `entry.createdById === currentUser.id` |

**Navigation computed signals:** `canViewReports`, `canExportReports`, `canViewIncomes`, `canManagePayments`, `canManageCategories`, `canManageUsers`, `hasElevatedAccess` (ENTRY_VIEW_ALL).

### 8.5 Form Button Visibility Logic

| Entry State | Visible Buttons | Controlled By |
|-------------|----------------|---------------|
| New entry (no ID) | Save as Draft + Save & Submit | Always visible |
| DRAFT (own) | Save as Draft + Save & Submit | `canSaveAsDraft()` + `canShowSubmitButton()` |
| PENDING_* (Captain edit) | Save only | `canEdit()` true, `canShowSubmitButton()` false |
| Read-only entry | No buttons | `isReadOnly()` = true |

### 8.6 Auth Flow

1. User enters credentials → `AuthService.login(credentials, persistent)`
2. `POST /api/auth/login` → Receive `{ user, accessToken, refreshToken, permissions[], onboardingCompleted }`
3. Update signals: `_currentUser`, `_token`, `_refreshToken`, `_permissions`, `_onboardingCompleted`
4. Persist: `localStorage` (persistent) or `sessionStorage` (session-only)
5. Navigate to `/dashboard` (or `/setup` if onboarding incomplete)

**Token Refresh:** `authInterceptor` catches 401 → calls `POST /api/auth/refresh` → retries queued requests. If refresh fails → `logout()` + redirect to `/login`.

---

## 9. API Endpoints Summary

> **Base prefix:** All finance endpoints are under `/api/finance/`. Auth endpoints under `/api/auth/`. User endpoints under `/api/users/`. Onboarding under `/api/onboarding/`.

### Entry Management (`/api/finance/entries`)
```
POST   /api/finance/entries                              Create entry (DRAFT) — ENTRY_CREATE
GET    /api/finance/entries/{id}                         Get entry by ID
GET    /api/finance/entries/number/{entryNumber}         Get entry by formatted number (e.g. FE-2026-001)
GET    /api/finance/entries/expenses/search              List expenses (paginated, filtered)
GET    /api/finance/entries/incomes/search               List incomes (paginated, filtered) — INCOME_VIEW
GET    /api/finance/entries/search                       List all entries (paginated, filtered)
GET    /api/finance/entries/status/{status}              List entries by status — ENTRY_VIEW_ALL
GET    /api/finance/entries/status/count/{status}        Count entries by status — ENTRY_VIEW_ALL
PUT    /api/finance/entries/{id}                         Full update of entry
PATCH  /api/finance/entries/{id}/context                 Update WHO + MainCategory + location fields
PATCH  /api/finance/entries/{id}/metadata                Update metadata fields (frequency, priority, tags)
PATCH  /api/finance/entries/{id}/receipt-number          Update receipt number
PATCH  /api/finance/entries/{id}/exchange-rate           Recalculate exchange rate
DELETE /api/finance/entries/{id}                         Soft-delete entry (DRAFT only)
GET    /api/finance/entries/{id}/history                 Unified entry history timeline (approvals + revisions)
GET    /api/finance/entries/capabilities                 Get current user's capability flags
```

**Search query parameters:** `page`, `size`, `sortColumn` (entryDate/amount/createdAt), `sortDirection` (asc/desc), `searchTerm`, `categoryId`, `entryType`, `whoId`, `mainCategoryId`, `startDate`, `endDate`, `status`

### Approval (`/api/finance/entries`)
```
GET  /api/finance/entries/pending                    Pending items for current user (role-filtered)
GET  /api/finance/entries/pending/count              Pending count for dashboard badge
POST /api/finance/entries/{id}/submit                Submit (role-based routing) — ENTRY_SUBMIT
POST /api/finance/entries/{id}/approve               Approve at current level (auto-detects PENDING_CAPTAIN or PENDING_MANAGER)
POST /api/finance/entries/{id}/reject                Reject (reason required)
POST /api/finance/entries/bulk/approve               Bulk approve entries
```

> **Note:** There is a single `/approve` endpoint — backend auto-detects entry's current status (PENDING_CAPTAIN or PENDING_MANAGER) and applies appropriate approval. No separate `/approve/captain` or `/approve/manager` endpoints.

### Attachments (`/api/finance/entries`)
```
POST   /api/finance/entries/{id}/attachments                          Upload single attachment
POST   /api/finance/entries/{id}/attachments/bulk                     Upload multiple attachments
GET    /api/finance/entries/{id}/attachments                          List attachments for entry
GET    /api/finance/entries/{id}/attachments/{attachmentId}/download  Get presigned download URL (Cloudflare R2)
DELETE /api/finance/entries/{id}/attachments/{attachmentId}           Delete attachment
```

### Payments (`/api/finance/entries`)
```
POST   /api/finance/entries/{id}/payments               Record partial payment — PAYMENT_CREATE
POST   /api/finance/entries/{id}/payments/full          Record full payment (marks as PAID) — PAYMENT_CREATE
GET    /api/finance/entries/{id}/payments               List payments for entry
GET    /api/finance/entries/{id}/payments/summary       Payment summary (approved, paid, remaining)
GET    /api/finance/entries/payments/recent             Recent payments (last 30 days) — PAYMENT_VIEW
GET    /api/finance/entries/payments/by-date            Payments filtered by date range — PAYMENT_VIEW
PATCH  /api/finance/entries/payments/{paymentId}        Update payment metadata — PAYMENT_EDIT
DELETE /api/finance/entries/payments/{paymentId}        Delete/reverse payment — PAYMENT_DELETE
```

### Dashboard (`/api/finance/dashboard`)

All dashboard endpoints are accessible by all authenticated roles (including CREW). Data is automatically scoped server-side: CREW users receive only their own entries; Manager/Captain/Admin receive full organization data.

```
GET /api/finance/dashboard/summary                  Financial summary — CREW-filtered automatically
GET /api/finance/dashboard/period-totals            Totals for a given period — CREW-filtered automatically
GET /api/finance/dashboard/category-totals          Totals grouped by category (entryType required)
GET /api/finance/dashboard/expense-totals           Expense totals by category — CREW-filtered automatically
GET /api/finance/dashboard/income-totals            Income totals by category — returns [] for CREW
GET /api/finance/dashboard/monthly-totals           Monthly breakdown
GET /api/finance/dashboard/cumulative-balance       Cumulative balance (optional date range)
GET /api/finance/dashboard/cumulative-balance/complete  Full cumulative balance dataset (required date range)
```

### Reports (`/api/finance/reports`)
```
GET  /api/finance/reports/annual-breakdown/{year}   Annual breakdown by category
GET  /api/finance/reports/period-breakdown          Period breakdown (startDate + endDate required)
POST /api/finance/reports/expense-tree              Expense hierarchical tree report
POST /api/finance/reports/income-tree               Income hierarchical tree report
GET  /api/finance/reports/expense-tree-pivot        Expense pivot tree by year (year + currency params)
GET  /api/finance/reports/income-tree-pivot         Income pivot tree by year (year + currency params)
```

### Categories (`/api/finance/categories`)
```
POST   /api/finance/categories                      Create category — hasRole("ADMIN")
GET    /api/finance/categories                      List categories (activeOnly param, default false)
GET    /api/finance/categories/{id}                 Get category by ID
GET    /api/finance/categories/by-type              Categories filtered by entryType + usage date
GET    /api/finance/categories/search               Search categories by keyword
GET    /api/finance/categories/validate/name        Validate category name uniqueness
PUT    /api/finance/categories/{id}                 Update category — hasRole("ADMIN")
PATCH  /api/finance/categories/{id}/display-order   Update display order — hasRole("ADMIN")
PATCH  /api/finance/categories/{id}/activate        Activate category — hasRole("ADMIN")
PATCH  /api/finance/categories/{id}/deactivate      Deactivate category — hasRole("ADMIN")
DELETE /api/finance/categories/{id}                 Delete category — hasRole("ADMIN")
```

### Reference Data (`/api/finance/reference`)
```
GET /api/finance/reference/main-categories              All main categories for tenant
GET /api/finance/reference/main-categories/{id}         Main category by ID
GET /api/finance/reference/main-categories/by-type      Main categories filtered by isTechnical
GET /api/finance/reference/who                          WHO options for tenant
GET /api/finance/reference/who/{id}                     WHO option by ID
GET /api/finance/reference/who/by-type                  WHO options filtered by isTechnical
GET /api/finance/reference/who/by-main-category/{id}    WHO options by main category ID
GET /api/finance/reference/dropdown-data                All dropdown data in one call
GET /api/finance/reference/dropdown-data/by-type        Dropdown data filtered by isTechnical
```

### Authentication (`/api/auth`)
```
POST /api/auth/login                    Authenticate, get JWT tokens + permissions[]
GET  /api/auth/me                       Get current authenticated user
POST /api/auth/refresh                  Refresh access token (returns new permissions[])
POST /api/auth/logout                   Invalidate refresh token
POST /api/auth/register                 Register basic user account (without org creation)
GET  /api/auth/verify-email             Verify email via token param
POST /api/auth/forgot-password          Request password reset email
POST /api/auth/reset-password           Complete password reset (token + new password)
POST /api/auth/resend-verification      Resend email verification
```

### Onboarding (`/api/onboarding`)
```
POST /api/onboarding/register           Register new tenant + admin user (full onboarding)
POST /api/onboarding/setup              Complete setup after email verification (authenticated)
GET  /api/onboarding/reference-data     Preview reference data (public, for registration UI)
GET  /api/onboarding/health             Health check
```

### Users (`/api/users`)
```
GET    /api/users                       List all users in tenant — USER_VIEW
GET    /api/users/{userId}             Get user by ID — USER_VIEW or own profile
POST   /api/users                       Create user — USER_MANAGE
PUT    /api/users/{userId}/profile      Update user profile — USER_MANAGE or self
PUT    /api/users/{userId}/role         Update user role — USER_MANAGE
PUT    /api/users/{userId}/password     Change password — USER_MANAGE or self
PATCH  /api/users/{id}/activate         Activate user — USER_MANAGE
PATCH  /api/users/{id}/deactivate       Deactivate user — USER_MANAGE
DELETE /api/users/{userId}              Delete user — USER_MANAGE
GET    /api/users/check-email           Check email availability
```

### File Import (`/api/files`)
```
(All endpoints currently commented out — not active)
```

---

### ⚠️ Frontend-Backend Gaps

> Angular çağırıyor ama backend'de karşılığı olmayan endpoint'ler:

| Angular Çağrısı | Backend Durumu | Açıklama |
|-----------------|---------------|----------|
| `GET /api/finance/entries/search/text` | ❌ **MISSING** | `EntryService` text search aktif olduğunda bu URL'yi kullanıyor. Backend sadece `GET /search` var. |
| `POST /api/finance/entries/income` | ❌ **MISSING** | `EntryService.createIncome()` — backend sadece generic `POST /api/finance/entries` var. |
| `POST /api/finance/entries/expense` | ❌ **MISSING** | `EntryService.createExpense()` — backend sadece generic `POST /api/finance/entries` var. |
| `GET /api/finance/entries/{id}/approvals` | ❌ **MISSING** | `ApprovalService.getApprovalHistory()` — backend'de `GET /{id}/history` var ama `/approvals` endpoint'i yok. |

> Mevcut olan ve düzgün çalışan endpoint'ler (önceki versiyonda yanlışlıkla eksik olarak belgelenmişti):

| Angular Çağrısı | Backend Durumu | Açıklama |
|-----------------|---------------|----------|
| `GET /api/finance/entries/{id}/history` | ✅ **EXISTS** | `EntryService.getHistory()` → backend `FinancialEntryController.GET /{id}/history` — unified timeline çalışıyor. |

---

## 10. Current Issues and Roadmap

### 10.1 Current Issues

1. **Text Search Gap:** `EntryService` uses `GET /search/text` URL when `searchTerm` is set, but backend only has `GET /search`. Text search is non-functional.
2. **Typed Create Endpoints Missing:** Frontend calls `POST /income` and `POST /expense` which don't exist in backend. These must use the generic `POST /` endpoint instead.
3. **Approval History Gap:** `ApprovalService.getApprovalHistory()` calls `GET /{id}/approvals` which doesn't exist. Backend has `GET /{id}/history` (unified timeline) in `FinancialEntryController`.

### 10.2 Implementation Status

| Feature | Backend | Frontend | Notes |
|---------|---------|----------|-------|
| Approval routing (`routeByRole`) | ✅ Done | ✅ Done | Captain skip, Admin bypass, limit check |
| `createdByName` in DTO | ✅ Done | ✅ Done | `fromWithUser()` + batch user lookup. `isOwner()` uses `createdById` correctly. |
| Partial approve | ✅ Done (EntryApproval entity supports it) | ⬜ Pending | `approvePartialAmount()` exists in domain but approval endpoint doesn't expose `approvedAmount` param |
| Payment recording | ✅ Done | ✅ Done | Full PaymentService + UI in ActionCenter (`/action-center`, `payment` tab) |
| Bulk approve | ✅ Done | ✅ Done | `ActionCenterStore` handles bulk selection + `bulkApprove()` call |
| Entry history timeline | ✅ Done | ✅ Done | `GET /{id}/history` → `EntryHistoryItemDto` — unified approvals + revisions |
| Approval history (per entry) | ❌ Missing | ✅ Done | Angular calls `GET /{id}/approvals` — backend endpoint not implemented |
| Text search | ❌ Missing | ✅ Done | Angular calls `GET /search/text` — backend only has `GET /search` |
| Typed create (income/expense) | ❌ Missing | ✅ Done | Angular calls `POST /income` + `POST /expense` — backend only has generic `POST /entries` |
| i18n (Turkish/English) | — | ✅ Done | Transloco with browser language detection and persistence |
| File import (Excel) | ❌ Commented out | ⬜ Pending | `FileImportController` endpoints all commented out |

### 10.3 Planned Enhancements

- Add missing backend endpoints: `GET /search/text`, `POST /income`, `POST /expense`, `GET /{id}/approvals`
- Partial approve: expose `approvedAmount` parameter on approve endpoint
- Email notifications for pending approvals and rejections
- Budget management: monthly/yearly limits per category
- Exchange rate service: API source, caching strategy, fallback mechanism
- Mobile-responsive approval workflow
- Bank API integration for payment reconciliation
- File import (Excel): reactivate `FileImportController`

---

## 11. Developer Quick Start

### Build & Run
```bash
# Backend
mvn clean install
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"

# Frontend (development)
cd marine-managment-angular
npm install
ng serve

# Frontend (production build)
ng build --configuration production
```

### Application Profiles
| Profile | Schema | Logging | R2 Storage |
|---------|--------|---------|------------|
| `dev` | create-drop | DEBUG | Disabled |
| `prod` | validate | minimal | Enabled |
| `test` | test-specific | — | — |

### Demo Users
[UNKNOWN/NOT_FOUND_IN_CODE] — demo user initialization not verified in codebase.

### Key Classes
- **Entity:** `FinancialEntry` (aggregate root), `Organization`, `User`
- **Service:** `ApprovalService` (routing + approval), `FinancialEntryService` (CRUD)
- **Security:** `EntryAccessPolicy` (permission checks), `TenantContext` (tenant isolation), `TenantFilter` (Hibernate filter activation)
- **DTO:** `EntryResponseDto` (with `from()` and `fromWithUser()` factory methods), `MoneyDto`
- **Frontend:** `EntryPermissionService` (UI permission mirror), `EntryService` (resource() API), `ActionCenterStore`

### Database Migrations
Flyway automatically runs migrations from `src/main/resources/db/migration/` (V001–V009).

### Testing Approval Flow
```sql
-- Enable manager approval with 1000 EUR limit
UPDATE organizations
SET manager_approval_enabled = true, approval_limit = 1000.00
WHERE id = 1;
```

Then test: Crew/Manager submit any amount → PENDING_CAPTAIN. Captain submit ≤1000 → APPROVED. Captain submit >1000 → PENDING_MANAGER.

### CORS Configuration (dev)
Allowed origins: `http://localhost:4200`, `https://localhost:4200`, `http://127.0.0.1:4200`
Allowed methods: GET, POST, PUT, PATCH, DELETE, OPTIONS
