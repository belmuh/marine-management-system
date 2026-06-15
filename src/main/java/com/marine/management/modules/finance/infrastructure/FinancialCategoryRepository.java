package com.marine.management.modules.finance.infrastructure;

import com.marine.management.modules.finance.domain.entities.FinancialCategory;
import com.marine.management.modules.finance.domain.enums.RecordType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FinancialCategoryRepository extends JpaRepository<FinancialCategory, UUID> {

    List<FinancialCategory> findByEnabledTrueOrderByDisplayOrderAsc();

    List<FinancialCategory> findAllByOrderByDisplayOrderAsc();

    long countByEnabledTrue();

    Optional<FinancialCategory> findByName(String name);

    boolean existsByName(String name);

    /**
     * Tenant-EXPLICIT queries — do not rely on the Hibernate tenant filter.
     * The filter is only activated by TenantFilter inside an authenticated HTTP request;
     * onboarding and CommandLineRunner contexts (demo data) run WITHOUT the filter,
     * so filter-dependent queries would see other tenants' rows there.
     */
    long countByTenantId(Long tenantId);

    List<FinancialCategory> findByTenantIdOrderByDisplayOrderAsc(Long tenantId);

    @Query("SELECT c FROM FinancialCategory c WHERE " +
            "LOWER(c.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<FinancialCategory> search(@Param("searchTerm") String searchTerm);

    //  isActive → enabled
    @Query("SELECT c, COUNT(e.id) as usageCount " +
            "FROM FinancialCategory c " +
            "LEFT JOIN FinancialEntry e ON e.category.id = c.id " +
            "AND e.entryDate > :oneYearAgo " +
            "AND e.entryType = :entryType " +
            "WHERE c.categoryType = :entryType " +
            "AND c.enabled = true " +
            "GROUP BY c.id " +
            "ORDER BY COUNT(e.id) DESC, c.displayOrder ASC")
    List<CategoryWithUsageCount> findByTypeWithUsageCount(
            @Param("entryType") RecordType entryType,
            @Param("oneYearAgo") LocalDate oneYearAgo
    );

    interface CategoryWithUsageCount {
        FinancialCategory getCategory();
        Long getUsageCount();
    }
}