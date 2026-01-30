package com.marine.management.modules.finance.infrastructure;

import com.marine.management.modules.finance.domain.entity.FinancialCategory;
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

    Optional<FinancialCategory> findByCode(String code);

    //  isActive → enabled
    List<FinancialCategory> findByEnabledTrueOrderByDisplayOrderAsc();

    List<FinancialCategory> findAllByOrderByDisplayOrderAsc();

    //  isActive → enabled
    long countByEnabledTrue();

    boolean existsByCode(String code);

    @Query("SELECT c FROM FinancialCategory c WHERE " +
            "LOWER(c.code) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
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