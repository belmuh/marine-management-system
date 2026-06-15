-- ============================================================================
-- V001: Baseline şema (squash: eski V001–V010, 2026-06-11)
--
-- Proje henüz canlıya çıkmadığı için eski 10 migration tek baseline'a ezildi.
-- Bu dosya entity'lerle birebir uyumludur (mvn verify: Flyway + Hibernate
-- validate + Testcontainers ile kanıtlandı).
--
-- Eski dosyalardan farklar (bilinçli temizlik):
--   - Ölü is_active kolonları atıldı (financial_categories, tenant_main_categories,
--     tenant_who_selections) — entity'ler is_enabled kullanıyor
--   - financial_entry_attachments: file_type/uploaded_by_id eski adları yok,
--     doğrudan content_type/uploaded_by (entity adları) + users FK
--   - revinfo: SERIAL yerine Hibernate 6 Envers'in beklediği revinfo_seq
--
-- DİKKAT: Mevcut dev DB'ler eski flyway_schema_history taşır; bir kez
-- sıfırlanmaları gerekir (DROP SCHEMA public CASCADE; CREATE SCHEMA public;).
-- ============================================================================

-- ────────────────────────────────────────────────────────────
-- Global tablolar
-- ────────────────────────────────────────────────────────────

CREATE TABLE organizations (
    id BIGSERIAL PRIMARY KEY,
    yacht_name VARCHAR(100) NOT NULL,
    company_name VARCHAR(100),
    flag_country VARCHAR(2) NOT NULL,
    base_currency VARCHAR(3) NOT NULL,
    yacht_type VARCHAR(50),
    yacht_length INTEGER,
    home_marina VARCHAR(200),
    current_location VARCHAR(200),
    timezone VARCHAR(50) NOT NULL DEFAULT 'Europe/Istanbul',
    financial_year_start_month INTEGER NOT NULL DEFAULT 1,
    subscription_status VARCHAR(20) NOT NULL,
    subscription_expires_at DATE,
    active BOOLEAN NOT NULL DEFAULT true,
    manager_approval_enabled BOOLEAN NOT NULL DEFAULT false,
    approval_limit NUMERIC(15,2),
    onboarding_completed BOOLEAN NOT NULL DEFAULT false,
    -- audit + soft delete
    version BIGINT NOT NULL DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    deleted_at TIMESTAMP,
    deleted_by_id UUID,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by_id UUID,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by_id UUID
);

CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(100) NOT NULL UNIQUE,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    password_hash VARCHAR(60) NOT NULL,
    role VARCHAR(20) NOT NULL,
    organization_id BIGINT NOT NULL REFERENCES organizations(id),
    is_active BOOLEAN NOT NULL DEFAULT true,
    last_login_at TIMESTAMP,
    -- e-posta doğrulama
    email_verified BOOLEAN NOT NULL DEFAULT false,
    verification_token VARCHAR(100),
    verification_token_expires_at TIMESTAMP,
    -- şifre sıfırlama
    password_reset_token VARCHAR(100),
    password_reset_token_expires_at TIMESTAMP,
    -- audit + soft delete
    version BIGINT NOT NULL DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    deleted_at TIMESTAMP,
    deleted_by_id UUID,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by_id UUID,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by_id UUID
);

CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY,
    token VARCHAR(255) NOT NULL UNIQUE,
    user_id UUID NOT NULL REFERENCES users(id),
    expiry_date TIMESTAMP NOT NULL,
    ip_address VARCHAR(255),
    user_agent VARCHAR(255),
    -- audit + soft delete (BaseAuditedEntity)
    version BIGINT NOT NULL DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    deleted_at TIMESTAMP,
    deleted_by_id UUID,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by_id UUID,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by_id UUID
);

-- Global referans verisi
CREATE TABLE main_categories (
    id BIGSERIAL PRIMARY KEY,
    name_tr VARCHAR(100) NOT NULL,
    name_en VARCHAR(100) NOT NULL,
    is_technical BOOLEAN NOT NULL DEFAULT true,
    display_order INTEGER,
    budget_guideline_min VARCHAR(10),
    budget_guideline_max VARCHAR(10),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE who (
    id BIGSERIAL PRIMARY KEY,
    name_tr VARCHAR(100) NOT NULL,
    name_en VARCHAR(100) NOT NULL,
    is_technical BOOLEAN NOT NULL DEFAULT true,
    display_order INTEGER,
    suggested_main_category_id BIGINT REFERENCES main_categories(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Döviz kurları (global, tenant'sız)
CREATE TABLE exchange_rates (
    id BIGSERIAL PRIMARY KEY,
    date DATE NOT NULL,
    from_currency VARCHAR(3) NOT NULL,
    to_currency VARCHAR(3) NOT NULL,
    rate NUMERIC(12, 8) NOT NULL,
    base BOOLEAN,
    source VARCHAR(50),
    created_at TIMESTAMP,
    CONSTRAINT uq_exchange_rates_date_pair UNIQUE (date, from_currency, to_currency)
);

-- ────────────────────────────────────────────────────────────
-- Tenant kapsamlı tablolar
-- ────────────────────────────────────────────────────────────

CREATE TABLE tenant_main_categories (
    id UUID PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    main_category_id BIGINT NOT NULL REFERENCES main_categories(id),
    budget_percentage NUMERIC(5,2),
    accounting_code VARCHAR(50),
    is_enabled BOOLEAN NOT NULL DEFAULT true,
    notes VARCHAR(500),
    -- audit + soft delete
    version BIGINT NOT NULL DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    deleted_at TIMESTAMP,
    deleted_by_id UUID,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by_id UUID,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by_id UUID,
    CONSTRAINT uq_tenant_main_category UNIQUE (tenant_id, main_category_id)
);

CREATE TABLE tenant_who_selections (
    id UUID PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    who_id BIGINT NOT NULL REFERENCES who(id),
    is_enabled BOOLEAN NOT NULL DEFAULT true,
    -- audit + soft delete
    version BIGINT NOT NULL DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    deleted_at TIMESTAMP,
    deleted_by_id UUID,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by_id UUID,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by_id UUID,
    CONSTRAINT uq_tenant_who UNIQUE (tenant_id, who_id)
);

CREATE TABLE financial_categories (
    id UUID PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    name_en VARCHAR(100),
    category_type VARCHAR(20) NOT NULL,
    description TEXT,
    is_technical BOOLEAN NOT NULL DEFAULT true,
    is_enabled BOOLEAN NOT NULL DEFAULT true,
    display_order INTEGER,
    -- audit + soft delete
    version BIGINT NOT NULL DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    deleted_at TIMESTAMP,
    deleted_by_id UUID,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by_id UUID,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by_id UUID,
    CONSTRAINT uq_financial_categories_tenant_name UNIQUE (tenant_id, name)
);

CREATE TABLE financial_entries (
    id UUID PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    entry_number VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    entry_type VARCHAR(20) NOT NULL,
    category_id UUID NOT NULL REFERENCES financial_categories(id),
    tenant_who_id UUID REFERENCES tenant_who_selections(id),
    tenant_main_category_id UUID REFERENCES tenant_main_categories(id),

    -- Talep edilen tutarlar (değişmez)
    original_amount NUMERIC(19,4) NOT NULL,
    original_currency VARCHAR(3) NOT NULL,
    base_amount NUMERIC(19,4) NOT NULL,
    base_currency VARCHAR(3) NOT NULL,

    -- Onaylanan / ödenen tutarlar
    approved_base_amount NUMERIC(19,4),
    approved_base_currency VARCHAR(3),
    paid_base_amount NUMERIC(19,4) DEFAULT 0,
    paid_base_currency VARCHAR(3),

    -- Kur anlık görüntüsü
    exchange_rate NUMERIC(19,6),
    exchange_rate_date DATE,

    entry_date DATE NOT NULL,
    payment_method VARCHAR(20) NOT NULL,
    description TEXT,
    receipt_number VARCHAR(100),
    rejection_reason VARCHAR(500),

    -- Konum
    country VARCHAR(50),
    city VARCHAR(50),
    specific_location VARCHAR(200),

    vendor VARCHAR(100),
    recipient VARCHAR(50),

    -- Metadata
    frequency VARCHAR(20),
    priority VARCHAR(20),
    tags VARCHAR(500),

    -- audit + soft delete
    version BIGINT NOT NULL DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    deleted_at TIMESTAMP,
    deleted_by_id UUID,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by_id UUID NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by_id UUID,

    CONSTRAINT uq_financial_entries_tenant_entry_number UNIQUE (tenant_id, entry_number)
);

CREATE TABLE financial_entry_attachments (
    id UUID PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    entry_id UUID NOT NULL REFERENCES financial_entries(id) ON DELETE CASCADE,
    file_name VARCHAR(255) NOT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_size BIGINT NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    attachment_type VARCHAR(20) NOT NULL,
    uploaded_by UUID NOT NULL REFERENCES users(id),
    uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE entry_approvals (
    id UUID PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    entry_id UUID NOT NULL REFERENCES financial_entries(id) ON DELETE CASCADE,
    approval_level VARCHAR(20) NOT NULL,     -- CAPTAIN, MANAGER

    -- Bu onay seviyesindeki tutarlar
    requested_amount NUMERIC(19,4) NOT NULL,
    requested_currency VARCHAR(3) NOT NULL,
    approved_amount NUMERIC(19,4),
    approved_currency VARCHAR(3),

    approval_status VARCHAR(20) NOT NULL,    -- PENDING, APPROVED, PARTIAL, REJECTED
    rejection_reason VARCHAR(500),
    approver_id UUID REFERENCES users(id),
    approval_date TIMESTAMP,

    -- audit + soft delete
    version BIGINT NOT NULL DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    deleted_at TIMESTAMP,
    deleted_by_id UUID,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by_id UUID,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by_id UUID
);

CREATE TABLE payments (
    id UUID PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    entry_id UUID NOT NULL REFERENCES financial_entries(id) ON DELETE CASCADE,
    amount NUMERIC(19,4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    payment_date DATE NOT NULL,
    payment_reference VARCHAR(100),
    payment_method VARCHAR(20),
    notes VARCHAR(500),
    recorded_by_id UUID NOT NULL,
    recorded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- audit + soft delete
    version BIGINT NOT NULL DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    deleted_at TIMESTAMP,
    deleted_by_id UUID,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by_id UUID,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by_id UUID
);

-- ────────────────────────────────────────────────────────────
-- Envers audit altyapısı
-- ────────────────────────────────────────────────────────────

-- Hibernate 6 DefaultRevisionEntity id'yi bu sequence'tan üretir (allocationSize=50)
CREATE SEQUENCE revinfo_seq START WITH 1 INCREMENT BY 50;

-- Fiş numarası üretimi: FinancialEntryRepository.getNextEntryNumber()
-- native NEXTVAL('financial_entry_seq') çağırır. Hiçbir entity'ye mapli
-- olmadığı için ddl-auto/validate görmez — migration'da oluşturulmak zorunda.
CREATE SEQUENCE financial_entry_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE revinfo (
    rev         INTEGER PRIMARY KEY,
    revtstmp    BIGINT NOT NULL,
    -- değişikliği kim yaptı
    user_id             UUID,
    username            VARCHAR(100),
    user_display_name   VARCHAR(200),
    -- değişiklik bağlamı
    source              VARCHAR(20) NOT NULL DEFAULT 'API',
    correlation_id      VARCHAR(100)
);

-- Yalnızca audit edilen alanlar (@NotAudited hariç) — detay için FinancialEntry
CREATE TABLE financial_entries_aud (
    id              UUID NOT NULL,
    rev             INTEGER NOT NULL REFERENCES revinfo(rev),
    revtype         SMALLINT,

    entry_number    VARCHAR(50),
    status          VARCHAR(20),
    entry_type      VARCHAR(20),

    original_amount         NUMERIC(19,4),
    original_currency       VARCHAR(3),
    base_amount             NUMERIC(19,4),
    base_currency           VARCHAR(3),
    approved_base_amount    NUMERIC(19,4),
    approved_base_currency  VARCHAR(3),
    paid_base_amount        NUMERIC(19,4),
    paid_base_currency      VARCHAR(3),

    entry_date          DATE,
    payment_method      VARCHAR(20),
    receipt_number      VARCHAR(100),
    exchange_rate       NUMERIC(19,6),
    exchange_rate_date  DATE,

    category_id                 UUID,
    tenant_who_id               UUID,
    tenant_main_category_id     UUID,

    created_at      TIMESTAMP,
    created_by_id   UUID,

    PRIMARY KEY (id, rev)
);

-- ────────────────────────────────────────────────────────────
-- İndeksler
-- ────────────────────────────────────────────────────────────

-- organizations
CREATE INDEX idx_organizations_subscription ON organizations(subscription_status);

-- users
CREATE INDEX idx_users_organization ON users(organization_id);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_verification_token ON users(verification_token) WHERE verification_token IS NOT NULL;
CREATE INDEX idx_users_password_reset_token ON users(password_reset_token) WHERE password_reset_token IS NOT NULL;

-- refresh_tokens
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_expiry ON refresh_tokens(expiry_date);

-- exchange_rates
CREATE INDEX idx_rate_lookup ON exchange_rates(date, from_currency, to_currency);

-- tenant_main_categories
CREATE INDEX idx_tenant_main_categories_tenant ON tenant_main_categories(tenant_id);
CREATE INDEX idx_tenant_main_categories_main_cat ON tenant_main_categories(main_category_id);

-- tenant_who_selections
CREATE INDEX idx_tenant_who_tenant ON tenant_who_selections(tenant_id);
CREATE INDEX idx_tenant_who_who ON tenant_who_selections(who_id);

-- financial_categories
CREATE INDEX idx_financial_categories_tenant ON financial_categories(tenant_id);

-- financial_entries
CREATE INDEX idx_financial_entries_tenant ON financial_entries(tenant_id);
CREATE INDEX idx_financial_entries_tenant_date ON financial_entries(tenant_id, entry_date);
CREATE INDEX idx_financial_entries_tenant_entry_number ON financial_entries(tenant_id, entry_number);
CREATE INDEX idx_financial_entries_category ON financial_entries(category_id);
CREATE INDEX idx_financial_entries_tenant_who ON financial_entries(tenant_who_id);
CREATE INDEX idx_financial_entries_tenant_main_cat ON financial_entries(tenant_main_category_id);
CREATE INDEX idx_financial_entries_status ON financial_entries(status);
CREATE INDEX idx_financial_entries_tenant_status ON financial_entries(tenant_id, status);
CREATE INDEX idx_financial_entries_tenant_created_by ON financial_entries(tenant_id, created_by_id);
CREATE INDEX idx_financial_entries_tenant_date_status ON financial_entries(tenant_id, entry_date, status);
CREATE INDEX idx_financial_entries_deleted ON financial_entries(is_deleted);

-- financial_entry_attachments
CREATE INDEX idx_financial_entry_attachments_entry ON financial_entry_attachments(entry_id);
CREATE INDEX idx_attachments_tenant ON financial_entry_attachments(tenant_id);
CREATE INDEX idx_attachments_type ON financial_entry_attachments(entry_id, attachment_type);

-- entry_approvals
CREATE INDEX idx_entry_approvals_tenant ON entry_approvals(tenant_id);
CREATE INDEX idx_entry_approvals_entry ON entry_approvals(entry_id);
CREATE INDEX idx_entry_approvals_level_status ON entry_approvals(approval_level, approval_status);
CREATE INDEX idx_entry_approvals_approver ON entry_approvals(approver_id);

-- payments
CREATE INDEX idx_payments_tenant ON payments(tenant_id);
CREATE INDEX idx_payments_entry ON payments(entry_id);
CREATE INDEX idx_payments_date ON payments(payment_date);
CREATE INDEX idx_payments_recorded_by ON payments(recorded_by_id);

-- revinfo / financial_entries_aud
CREATE INDEX idx_revinfo_user_id ON revinfo(user_id);
CREATE INDEX idx_revinfo_timestamp ON revinfo(revtstmp);
CREATE INDEX idx_financial_entries_aud_id ON financial_entries_aud(id);
CREATE INDEX idx_financial_entries_aud_rev ON financial_entries_aud(rev);
CREATE INDEX idx_financial_entries_aud_status ON financial_entries_aud(status);
CREATE INDEX idx_financial_entries_aud_id_rev_desc ON financial_entries_aud(id, rev DESC);
