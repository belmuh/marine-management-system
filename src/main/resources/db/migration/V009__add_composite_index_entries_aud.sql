-- ============================================================================
-- V009: Add composite index for efficient history queries
-- Optimizes: SELECT ... FROM financial_entries_aud WHERE id = ? ORDER BY rev DESC
-- ============================================================================

CREATE INDEX IF NOT EXISTS idx_financial_entries_aud_id_rev_desc
    ON financial_entries_aud(id, rev DESC);
