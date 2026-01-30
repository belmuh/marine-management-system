package com.marine.management.modules.finance.infrastructure;

import com.marine.management.modules.finance.domain.entity.TenantMainCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantMainCategoryRepository extends JpaRepository<TenantMainCategory, UUID> {

    //  Ana liste query'si - DTO dönüşümü için optimize
    @Query("""
        SELECT tmc FROM TenantMainCategory tmc 
        JOIN FETCH tmc.mainCategory mc
        WHERE tmc.enabled = true 
        AND tmc.deleted = false
        ORDER BY mc.displayOrder
        """)
    List<TenantMainCategory> findAllActiveWithMainCategory();

    //  Technical filtreli versi
    @Query("""
        SELECT tmc FROM TenantMainCategory tmc 
        JOIN FETCH tmc.mainCategory mc
        WHERE tmc.enabled = true 
        AND tmc.deleted = false
        AND mc.technical = :technical
        ORDER BY mc.displayOrder
        """)
    List<TenantMainCategory> findByEnabledTrueAndIsTechnicalWithMainCategory(@Param("technical") boolean technical);

    //  Code ile bulma
    @Query("""
        SELECT tmc FROM TenantMainCategory tmc 
        JOIN FETCH tmc.mainCategory mc
        WHERE mc.code = :code
        AND tmc.deleted = false
        """)
    Optional<TenantMainCategory> findByMainCategoryCodeWithMainCategory(@Param("code") String code);

    //  MainCategory ID ile bulma
    @Query("""
        SELECT tmc FROM TenantMainCategory tmc 
        JOIN FETCH tmc.mainCategory mc
        WHERE mc.id = :mainCategoryId
        AND tmc.deleted = false
        """)
    Optional<TenantMainCategory> findByMainCategoryIdWithMainCategory(@Param("mainCategoryId") Long mainCategoryId);

    //  Existence check - JOIN FETCH gereksiz (sadece count)
    boolean existsByMainCategory_IdAndDeletedFalse(Long mainCategoryId);

    //  Silinenleri de getir
    @Query("""
        SELECT tmc FROM TenantMainCategory tmc 
        JOIN FETCH tmc.mainCategory mc
        ORDER BY mc.displayOrder
        """)
    List<TenantMainCategory> findAllIncludingDeletedWithMainCategory();
}