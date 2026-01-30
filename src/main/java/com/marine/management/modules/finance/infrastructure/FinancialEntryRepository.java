package com.marine.management.modules.finance.infrastructure;

import com.marine.management.modules.finance.domain.enums.RecordType;
import com.marine.management.modules.finance.domain.entity.FinancialCategory;
import com.marine.management.modules.finance.domain.entity.FinancialEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FinancialEntryRepository extends JpaRepository<FinancialEntry, UUID> {

    @Query(value = "SELECT NEXTVAL('financial_entry_seq')", nativeQuery = true)
    int getNextSequence();

    Optional<FinancialEntry> findByEntryNumber_Value(String value);

    boolean existsByEntryNumber_Value(String value);

    Page<FinancialEntry> findByCreatedById(UUID userId, Pageable pageable);

    List<FinancialEntry> findByCreatedByIdOrderByEntryDateDesc(UUID userId);

    Page<FinancialEntry> findByEntryDateBetween(LocalDate startDate, LocalDate endDate, Pageable pageable);

    List<FinancialEntry> findByEntryTypeOrderByEntryDateDesc(RecordType type);

    Page<FinancialEntry> findByEntryType(RecordType type, Pageable pageable);

    List<FinancialEntry> findByCategoryOrderByEntryDateDesc(FinancialCategory category);

    Page<FinancialEntry> findByCategory(FinancialCategory category, Pageable pageable);

    Page<FinancialEntry> findByTenantWho_Id(UUID whoId, Pageable pageable);

    List<FinancialEntry> findByTenantWho_IdOrderByEntryDateDesc(UUID whoId);

    Page<FinancialEntry> findByTenantMainCategory_Id(UUID mainCategoryId, Pageable pageable);

    List<FinancialEntry> findByTenantMainCategory_IdOrderByEntryDateDesc(UUID mainCategoryId);

    Page<FinancialEntry> findByTenantWho_IdAndTenantMainCategory_Id(UUID whoId, UUID mainCategoryId, Pageable pageable);

    long countByEntryTypeAndEntryDateBetween(RecordType entryType, LocalDate startDate, LocalDate endDate);

    // Veritabanından tarih aralığındaki kayıtları çek
    @Query("""
        SELECT e.entryDate as entryDate,
               e.category.name as categoryName,
               e.baseAmount.amount as baseAmount,
               e.entryType as entryType
        FROM FinancialEntry e
        WHERE e.entryDate BETWEEN :start AND :end
        ORDER BY e.entryDate DESC
    """)
    List<FinancialEntryProjection> findByEntryDateBetweenOrderByEntryDateDesc(
            LocalDate start,
            LocalDate end
    );

    // Projection arayüzü / class
    interface FinancialEntryProjection {
        LocalDate getEntryDate();

        String getCategoryName();

        BigDecimal getBaseAmount();  // ✅ getAmount() → getBaseAmount()

        RecordType getEntryType();
    }
}