package com.marine.management.shared.multitenant;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class TenantFilterAspect {

    private static final Logger log = LoggerFactory.getLogger(TenantFilterAspect.class);

    @PersistenceContext
    private EntityManager entityManager;

    // @Transactional metodlar için daha spesifik pointcut
    @Before("@within(org.springframework.stereotype.Service) || " +
            "@within(org.springframework.web.bind.annotation.RestController) || " +
            "@annotation(org.springframework.transaction.annotation.Transactional)")
    public void enableTenantFilter(JoinPoint joinPoint) {

        if (!TenantContext.hasTenantContext()) {
            return;
        }

        try {
            Long tenantId = TenantContext.getCurrentTenantId();
            Session session = entityManager.unwrap(Session.class);

            // İdempotent - zaten enabled ise skip et
            if (session.getEnabledFilter("tenantFilter") != null) {
                log.trace("Filter already enabled for tenant: {}", tenantId);
                return;
            }

            session.enableFilter("tenantFilter").setParameter("tenantId", tenantId);

            log.trace("🔒 AOP: Filter ENABLED for Tenant: {} in method: {}",
                    tenantId, joinPoint.getSignature().getName());

        } catch (Exception e) {
            log.error(" AOP: Failed to enable tenant filter", e);
        }
    }
}