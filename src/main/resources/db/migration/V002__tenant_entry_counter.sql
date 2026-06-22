-- ============================================================
-- Tenant-scoped entry numaralandırma
--
-- Global financial_entry_seq yerine her (tenant_id, year)
-- çifti kendi sayacını tutar. Böylece:
--   - Her tenant yılda 0001'den başlar
--   - Numaralar deliksiz (komşu tenant trafiğinden etkilenmez)
--   - INSERT ON CONFLICT atomik → race condition yok
--
-- Eski sequence (financial_entry_seq) kasıtlı bırakıldı —
-- rollback gerekirse Java kodu eski haline alınır,
-- sequence zaten DB'de hazır olur. V003'te temizlenebilir.
-- ============================================================

CREATE TABLE tenant_entry_counter (
    tenant_id BIGINT NOT NULL,
    year      INT    NOT NULL,
    last_seq  INT    NOT NULL DEFAULT 0,
    PRIMARY KEY (tenant_id, year)
);
