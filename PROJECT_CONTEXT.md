# Marine Management System - Project Documentation

**Version 2.0 — March 2026**
**Architecture: Modular Monolith + DDD Lite**

---

## 1. Project Overview and Purpose

The Marine Management System is a multi-tenant, web-based SaaS application designed to manage financial operations, personnel, and organizational data for yacht and marine vessel management companies. The system provides comprehensive financial tracking, multi-level approval workflows, and role-based access control for marine industry operations.

**Key Objectives:**
- Centralized financial entry management (expenses and income) with multi-currency support
- Multi-level approval workflows with role-based routing (Captain + Manager levels)
- Complete tenant isolation via Hibernate filters (zero cross-tenant data leakage)
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

### Database
- **Type:** PostgreSQL
- **ORM:** JPA/Hibernate with Hibernate Filters for multi-tenancy
- **Versioning:** Flyway migrations
- **Audit:** Hibernate Envers (revision tables per audited entity)
- **Connection Management:** HikariCP (Spring Boot default)

### Frontend
- **Framework:** Angular 20.0.0 (Standalone Components with new control flow: `@if`, `@for`, `@switch`)
- **Language:** TypeScript 5.8.2 (strict mode)
- **State Management:** Angular Signals (`signal()`, `computed()`, `effect()`) — **No NgRx**
- **Reactivity:** Angular 20 `resource()` API for async data loading with automatic caching
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

**Inheritance chain:** `CREW → MANAGER → CAPTAIN`. Both `ADMIN` and `SUPER_ADMIN` inherit from `CAPTAIN` as siblings.

| Role | Responsibilities | Approval Authority | Key Permissions |
|------|------------------|-------------------|-----------------|
| **SUPER_ADMIN** | Developer/system use only. Cross-tenant access | Full access across all tenants | All CAPTAIN permissions + SYSTEM_CONFIG, TENANT_CREATE, TENANT_DELETE, CROSS_TENANT_ACCESS |
| **ADMIN** | Alias for CAPTAIN — backward compatibility only. Functionally identical to CAPTAIN. | Same as CAPTAIN | Inherits all CAPTAIN permissions (empty own-permission set) |
| **CAPTAIN** | Financial oversight, primary approver, creates own expense entries, crew supervision | Approves at any level. Self-submit skips own level (auto-approve or → PENDING_MANAGER) | APPROVE_CAPTAIN, APPROVE_MANAGER, VIEW_ALL, EDIT_ALL, INCOME_CREATE/EDIT/DELETE, REPORT_VIEW |
| **MANAGER** | Secondary approval for high-value entries, compliance, reporting. **Does NOT create entries.** | Approves PENDING_MANAGER only | APPROVE_MANAGER, VIEW_ALL, INCOME_VIEW, PAYMENT_VIEW/CREATE, REPORT_VIEW, REPORT_EXPORT |
| **CREW** | Creates expenses, submits for approval, basic data entry | None — can only create and submit | ENTRY_CREATE, ENTRY_SUBMIT, VIEW_OWN, EDIT_OWN, DELETE_OWN, CATEGORY_VIEW |

### 3.2 Multi-Tenancy Model

- **Tenant Entity:** `Organization` — represents a yacht/vessel company
- **Tenant Isolation:** Hibernate `@Filter` on `tenant_id` — automatic, no manual WHERE clauses
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

- **JWT Tokens:** Access token (short-lived) + Refresh token (long-lived)
- **Password Storage:** BCrypt via Spring `PasswordEncoder`
- **Session Context:** `JwtAuthenticationFilter` sets both SecurityContext and TenantContext per request
- **Role-Based Access:** Custom `Role` enum with `Permission` set per role
- **Authorization Layer:** `EntryAccessPolicy` component for all data access decisions
- **Tenant Verification:** All operations verify `user.organization_id = TenantContext.tenantId`

### 4.2 Authorization Layers (in order)

| # | Layer | Implementation |
|---|-------|---------------|
| 1 | Tenant Isolation | Hibernate `@Filter` on `tenant_id`. Automatic, no manual WHERE. `TenantFilterAspect` enables filter per request |
| 2 | Permission Check | `EntryAccessPolicy` checks `Role.hasPermission()` before any operation |
| 3 | Ownership Check | `createdById` comparison for Crew users (ensures users only access own entries) |
| 4 | Status Guard | Entity methods enforce valid transitions (e.g., `submit()` throws if not DRAFT) |

### 4.3 Access Control Rules

#### Entry Read Access
- `ENTRY_VIEW_ALL` (Captain/Manager): Can view all expenses in organization
- `ENTRY_VIEW_OWN` (Crew): Can view only own entries (checked by `EntryAccessPolicy.checkReadAccess()`)
- `INCOME_VIEW` (Captain/Manager): Can view all income entries

#### Entry Write Access
- `ENTRY_EDIT_ALL` (Captain/Admin): Can edit any non-final expense
- `ENTRY_EDIT_OWN` (Crew): Can edit own DRAFT entries only
- `INCOME_EDIT` (Captain/Admin only): Can edit income entries. **Manager does NOT have this permission.**

#### Approval Access
- `ENTRY_APPROVE_CAPTAIN` (Captain only): Approve PENDING_CAPTAIN → APPROVED or PENDING_MANAGER
- `ENTRY_APPROVE_MANAGER` (Manager OR Captain): Approve PENDING_MANAGER → APPROVED

#### Rejection & Deletion
- `ENTRY_REJECT` (Captain/Manager): Reject PENDING_CAPTAIN or PENDING_MANAGER (reason required)
- `ENTRY_DELETE_OWN` (Crew): Delete own DRAFT entries only
- `ENTRY_DELETE_ALL` (Captain/Admin): Delete any DRAFT entry

---

## 5. Core Business Rules

### 5.1 Entry Status Lifecycle

**Status Flow:**
```
DRAFT
  ↓ (submit — routed by ApprovalService.routeByRole())
  ├─ Crew submit → PENDING_CAPTAIN
  ├─ Captain submit (≤ limit) → APPROVED
  ├─ Captain submit (> limit, manager enabled) → PENDING_MANAGER
  └─ Admin submit → APPROVED

PENDING_CAPTAIN
  ├─ Captain approve (≤ limit or !managerEnabled) → APPROVED
  ├─ Captain approve (> limit and managerEnabled) → PENDING_MANAGER
  ├─ Captain reject → REJECTED
  └─ (Only Captain can act)

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
- **Manager does NOT create entries.** Manager's role is approval only — they approve high-value entries that exceed the Captain's approval limit.
- **Captain creates entries** and submits them. Captain's own entries skip PENDING_CAPTAIN (self-approval is meaningless).
- **REJECTED is final.** If an entry is rejected, the Crew member must create a new entry. There is no resubmit/reopen flow.
- **Income is Captain-only.** Only Captain (and Admin) can create income entries. Crew cannot view or create incomes. Income entries do not go through approval — they are auto-approved on submit since Captain is already the financial authority.
- **Crew pocket expenses** (when Crew pays from own pocket) are recorded as income by the Captain, not by the Crew member.

### 5.2 Submit Routing Rules (ApprovalService.routeByRole)

This is the core routing logic. When a user clicks "Submit", `ApprovalService.routeByRole()` determines the target status based on the submitter's role:

| Submitter | Condition | Target Status | Entity Method |
|-----------|-----------|---------------|--------------|
| **CREW** | Always | PENDING_CAPTAIN | `entry.submit()` |
| **CAPTAIN** | amount ≤ limit OR !managerEnabled | APPROVED | `entry.submitAndApprove()` |
| **CAPTAIN** | amount > limit AND managerEnabled | PENDING_MANAGER | `entry.submitToManager()` |
| **ADMIN** | Always (bypass) | APPROVED | `entry.submitAndApprove()` |

**Important:** Captain's own entries NEVER go to PENDING_CAPTAIN — it would be meaningless for Captain to approve their own submission. Manager does not create or submit entries — Manager's role is exclusively approval of high-value entries.

### 5.3 Approval Rules

| Current Status | Approver | Condition | Result |
|---------------|----------|-----------|--------|
| PENDING_CAPTAIN | Captain | amount ≤ limit OR !managerEnabled | APPROVED |
| PENDING_CAPTAIN | Captain | amount > limit AND managerEnabled | PENDING_MANAGER |
| PENDING_MANAGER | Manager or Captain | Always | APPROVED |

### 5.4 Pending List Rules (EntryAccessPolicy.getPendingSpecification)

Each role sees different entries in their Pending Actions view:

| Role | Sees | Purpose |
|------|------|---------|
| **CREW** | Own DRAFT entries only | Reminder to submit unfinished entries |
| **MANAGER** | PENDING_MANAGER entries | High-value entries awaiting their approval |
| **CAPTAIN** | All DRAFT + PENDING_CAPTAIN + PENDING_MANAGER | Full visibility — Captain oversees everything |

### 5.5 Amount Calculations

- **Original Amount:** Immutable requested amount in original currency
- **Base Amount (EUR):** Calculated using exchange rate at entry date. If original = EUR, no conversion.
- **Approved Amount:** Set when entry is approved (`approvedBaseAmount = baseAmount`). Zero until approval.
- **Paid Amount:** Cumulative payment. Incremental only. Cannot exceed approved amount.
- **Remaining Amount:** `approvedBaseAmount - paidBaseAmount`

**Validation Rules:**
- Paid amount cannot exceed approved amount
- Entry amount must be positive
- Entry date cannot be in the future
- Category must be active
- All linked objects (category, WHO, main category) must belong to same tenant

### 5.6 Exchange Rate Handling

- **Rate Date:** Captured at entry creation (entry date)
- **Conversion:** `baseAmount = originalAmount × rate`
- **Updates:** Exchange rate can be recalculated manually
- **Service:** `ExchangeRateService` provides rates (implementation details: API source, caching, and fallback strategy TBD)
- **Error Handling:** `ExchangeRateCalculationException` if rate unavailable

---

## 6. Domain Design

### 6.0 Aggregate Root: FinancialEntry

`FinancialEntry` bu modülün Aggregate Root'u. `Payment`, `FinancialEntryAttachment` ve `EntryApproval` tek başlarına anlam taşımaz — hepsi `FinancialEntry` bağlamında var olur ve onun yaşam döngüsüne tabidir.

**Cascade kuralları:**
- `Payment` → `CascadeType.ALL` + `orphanRemoval = true`. Entry yaşarken payment eklenip çıkarılabilir.
- `FinancialEntryAttachment` → `CascadeType.ALL` + `orphanRemoval = true`. Zaten bu şekilde implemente edilmiş.
- `EntryApproval` → `CascadeType.PERSIST` + `CascadeType.MERGE`. Kalıcı audit trail, fiziksel olarak silinmez.

**Soft Delete:**
Silme işlemi fiziksel değil. `BaseAuditedEntity`'de `is_deleted`, `deleted_at`, `deleted_by_id` field'ları zaten mevcut. Hibernate `@Where(clause = "is_deleted = false")` anotasyonu tüm query'lere otomatik uygulanır — `@FilterDef` gibi manuel `enableFilter()` çağrısı gerekmez, her zaman aktiftir.

Entry soft delete olduğunda child entity'lere (`Payment`, `Attachment`, `Approval`) ayrıca `deleted` flag koymaya gerek yok — entry'ye erişilemeyen child'lara da erişilemez. Aggregate Root'un doğal koruması.

`CascadeType.REMOVE` hiçbir yerde kullanılmaz — fiziksel silme yoktur.

> **Not:** `@Where` Hibernate 6'da deprecated, yerine `@SQLRestriction` önerilmektedir. Mevcut kod çalışmaya devam eder ancak ileride geçiş yapılabilir.

**PaymentRepository:**
Mutation için kullanılmaz — `Payment` ekleme/silme işlemleri `FinancialEntry.addPayment()` / `removePayment()` üzerinden yapılır, cascade ile persist edilir. Repository sadece read query'ler için tutulur (`findByEntry`, `countByEntry`, `sumPaymentsByEntryId`, vb.).

**Audit (kim sildi, ne zaman):**
Hibernate Envers tüm entity değişikliklerini otomatik kayıt altına alıyor. Soft delete bir `UPDATE` (`deleted = true`) olarak Envers tarafından da loglanır — kaydın tüm geçmişi + "silindi" işareti korunur. Ekstra bir mekanizmaya gerek yok.

---

## 6. Data Model

### 6.1 EntryResponseDto (Primary API Response)

| Field | Type | Notes |
|-------|------|-------|
| `id` | UUID | Primary key |
| `entryNumber` | String | Formatted: FE-2026-001 |
| `status` | EntryStatus | DRAFT, PENDING_CAPTAIN, PENDING_MANAGER, APPROVED, REJECTED, PARTIALLY_PAID, PAID |
| `entryType` | RecordType | EXPENSE or INCOME |
| `categoryId` | UUID | Financial category reference |
| `categoryCode` / `categoryName` | String | Category display info |
| `originalAmount` | MoneyDto | Immutable. Crew's original request (amount + currency) |
| `baseAmount` | MoneyDto | EUR equivalent via exchange rate |
| `approvedBaseAmount` | MoneyDto | Set on approval. null/zero until approved |
| `paidBaseAmount` | MoneyDto | Cumulative payment. Incremental only |
| `remainingAmount` | MoneyDto | Calculated: approved - paid |
| `createdById` | UUID | User who created the entry. **IMPORTANT: JSON serializes as `createdById`, NOT `createdBy`** |
| `createdByName` | String | Display name (e.g., "Jane Crew"). Populated via batch user lookup. May be null in some contexts (e.g., `from()` vs `fromWithUser()`) |
| `hasAttachments` | Boolean | Whether entry has attached files |

**CRITICAL Frontend Note:** The backend record field is `createdById` (UUID). Frontend `EntryResponse` model MUST use `createdById` — not `createdBy`. A field name mismatch causes `EntryPermissionService.isOwner()` to always return false, hiding edit/delete buttons for Crew users on their own entries.

### 6.2 Core Database Tables

#### `organizations`
- Primary key: `id` (BIGSERIAL)
- Key fields: `yacht_name` (unique), `flag_country`, `base_currency`
- Approval config: `manager_approval_enabled` (BOOLEAN), `approval_limit` (NUMERIC)
- Subscription: `subscription_status`, `subscription_expires_at`

#### `users`
- Primary key: `id` (UUID)
- Key fields: `email` (unique), `first_name`, `last_name`, `password_hash`, `role`
- FK: `organization_id` → organizations

#### `financial_entries` ⭐ Core Table
- Primary key: `id` (UUID)
- Status: `status` VARCHAR(20) — DRAFT, PENDING_CAPTAIN, PENDING_MANAGER, APPROVED, REJECTED, PARTIALLY_PAID, PAID
- Amounts: `original_amount`/`original_currency` (immutable), `base_amount`/`base_currency` (EUR), `approved_base_amount`/`approved_base_currency`, `paid_base_amount`/`paid_base_currency`
- Context: `category_id`, `tenant_who_id`, `tenant_main_category_id`
- Location: `country`, `city`, `specific_location`, `vendor`, `recipient`
- Audit: `created_by_id`, `updated_by_id` (via AuditingEntityListener)
- Key indexes: `(tenant_id, status)`, `(tenant_id, entry_date, status)`, `(tenant_id, created_by_id)`

#### `financial_categories`
- Primary key: `id` (UUID)
- Key fields: `code`, `name`, `category_type` (EXPENSE/INCOME), `is_active`
- Tenant-scoped via `tenant_id`

#### `financial_entry_attachments`
- Primary key: `id` (UUID)
- FK: `entry_id` → financial_entries (cascade delete)
- Fields: `file_name`, `file_path`, `file_size`, `file_type`

---

## 7. Frontend Architecture

### 7.1 Module Structure and Routing

**Public Routes:** `/login`, `/register`, `/access-denied`
**Protected Routes:** All features under Layout wrapper with `authGuard` (outer) + `permissionGuard` (per-route)

| Route | Component | Required Permission |
|-------|-----------|-------------------|
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

**Lazy Loading:** Components loaded via `loadComponent()` on demand

### 7.2 Core Module (src/app/core/)
- **guards/:** `authGuard` (checks isAuthenticated on Layout wrapper), `permissionGuard` (checks specific permissions per route)
- **interceptors/:** `authInterceptor` (adds Bearer token, handles 401 refresh), `errorInterceptor` (global error handling)
- **services:** `AuthService`, `StorageService`, `NotificationService`, `EntryPermissionService` (root singleton), `TenantContextService`

### 7.3 State Management: Signals-Based Stores

Each feature has a dedicated store using Angular Signals. No NgRx dependency.

| Store | Scope | Key Signals |
|-------|-------|-------------|
| `ExpenseListStore` | Component-level | entries, filters, pagination, bulk selection |
| `ExpenseFormStore` | Component-level | form, isEditMode, isFormReadOnly, canSubmitForApproval |
| `ActionCenterStore` | Component-level | approvalEntries, paymentEntries, activeTab (approvals/payments), bulk selection, reject modal |
| `PendingApprovalsStore` | Component-level | pendingEntries grouped by status |
| `EntryPermissionService` | Root (singleton) | canEdit, canDelete, canSubmit, canApprove, canReject |

### 7.4 EntryPermissionService (Frontend Permission Mirror)

Frontend mirror of backend `EntryAccessPolicy`. Provides UI-level permission checks. **Backend remains the ultimate authority** — this service only prevents unnecessary UI actions and improves UX.

| Method | Rules |
|--------|-------|
| `canEdit(entry)` | Admin: always. Final status: never. Captain: any non-final. Crew/Manager: own DRAFT only |
| `canDelete(entry)` | Only DRAFT. Captain/Admin: any DRAFT. Crew/Manager: own DRAFT only |
| `canSubmit(entry)` | Only DRAFT. Owner or Admin |
| `canApproveCaptain(entry)` | (Captain OR Admin) AND status === PENDING_CAPTAIN |
| `canApproveManager(entry)` | (Manager OR Captain OR Admin) AND status === PENDING_MANAGER |
| `isReadOnly(entry)` | `!canEdit()` — form disabled, no save buttons |
| `canShowSubmitButton(entry)` | status === DRAFT (REJECTED is final — no resubmit) |

### 7.5 Form Button Visibility Logic

| Entry State | Visible Buttons | Controlled By |
|-------------|----------------|---------------|
| New entry (no ID) | Save as Draft + Save & Submit | Always visible |
| DRAFT (own) | Save as Draft + Save & Submit | `canSaveAsDraft()` + `canShowSubmitButton()` |
| PENDING_* (Captain edit) | Save only | `canEdit()` but `!canShowSubmitButton()` |
| Read-only entry | No buttons | `isFormReadOnly()` = true |

### 7.6 Auth Flow

1. User enters credentials → `AuthService.login(credentials, persistent)`
2. POST `/api/auth/login` → Receive `{user, accessToken, refreshToken}`
3. Update signals: `_currentUser`, `_token`, `_refreshToken`
4. Persist: `localStorage` (persistent) or `sessionStorage` (session-only)
5. Set tenant context → all services auto-reload data
6. Navigate to `/dashboard`

**Token Refresh:** `authInterceptor` catches 401 → calls `/api/auth/refresh` → retries request. If refresh fails → `logout()` + redirect to `/login`.

---

## 8. API Endpoints Summary

> **Base prefix:** All finance endpoints are under `/api/finance/`. Auth endpoints are under `/api/auth/`.

### Entry Management (`/api/finance/entries`)
```
POST   /api/finance/entries                         Create entry (DRAFT)
GET    /api/finance/entries/{id}                    Get entry by ID
GET    /api/finance/entries/number/{entryNumber}    Get entry by formatted number (e.g. FE-2026-001)
GET    /api/finance/entries/expenses/search         List expenses (paginated, filtered)
GET    /api/finance/entries/incomes/search          List incomes (paginated, filtered)
GET    /api/finance/entries/search                  List all entries (paginated, filtered)
GET    /api/finance/entries/status/{status}         List entries by status
GET    /api/finance/entries/status/count/{status}   Count entries by status
PUT    /api/finance/entries/{id}                    Full update of entry
PATCH  /api/finance/entries/{id}/context            Update WHO + MainCategory fields
PATCH  /api/finance/entries/{id}/metadata           Update metadata fields
PATCH  /api/finance/entries/{id}/receipt-number     Update receipt number
PATCH  /api/finance/entries/{id}/exchange-rate      Recalculate exchange rate
DELETE /api/finance/entries/{id}                    Delete entry (DRAFT only)
GET    /api/finance/entries/capabilities            Get current user's capabilities
```

### Approval (`/api/finance/entries`)
```
POST /api/finance/entries/{id}/submit               Submit (role-based routing via routeByRole)
POST /api/finance/entries/{id}/approve              Approve at current level (auto-detects PENDING_CAPTAIN or PENDING_MANAGER)
POST /api/finance/entries/{id}/reject               Reject (reason required)
GET  /api/finance/entries/pending                   Pending items for current user (role-filtered)
GET  /api/finance/entries/pending/count             Pending count for dashboard badge
POST /api/finance/entries/bulk/approve              Bulk approve entries
```

> **Note:** There is a single `/approve` endpoint — the backend auto-detects the entry's current status (PENDING_CAPTAIN or PENDING_MANAGER) and applies the appropriate approval. There are no separate `/approve/captain` or `/approve/manager` sub-endpoints.

> ⚠️ **Frontend-Backend Gap — Angular calls these endpoints but they don't exist in the backend yet:**
> - `GET /api/finance/entries/{id}/approvals` — Approval history for an entry (Angular: `approval.service.ts`)
> - `GET /api/finance/entries/search/text` — Full-text search across entries (Angular: `entry.service.ts`, used when searchTerm is set)
> - `POST /api/finance/entries/income` — Typed create for income entries (Angular: `entry.service.ts`)
> - `POST /api/finance/entries/expense` — Typed create for expense entries (Angular: `entry.service.ts`)
>
> The backend only exposes a single `POST /api/finance/entries` (generic create). The three missing GET/POST endpoints need to be added to `FinancialEntryController`.

### Attachments (`/api/finance/entries`)
```
POST   /api/finance/entries/{id}/attachments                     Upload single attachment
POST   /api/finance/entries/{id}/attachments/bulk                Upload multiple attachments
GET    /api/finance/entries/{id}/attachments                     List attachments for entry
GET    /api/finance/entries/{id}/attachments/{attachmentId}/download  Download attachment
DELETE /api/finance/entries/{id}/attachments/{attachmentId}      Delete attachment
```

### Payments (`/api/finance/entries`)
```
POST   /api/finance/entries/{id}/payments            Record partial payment
POST   /api/finance/entries/{id}/payments/full       Record full payment (marks as PAID)
GET    /api/finance/entries/{id}/payments            List payments for entry
GET    /api/finance/entries/{id}/payments/summary    Payment summary for entry
GET    /api/finance/entries/payments/recent          Recent payments across all entries
GET    /api/finance/entries/payments/by-date         Payments filtered by date range
PATCH  /api/finance/entries/payments/{paymentId}     Update payment record
DELETE /api/finance/entries/payments/{paymentId}     Delete payment record
```

### Dashboard (`/api/finance/dashboard`)
```
GET /api/finance/dashboard/summary                  Overall financial summary
GET /api/finance/dashboard/period-totals            Totals for a given period
GET /api/finance/dashboard/category-totals          Totals grouped by category
GET /api/finance/dashboard/expense-totals           Expense totals
GET /api/finance/dashboard/income-totals            Income totals
GET /api/finance/dashboard/monthly-totals           Monthly breakdown
GET /api/finance/dashboard/cumulative-balance       Cumulative balance over time
GET /api/finance/dashboard/cumulative-balance/complete  Full cumulative balance dataset
```

### Categories & Reference Data
```
GET  /api/finance/categories                                List financial categories
POST /api/finance/categories                                Create category (Captain/Admin)
GET  /api/finance/reference/who                             WHO options for tenant
GET  /api/finance/reference/who/{id}                        WHO option by ID
GET  /api/finance/reference/who/by-type                     WHO options filtered by type
GET  /api/finance/reference/who/by-main-category/{id}       WHO options by main category
GET  /api/finance/reference/main-categories                 MainCategory options for tenant
GET  /api/finance/reference/main-categories/{id}            MainCategory by ID
GET  /api/finance/reference/main-categories/by-type         MainCategories filtered by type
GET  /api/finance/reference/dropdown-data                   All dropdown data in one call
GET  /api/finance/reference/dropdown-data/by-type           Dropdown data filtered by type
```

### Auth
```
POST /api/auth/login                    Authenticate, get JWT tokens
POST /api/auth/refresh                  Refresh access token
POST /api/auth/register                 Register new tenant + admin user
POST /api/auth/logout                   Invalidate refresh token
```

---

## 9. Current Issues and Roadmap

### 9.1 Current Issues

1. **Pending page UX:** Currently uses card layout which is inefficient for 28+ entries. Missing: `createdByName` display, table view, bulk actions, status tabs, summary metrics.

### 9.2 Implementation Status

| Feature | Backend | Frontend | Notes |
|---------|---------|----------|-------|
| Approval routing (`routeByRole`) | ✅ Done | ✅ Done | Captain skip, Admin bypass, limit check |
| `createdByName` in DTO | ✅ Done | ⬜ Pending | `fromWithUser()` + batch user lookup ready, pending page not yet updated |
| Partial approve | ⬜ Planned | ⬜ Planned | `approvedAmount` parameter needed on approve endpoints |
| Payment recording | ✅ Done | ✅ Done | Full payment service + UI in ActionCenter (`/action-center` route, `payment` tab) |
| Bulk approve | ✅ Done | ✅ Done | `ActionCenterStore` handles bulk selection + approve call |
| Approval history | ⬜ Missing | ✅ Done | Angular calls `GET /{id}/approvals` — backend endpoint not implemented yet |
| Text search | ⬜ Missing | ✅ Done | Angular calls `GET /search/text` — backend endpoint not implemented yet |
| Typed create (income/expense) | ⬜ Missing | ✅ Done | Angular calls `POST /income` + `POST /expense` — backend only has generic `POST /entries` |

### 9.3 Planned Enhancements

- Pending page redesign: table format, createdByName column, bulk approve, status tabs
- Status badge colors: DRAFT=gray, PENDING_CAPTAIN=orange, APPROVED=green, REJECTED=red
- Payment UI: record payments against approved entries, partial payment tracking
- Partial approve: Captain/Manager can approve reduced amount
- Email notifications for pending approvals and rejections
- Budget management: monthly/yearly limits per category
- Exchange rate service: API source, caching strategy, fallback mechanism
- Mobile-responsive approval workflow
- Bank API integration for payment reconciliation

---

## 10. Developer Quick Start

### Build & Run
```bash
# Backend
mvn clean install
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"

# Frontend (development)
cd frontend
npm install
ng serve

# Frontend (production build)
ng build --configuration production
```

### Demo Users (dev profile)
| Email | Role | Password |
|-------|------|----------|
| admin@demo.com | ADMIN | Demo123! |
| captain@demo.com | CAPTAIN | Demo123! |
| manager@demo.com | MANAGER | Demo123! |
| crew1@demo.com | CREW | Demo123! |
| crew2@demo.com | CREW | Demo123! |

### Key Classes
- **Entity:** `FinancialEntry`, `Organization`, `User`
- **Service:** `ApprovalService` (routing + approval), `EntryService` (CRUD)
- **Security:** `EntryAccessPolicy` (permission checks), `TenantContext` (tenant isolation)
- **DTO:** `EntryResponseDto` (with `from()` and `fromWithUser()` factory methods)
- **Frontend:** `EntryPermissionService` (UI permission mirror), `ExpenseListStore`, `ExpenseFormStore`

### Database Migrations
Flyway automatically runs migrations from `src/main/resources/db/migration/`

### Testing Approval Flow
```sql
-- Enable manager approval with 1000 EUR limit
UPDATE organizations
SET manager_approval_enabled = true, approval_limit = 1000.00
WHERE id = 1;
```

Then test: Crew submit any amount → PENDING_CAPTAIN. Captain submit ≤1000 → APPROVED. Captain submit >1000 → PENDING_MANAGER.