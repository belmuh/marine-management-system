package com.marine.management.modules.finance.infrastructure;

import com.marine.management.modules.finance.domain.FinancialCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FinancialCategoryRepository extends JpaRepository<FinancialCategory, UUID> {

    Optional<FinancialCategory> findByCode(String code);
    List<FinancialCategory> findByIsActiveTrueOrderByDisplayOrderAsc();
    List<FinancialCategory> findAllByOrderByDisplayOrderAsc();

    long countByIsActiveTrue();
    boolean existsByCode(String code);

    @Query("SELECT c FROM FinancialCategory c WHERE " +
            "LOWER(c.code) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(c.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<FinancialCategory> search(@Param("searchTerm") String searchTerm);
}
