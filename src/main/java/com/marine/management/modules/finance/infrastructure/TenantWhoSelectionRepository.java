package com.marine.management.modules.finance.infrastructure;

import com.marine.management.modules.finance.domain.entities.TenantWhoSelection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantWhoSelectionRepository extends JpaRepository<TenantWhoSelection, UUID> {

    // Ana liste query'si - DTO dönüşümü için optimize
    @Query("""
        SELECT tws FROM TenantWhoSelection tws 
        JOIN FETCH tws.who w
        WHERE tws.enabled = true 
        AND tws.deleted = false
        ORDER BY w.displayOrder
        """)
    List<TenantWhoSelection> findAllActiveWithWho();

    // Technical filtreli versi
    @Query("""
        SELECT tws FROM TenantWhoSelection tws 
        JOIN FETCH tws.who w
        WHERE tws.enabled = true 
        AND tws.deleted = false
        AND w.technical = :technical
        ORDER BY w.displayOrder
        """)
    List<TenantWhoSelection> findByEnabledTrueAndTechnicalWithWho(@Param("technical") boolean technical);

    // Suggested main category filtreli
    @Query("""
        SELECT tws FROM TenantWhoSelection tws 
        JOIN FETCH tws.who w
        WHERE tws.enabled = true 
        AND tws.deleted = false
        AND w.suggestedMainCategoryId = :mainCategoryId
        ORDER BY w.displayOrder
        """)
    List<TenantWhoSelection> findBySuggestedMainCategoryWithWho(@Param("mainCategoryId") Long mainCategoryId);

    // Code ile bulma
    @Query("""
        SELECT tws FROM TenantWhoSelection tws 
        JOIN FETCH tws.who w
        WHERE w.code = :code
        AND tws.deleted = false
        """)
    Optional<TenantWhoSelection> findByWhoCodeWithWho(@Param("code") String code);

    // Who ID ile bulma
    @Query("""
        SELECT tws FROM TenantWhoSelection tws 
        JOIN FETCH tws.who w
        WHERE w.id = :whoId
        AND tws.deleted = false
        """)
    Optional<TenantWhoSelection> findByWhoIdWithWho(@Param("whoId") Long whoId);

    // Existence check - JOIN FETCH gereksiz (sadece count)
    boolean existsByWho_IdAndDeletedFalse(Long whoId);

    // Override findById - JOIN FETCH ile
    @Query("""
        SELECT tws FROM TenantWhoSelection tws 
        JOIN FETCH tws.who w
        WHERE tws.id = :id
        AND tws.deleted = false
        """)
    Optional<TenantWhoSelection> findByIdWithWho(@Param("id") UUID id);
}