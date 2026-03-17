-- V004: Add attachment_type column to financial_entry_attachments
-- Existing attachments are classified as DIGER (Other) by default.

ALTER TABLE financial_entry_attachments
    ADD COLUMN attachment_type VARCHAR(20) NOT NULL DEFAULT 'DIGER';

-- Remove default after backfill so future inserts must supply the value explicitly
ALTER TABLE financial_entry_attachments
    ALTER COLUMN attachment_type DROP DEFAULT;

-- Optional index for filtering attachments by type per entry
CREATE INDEX idx_attachments_type ON financial_entry_attachments (entry_id, attachment_type);
