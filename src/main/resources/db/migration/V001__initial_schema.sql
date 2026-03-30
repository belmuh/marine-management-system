-- Organizations
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
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Users
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
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by_id UUID,
    updated_by_id UUID,
    version BIGINT NOT NULL DEFAULT 0
);

-- Main Categories (Global)
CREATE TABLE main_categories (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name_tr VARCHAR(100) NOT NULL,
    name_en VARCHAR(100) NOT NULL,
    is_technical BOOLEAN NOT NULL DEFAULT true,
    display_order INTEGER,
    budget_guideline_min VARCHAR(10),
    budget_guideline_max VARCHAR(10),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- WHO (Global)
CREATE TABLE who (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name_tr VARCHAR(100) NOT NULL,
    name_en VARCHAR(100) NOT NULL,
    is_technical BOOLEAN NOT NULL DEFAULT true,
    suggested_main_category_id BIGINT REFERENCES main_categories(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Tenant Main Categories
CREATE TABLE tenant_main_categories (
    id UUID PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    main_category_id BIGINT NOT NULL REFERENCES main_categories(id),
    budget_percentage NUMERIC(5,2),
    accounting_code VARCHAR(50),
    is_active BOOLEAN NOT NULL DEFAULT true,
    notes VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by_id UUID,
    updated_by_id UUID,
    UNIQUE(tenant_id, main_category_id)
);

-- Tenant WHO Selections
CREATE TABLE tenant_who_selections (
    id UUID PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    who_id BIGINT NOT NULL REFERENCES who(id),
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by_id UUID,
    updated_by_id UUID,
    UNIQUE(tenant_id, who_id)
);

-- Financial Categories
CREATE TABLE financial_categories (
    id UUID PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    is_active BOOLEAN NOT NULL DEFAULT true,
    is_technical BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by_id UUID,
    updated_by_id UUID,
    UNIQUE(tenant_id, code)
);

-- Financial Entries (UPDATED with Approval & Payment Support)
CREATE TABLE financial_entries (
    id UUID PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    entry_number VARCHAR(50) NOT NULL,

    -- Status (NEW)
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',

    entry_type VARCHAR(20) NOT NULL,
    category_id UUID NOT NULL REFERENCES financial_categories(id),
    tenant_who_id UUID REFERENCES tenant_who_selections(id),
    tenant_main_category_id UUID REFERENCES tenant_main_categories(id),

    -- Requested amounts (crew's original request - IMMUTABLE)
    original_amount NUMERIC(19,4) NOT NULL,
    original_currency VARCHAR(3) NOT NULL,
    base_amount NUMERIC(19,4) NOT NULL,
    base_currency VARCHAR(3) NOT NULL,

    -- Approved amounts (final approved after approval chain) (NEW)
    approved_base_amount NUMERIC(19,4),
    approved_base_currency VARCHAR(3),

    -- Paid amounts (actual payment made) (NEW)
    paid_base_amount NUMERIC(19,4) DEFAULT 0,
    paid_base_currency VARCHAR(3),

    -- Exchange rate (snapshot at entry date)
    exchange_rate NUMERIC(19,6),
    exchange_rate_date DATE,

    entry_date DATE NOT NULL,
    payment_method VARCHAR(20) NOT NULL,
    description TEXT,
    receipt_number VARCHAR(100),

    -- Location
    country VARCHAR(50),
    city VARCHAR(50),
    specific_location VARCHAR(200),

    vendor VARCHAR(100),
    recipient VARCHAR(50),

    -- Metadata
    frequency VARCHAR(20),
    priority VARCHAR(20),
    tags VARCHAR(500),

    -- Audit fields
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    deleted_at TIMESTAMP,
    deleted_by_id UUID,
    created_by_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by_id UUID,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,

    -- Constraints (UPDATED: tenant-specific unique)
    CONSTRAINT uq_financial_entries_tenant_entry_number UNIQUE (tenant_id, entry_number)
);

-- Financial Entry Attachments
CREATE TABLE financial_entry_attachments (
    id UUID PRIMARY KEY,
    entry_id UUID NOT NULL REFERENCES financial_entries(id) ON DELETE CASCADE,
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_size BIGINT NOT NULL,
    file_type VARCHAR(100),
    uploaded_by_id UUID NOT NULL,
    uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Entry Approvals (NEW - Audit trail for approval chain)
CREATE TABLE entry_approvals (
    id UUID PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    entry_id UUID NOT NULL REFERENCES financial_entries(id) ON DELETE CASCADE,

    -- Approval level (CAPTAIN, MANAGER)
    approval_level VARCHAR(20) NOT NULL,

    -- Amounts at this approval level
    requested_amount NUMERIC(19,4) NOT NULL,
    requested_currency VARCHAR(3) NOT NULL,
    approved_amount NUMERIC(19,4),
    approved_currency VARCHAR(3),

    -- Approval decision
    approval_status VARCHAR(20) NOT NULL, -- PENDING, APPROVED, PARTIAL, REJECTED
    rejection_reason VARCHAR(500),

    -- Approver info
    approver_id UUID REFERENCES users(id),
    approval_date TIMESTAMP,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Payments (NEW - Payment history)
CREATE TABLE payments (
    id UUID PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    entry_id UUID NOT NULL REFERENCES financial_entries(id) ON DELETE CASCADE,

    -- Payment details
    amount NUMERIC(19,4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    payment_date DATE NOT NULL,
    payment_reference VARCHAR(100),
    payment_method VARCHAR(20),

    notes VARCHAR(500),

    -- Audit
    recorded_by_id UUID NOT NULL,
    recorded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================================
-- INDEXES
-- ============================================================================

-- Organizations
CREATE INDEX idx_organizations_subscription ON organizations(subscription_status);

-- Users
CREATE INDEX idx_users_organization ON users(organization_id);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role ON users(role);

-- Tenant Main Categories
CREATE INDEX idx_tenant_main_categories_tenant ON tenant_main_categories(tenant_id);
CREATE INDEX idx_tenant_main_categories_main_cat ON tenant_main_categories(main_category_id);

-- Tenant WHO Selections
CREATE INDEX idx_tenant_who_tenant ON tenant_who_selections(tenant_id);
CREATE INDEX idx_tenant_who_who ON tenant_who_selections(who_id);

-- Financial Categories
CREATE INDEX idx_financial_categories_tenant ON financial_categories(tenant_id);
CREATE INDEX idx_financial_categories_code ON financial_categories(tenant_id, code);

-- Financial Entries (UPDATED with approval workflow indexes)
CREATE INDEX idx_financial_entries_tenant ON financial_entries(tenant_id);
CREATE INDEX idx_financial_entries_tenant_date ON financial_entries(tenant_id, entry_date);
CREATE INDEX idx_financial_entries_tenant_entry_number ON financial_entries(tenant_id, entry_number);
CREATE INDEX idx_financial_entries_category ON financial_entries(category_id);
CREATE INDEX idx_financial_entries_tenant_who ON financial_entries(tenant_who_id);
CREATE INDEX idx_financial_entries_tenant_main_cat ON financial_entries(tenant_main_category_id);

-- NEW: Approval workflow indexes
CREATE INDEX idx_financial_entries_status ON financial_entries(status);
CREATE INDEX idx_financial_entries_tenant_status ON financial_entries(tenant_id, status);
CREATE INDEX idx_financial_entries_tenant_created_by ON financial_entries(tenant_id, created_by_id);
CREATE INDEX idx_financial_entries_tenant_date_status ON financial_entries(tenant_id, entry_date, status);

-- Entry Approvals
CREATE INDEX idx_entry_approvals_entry ON entry_approvals(entry_id);
CREATE INDEX idx_entry_approvals_level_status ON entry_approvals(approval_level, approval_status);
CREATE INDEX idx_entry_approvals_approver ON entry_approvals(approver_id);

-- Payments
CREATE INDEX idx_payments_tenant ON payments(tenant_id);
CREATE INDEX idx_payments_entry ON payments(entry_id);
CREATE INDEX idx_payments_date ON payments(payment_date);

-- Financial Entry Attachments
CREATE INDEX idx_financial_entry_attachments_entry ON financial_entry_attachments(entry_id);