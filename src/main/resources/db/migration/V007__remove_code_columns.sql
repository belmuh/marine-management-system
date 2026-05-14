-- V007: Remove 'code' columns from categories and who tables
-- The 'code' field was an unnecessary technical identifier;
-- name fields are sufficient for user-facing reference data.

-- 1. Drop indexes that reference code
DROP INDEX IF EXISTS idx_financial_categories_code;

-- 2. Drop unique constraint on financial_categories (tenant_id, code)
ALTER TABLE financial_categories DROP CONSTRAINT IF EXISTS uq_financial_categories_tenant_code;

-- 3. Add unique constraint on financial_categories (tenant_id, name)
ALTER TABLE financial_categories ADD CONSTRAINT uq_financial_categories_tenant_name UNIQUE (tenant_id, name);

-- 4. Drop code columns
ALTER TABLE financial_categories DROP COLUMN IF EXISTS code;
ALTER TABLE main_categories DROP COLUMN IF EXISTS code;
ALTER TABLE who DROP COLUMN IF EXISTS code;
