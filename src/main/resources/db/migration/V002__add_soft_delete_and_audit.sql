

-- users
-- financial_entries
ALTER TABLE financial_entries ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE financial_entries ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE financial_entries ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;
ALTER TABLE financial_entries ADD COLUMN IF NOT EXISTS deleted_by_id UUID;
ALTER TABLE financial_entries ADD COLUMN IF NOT EXISTS created_by_id UUID;
ALTER TABLE financial_entries ADD COLUMN IF NOT EXISTS updated_by_id UUID;

CREATE INDEX IF NOT EXISTS idx_financial_entries_deleted ON financial_entries(is_deleted);

-- users
ALTER TABLE users ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE users ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE users ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;
ALTER TABLE users ADD COLUMN IF NOT EXISTS deleted_by_id UUID;
ALTER TABLE users ADD COLUMN IF NOT EXISTS created_by_id UUID;
ALTER TABLE users ADD COLUMN IF NOT EXISTS updated_by_id UUID;

-- organizations
ALTER TABLE organizations ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE organizations ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE organizations ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;
ALTER TABLE organizations ADD COLUMN IF NOT EXISTS deleted_by_id UUID;
ALTER TABLE organizations ADD COLUMN IF NOT EXISTS created_by_id UUID;
ALTER TABLE organizations ADD COLUMN IF NOT EXISTS updated_by_id UUID;
ALTER TABLE organizations ADD COLUMN IF NOT EXISTS subscription_status VARCHAR(30) NOT NULL DEFAULT 'TRIAL';
ALTER TABLE organizations ADD COLUMN IF NOT EXISTS subscription_expires_at DATE;

-- financial_categories
ALTER TABLE financial_categories ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE financial_categories ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE financial_categories ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;
ALTER TABLE financial_categories ADD COLUMN IF NOT EXISTS deleted_by_id UUID;
ALTER TABLE financial_categories ADD COLUMN IF NOT EXISTS created_by_id UUID;
ALTER TABLE financial_categories ADD COLUMN IF NOT EXISTS updated_by_id UUID;

-- Diğer tablolar (tenant_main_categories, tenant_who_selections) için de
-- aynı şekilde kolonları tek tek ALTER TABLE ile ekleyerek devam edin.

-- tenant_main_categories
ALTER TABLE tenant_main_categories ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE tenant_main_categories ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE tenant_main_categories ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;
ALTER TABLE tenant_main_categories ADD COLUMN IF NOT EXISTS deleted_by_id UUID;
ALTER TABLE tenant_main_categories ADD COLUMN IF NOT EXISTS created_by_id UUID;
ALTER TABLE tenant_main_categories ADD COLUMN IF NOT EXISTS updated_by_id UUID;

-- tenant_who_selections
ALTER TABLE tenant_who_selections ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE tenant_who_selections ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE tenant_who_selections ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;
ALTER TABLE tenant_who_selections ADD COLUMN IF NOT EXISTS deleted_by_id UUID;
ALTER TABLE tenant_who_selections ADD COLUMN IF NOT EXISTS created_by_id UUID;
ALTER TABLE tenant_who_selections ADD COLUMN IF NOT EXISTS updated_by_id UUID;

ALTER TABLE organizations ADD COLUMN IF NOT EXISTS subscription_status VARCHAR(30) NOT NULL DEFAULT 'TRIAL';
ALTER TABLE organizations ADD COLUMN IF NOT EXISTS subscription_expires_at DATE;