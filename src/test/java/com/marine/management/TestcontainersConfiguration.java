package com.marine.management;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Entegrasyon testleri için gerçek PostgreSQL container'ı.
 *
 * NEDEN H2 DEĞİL:
 * - RLS (Row Level Security) yalnızca gerçek PostgreSQL'de test edilebilir
 * - Flyway migration'ları (V001+) PG-spesifik SQL içerir
 * - Prod ile aynı dialect → "testte geçti, prod'da patladı" sınıfı hatalar elenir
 *
 * @ServiceConnection: datasource URL/kullanıcı/şifre otomatik bağlanır,
 * application-test.properties'te DB konfigürasyonu gerekmez.
 *
 * Kullanım: @SpringBootTest sınıfına @Import(TestcontainersConfiguration.class)
 *
 * Gereksinim: lokalde/CI'da Docker.
 *
 * @see docs/RLS_IMPLEMENTATION_PLAN.md §5-6
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>("postgres:18-alpine");
    }
}
