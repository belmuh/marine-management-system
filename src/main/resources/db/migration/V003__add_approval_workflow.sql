-- ============================================================================
-- V003: Add Approval & Payment Workflow Support
-- ============================================================================

-- 1. Update financial_entries table - Add approval workflow columns
ALTER TABLE financial_entries
    ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'DRAFT',
    ADD COLUMN IF NOT EXISTS approved_base_amount NUMERIC(19,4),
    ADD COLUMN IF NOT EXISTS approved_base_currency VARCHAR(3),
    ADD COLUMN IF NOT EXISTS paid_base_amount NUMERIC(19,4) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS paid_base_currency VARCHAR(3);

-- Make status NOT NULL
UPDATE financial_entries SET status = 'DRAFT' WHERE status IS NULL;
ALTER TABLE financial_entries ALTER COLUMN status SET NOT NULL;

-- 2. Add audit fields to entry_approvals (table already exists from V001)
ALTER TABLE entry_approvals
    ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS deleted_by_id UUID,
    ADD COLUMN IF NOT EXISTS created_by_id UUID,
    ADD COLUMN IF NOT EXISTS updated_by_id UUID,
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

-- 3. Add indexes for approval workflow
CREATE INDEX IF NOT EXISTS idx_financial_entries_status
    ON financial_entries(status);

CREATE INDEX IF NOT EXISTS idx_financial_entries_tenant_status
    ON financial_entries(tenant_id, status);

CREATE INDEX IF NOT EXISTS idx_financial_entries_tenant_created_by
    ON financial_entries(tenant_id, created_by_id);

CREATE INDEX IF NOT EXISTS idx_financial_entries_tenant_date_status
    ON financial_entries(tenant_id, entry_date, status);

-- 4. Add indexes for entry_approvals
CREATE INDEX IF NOT EXISTS idx_entry_approvals_tenant
    ON entry_approvals(tenant_id);

CREATE INDEX IF NOT EXISTS idx_entry_approvals_entry
    ON entry_approvals(entry_id);

CREATE INDEX IF NOT EXISTS idx_entry_approvals_level_status
    ON entry_approvals(approval_level, approval_status);

CREATE INDEX IF NOT EXISTS idx_entry_approvals_approver
    ON entry_approvals(approver_id);

-- 5. Add indexes for payments
CREATE INDEX IF NOT EXISTS idx_payments_tenant
    ON payments(tenant_id);

CREATE INDEX IF NOT EXISTS idx_payments_entry
    ON payments(entry_id);

CREATE INDEX IF NOT EXISTS idx_payments_date
    ON payments(payment_date);

CREATE INDEX IF NOT EXISTS idx_payments_recorded_by
    ON payments(recorded_by_id);