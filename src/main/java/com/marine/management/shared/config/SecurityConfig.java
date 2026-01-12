package com.marine.management.shared.config;

import com.marine.management.shared.multitenant.TenantFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Spring Security configuration with JWT authentication and multi-tenant isolation.
 *
 * Architecture Principles:
 * - Stateless authentication (JWT-based)
 * - Tenant isolation via ThreadLocal context
 * - Loose coupling: TenantFilter independent of auth mechanism
 * - Role-based access control (RBAC)
 *
 * Filter Chain Order:
 * 1. JwtAuthenticationFilter - Authenticates request (before UsernamePasswordAuthenticationFilter)
 * 2. TenantFilter - Extracts tenant context (after authentication completes)
 *
 * Note: TenantFilter references UsernamePasswordAuthenticationFilter (Spring built-in)
 * rather than JwtAuthenticationFilter to maintain loose coupling. This allows
 * changing authentication mechanisms without affecting tenant isolation logic.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;
    private final TenantFilter tenantFilter;
    private final CorsConfigurationSource corsConfigurationSource;

    public SecurityConfig(
            JwtAuthenticationFilter jwtFilter,
            TenantFilter tenantFilter,
            CorsConfigurationSource corsConfigurationSource
    ) {
        this.jwtFilter = jwtFilter;
        this.tenantFilter = tenantFilter;
        this.corsConfigurationSource = corsConfigurationSource;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(authz -> authz
                        // Public endpoints
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/actuator/health").permitAll()

                        // Protected endpoints (authority-based, not role-based)
                        .requestMatchers("/api/users/**")
                        .hasAnyAuthority("SUPER_ADMIN", "ADMIN", "MANAGER")

                        .requestMatchers("/api/finance/**")
                        .hasAnyAuthority("SUPER_ADMIN", "ADMIN", "MANAGER", "CAPTAIN", "USER")

                        .requestMatchers("/api/reports/**")
                        .hasAnyAuthority("SUPER_ADMIN", "ADMIN", "MANAGER", "CAPTAIN")

                        .anyRequest().authenticated()
                )
                // Filter chain: JWT auth → Tenant context
                // Both filters are positioned relative to Spring's UsernamePasswordAuthenticationFilter
                // to maintain loose coupling and allow flexible auth mechanism changes
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(tenantFilter, UsernamePasswordAuthenticationFilter.class)

                .headers(headers -> headers
                        .frameOptions(frame -> frame.deny())
                );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config
    ) throws Exception {
        return config.getAuthenticationManager();
    }
}