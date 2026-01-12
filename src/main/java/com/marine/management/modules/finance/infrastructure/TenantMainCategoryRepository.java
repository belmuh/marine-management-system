package com.marine.management.modules.finance.infrastructure;

import com.marine.management.modules.finance.domain.entity.TenantMainCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TenantMainCategoryRepository extends JpaRepository<TenantMainCategory, Long> {
    // Hibernate @Filter otomatik tenant filtreleme yapacak
    List<TenantMainCategory> findByIsActiveTrue();
    List<TenantMainCategory> findByIsActiveTrueAndMainCategory_Technical(boolean technical);
    Optional<TenantMainCategory> findByMainCategory_Code(String code);
}
