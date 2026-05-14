-- ============================================================================
-- V008: Hibernate Envers Audit Tables
-- Creates revision tracking infrastructure for financial entry change history.
--
-- Tables:
--   revinfo                  → One row per transaction that modified audited entities
--   financial_entries_aud    → Snapshot of audited fields per revision
--
-- Design decisions:
--   - Explicit Flyway migration (not Hibernate auto-DDL)
--   - Only audited fields in _aud table (excludes @NotAudited and @AuditOverride)
--   - Indexes on entity_id + rev for efficient history queries
--   - Custom revinfo with user identity + change context
-- ============================================================================

-- ────────────────────────────────────────────────────────────
-- REVINFO — Custom revision metadata
-- One row per "unit of work" (transaction) that changed audited entities
-- ────────────────────────────────────────────────────────────
CREATE TABLE revinfo (
    rev         SERIAL PRIMARY KEY,
    revtstmp    BIGINT NOT NULL,

    -- Who made the change
    user_id             UUID,
    username            VARCHAR(100),
    user_display_name   VARCHAR(200),

    -- Change context
    source              VARCHAR(20) NOT NULL DEFAULT 'API',
    correlation_id      VARCHAR(100)
);

CREATE INDEX idx_revinfo_user_id ON revinfo(user_id);
CREATE INDEX idx_revinfo_timestamp ON revinfo(revtstmp);

-- ────────────────────────────────────────────────────────────
-- FINANCIAL_ENTRIES_AUD — Audited field snapshots
-- Only contains fields NOT marked with @NotAudited or @AuditOverride(isAudited=false)
--
-- Audited fields:
--   Core:     status, entry_type, entry_number
--   Amounts:  original_amount/currency, base_amount/currency,
--             approved_base_amount/currency, paid_base_amount/currency
--   Business: entry_date, payment_method, receipt_number
--   Exchange: exchange_rate, exchange_rate_date
--   Context:  category_id, tenant_who_id, tenant_main_category_id
--   Audit:    created_at, created_by_id (from BaseAuditedEntity, kept for "who created")
--
-- Excluded (@NotAudited / @AuditOverride):
--   description, recipient, country, city, specific_location, vendor,
--   frequency, priority, tags, rejection_reason,
--   version, is_deleted, deleted_at, deleted_by_id, updated_at, updated_by_id, tenant_id
--   attachments, payments, approvals (collections)
-- ────────────────────────────────────────────────────────────
CREATE TABLE financial_entries_aud (
    id              UUID NOT NULL,
    rev             INTEGER NOT NULL REFERENCES revinfo(rev),
    revtype         SMALLINT,

    -- Core
    entry_number    VARCHAR(50),
    status          VARCHAR(20),
    entry_type      VARCHAR(20),

    -- Amounts
    original_amount     NUMERIC(19,4),
    original_currency   VARCHAR(3),
    base_amount         NUMERIC(19,4),
    base_currency       VARCHAR(3),
    approved_base_amount    NUMERIC(19,4),
    approved_base_currency  VARCHAR(3),
    paid_base_amount    NUMERIC(19,4),
    paid_base_currency  VARCHAR(3),

    -- Business
    entry_date          DATE,
    payment_method      VARCHAR(20),
    receipt_number      VARCHAR(100),

    -- Exchange rate snapshot
    exchange_rate       NUMERIC(19,6),
    exchange_rate_date  DATE,

    -- Context references (FK IDs only, target not audited)
    category_id                 UUID,
    tenant_who_id               UUID,
    tenant_main_category_id     UUID,

    -- Audit (from BaseAuditedEntity — only created_at/by kept)
    created_at      TIMESTAMP,
    created_by_id   UUID,

    PRIMARY KEY (id, rev)
);

-- Performance indexes for history queries
CREATE INDEX idx_financial_entries_aud_id ON financial_entries_aud(id);
CREATE INDEX idx_financial_entries_aud_rev ON financial_entries_aud(rev);
CREATE INDEX idx_financial_entries_aud_status ON financial_entries_aud(status);

-- Composite index: history query for a specific entity (newest first)
CREATE INDEX idx_financial_entries_aud_id_rev_desc ON financial_entries_aud(id, rev DESC);
