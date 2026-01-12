package com.marine.management.modules.finance.infrastructure;

import com.marine.management.modules.finance.domain.entity.TenantWhoSelection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TenantWhoSelectionRepository extends JpaRepository<TenantWhoSelection, Long> {
    // Hibernate @Filter otomatik tenant filtreleme yapacak
    List<TenantWhoSelection> findByIsActiveTrue();
    List<TenantWhoSelection> findByIsActiveTrueAndWho_Technical(boolean technical);
    List<TenantWhoSelection> findByIsActiveTrueAndWho_SuggestedMainCategoryId(Long mainCategoryId);
    Optional<TenantWhoSelection> findByWho_Code(String code);
}