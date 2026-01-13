-- Organizations
CREATE TABLE organizations (
    id BIGSERIAL PRIMARY KEY,
    yacht_name VARCHAR(100) NOT NULL,
    company_name VARCHAR(100),
    flag_country VARCHAR(2) NOT NULL,
    base_currency VARCHAR(3) NOT NULL,
    subscription_status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Users
CREATE TABLE users (
    id UUID PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    tenant_id BIGINT NOT NULL REFERENCES organizations(id),
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
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
    UNIQUE(tenant_id, code)
);

-- Financial Entries
CREATE TABLE financial_entries (
    id UUID PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    entry_number VARCHAR(20) NOT NULL UNIQUE,
    entry_type VARCHAR(20) NOT NULL,
    category_id UUID NOT NULL REFERENCES financial_categories(id),
    tenant_who_id UUID REFERENCES tenant_who_selections(id),
    tenant_main_category_id UUID REFERENCES tenant_main_categories(id),
    amount NUMERIC(15,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    exchange_rate NUMERIC(10,6),
    base_amount NUMERIC(15,2),
    base_currency VARCHAR(3),
    entry_date DATE NOT NULL,
    payment_method VARCHAR(20) NOT NULL,
    description TEXT,
    receipt_number VARCHAR(50),
    country VARCHAR(100),
    location_city VARCHAR(100),
    location_country VARCHAR(100),
    vendor VARCHAR(200),
    recipient VARCHAR(200),
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX idx_tenant_main_categories_tenant ON tenant_main_categories(tenant_id);
CREATE INDEX idx_tenant_who_tenant ON tenant_who_selections(tenant_id);
CREATE INDEX idx_financial_categories_tenant ON financial_categories(tenant_id);
CREATE INDEX idx_financial_entries_tenant ON financial_entries(tenant_id);
CREATE INDEX idx_financial_entries_date ON financial_entries(entry_date);