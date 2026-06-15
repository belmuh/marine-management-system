package com.marine.management.shared.multitenant;

import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Boot'un oluşturduğu DataSource'u (Hikari) TenantAwareDataSource ile sarar.
 * JPA, native query, JdbcTemplate, Flyway — tüm DB erişimi bu bean'den
 * geçtiği için tek kapsama noktası burasıdır.
 */
@Configuration
public class TenantDataSourceConfig {

    @Bean
    public static BeanPostProcessor tenantAwareDataSourceWrapper() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                if (bean instanceof DataSource dataSource
                        && !(bean instanceof TenantAwareDataSource)) {
                    return new TenantAwareDataSource(dataSource);
                }
                return bean;
            }
        };
    }
}
