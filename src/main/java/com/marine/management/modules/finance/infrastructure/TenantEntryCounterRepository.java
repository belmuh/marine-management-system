package com.marine.management.modules.finance.infrastructure;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Tenant ve yıl bazında atomik entry sayacı.
 *
 * Her (tenant_id, year) çifti için ayrı bir sayaç tutar.
 * Yeni yılda sayaç otomatik olarak 1'den başlar.
 *
 * Neden JdbcTemplate?
 * Spring Data JPA, RETURNING clause'u olan INSERT ON CONFLICT
 * sorgularını native query olarak desteklemez. JdbcTemplate
 * bu sorguyu doğrudan çalıştırır ve dönen değeri alır.
 *
 * Neden INSERT ON CONFLICT?
 * SELECT ... FOR UPDATE'e göre daha basit ve aynı şekilde atomik.
 * PostgreSQL bu işlemi tek bir lock cycle'da tamamlar.
 */
@Repository
public class TenantEntryCounterRepository {

    private final JdbcTemplate jdbcTemplate;

    public TenantEntryCounterRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Bu tenant ve yıl için bir sonraki sıra numarasını atomik olarak döner.
     * İlk çağrıda 1, sonrakilerde bir öncekinin +1'i gelir.
     *
     * @param tenantId mevcut tenant ID (TenantContext'ten alınır)
     * @param year     kayıt yılı (genellikle bugünün yılı)
     * @return bir sonraki sıra numarası (1'den başlar)
     */
    public int nextSequence(Long tenantId, int year) {
        Integer result = jdbcTemplate.queryForObject(
                """
                INSERT INTO tenant_entry_counter (tenant_id, year, last_seq)
                VALUES (?, ?, 1)
                ON CONFLICT (tenant_id, year)
                DO UPDATE SET last_seq = tenant_entry_counter.last_seq + 1
                RETURNING last_seq
                """,
                Integer.class,
                tenantId,
                year
        );
        if (result == null) {
            throw new IllegalStateException(
                    "tenant_entry_counter RETURNING null — tenantId=%d year=%d"
                            .formatted(tenantId, year)
            );
        }
        return result;
    }
}
