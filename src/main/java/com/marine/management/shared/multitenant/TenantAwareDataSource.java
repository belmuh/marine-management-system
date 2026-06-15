package com.marine.management.shared.multitenant;

import org.springframework.jdbc.datasource.DelegatingDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Pool'dan alınan her bağlantıda PostgreSQL session değişkeni app.tenant_id'yi
 * TenantContext'ten set'ler. RLS policy'leri (V002) bu değişkeni okur.
 *
 * - Tenant context yoksa '' yazılır → policy'ler hiç satır döndürmez (fail-closed).
 * - Her checkout'ta yeniden yazıldığı için pooled bağlantıda önceki
 *   request'in tenant'ı sızamaz.
 * - is_local=false (session-level): transaction dışı autocommit sorgular da kapsanır.
 */
public class TenantAwareDataSource extends DelegatingDataSource {

    private static final String SET_TENANT_SQL =
            "SELECT set_config('app.tenant_id', ?, false)";

    public TenantAwareDataSource(DataSource targetDataSource) {
        super(targetDataSource);
    }

    @Override
    public Connection getConnection() throws SQLException {
        Connection conn = super.getConnection();
        applyTenantSetting(conn);
        return conn;
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        Connection conn = super.getConnection(username, password);
        applyTenantSetting(conn);
        return conn;
    }

    private void applyTenantSetting(Connection conn) throws SQLException {
        String tenant = TenantContext.hasTenantContext()
                ? TenantContext.getCurrentTenantId().toString()
                : "";
        try (PreparedStatement ps = conn.prepareStatement(SET_TENANT_SQL)) {
            ps.setString(1, tenant);
            ps.execute();
        } catch (SQLException e) {
            // set_config başarısızsa bağlantıyı belirsiz tenant durumuyla
            // kullanıma/poola bırakma — kapat ve hatayı yükselt (fail-closed).
            try {
                conn.close();
            } catch (SQLException ignored) {
                // kapatma hatası orijinal hatayı gölgelemesin
            }
            throw e;
        }
    }
}
