package com.marine.management.modules.finance.infrastructure;

import com.marine.management.modules.finance.domain.entity.FinancialEntry;
import com.marine.management.modules.finance.domain.enums.RecordType;
import com.marine.management.modules.finance.presentation.dto.EntryResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.UUID;

@Repository
public interface FinancialEntrySearchRepository
        extends org.springframework.data.repository.Repository<FinancialEntry, UUID> {

    @Query("""
        SELECT new com.marine.management.modules.finance.presentation.dto.EntryResponseDto(
            e.id,
            e.entryNumber.value,
            e.entryType,
            c.id,
            c.code,
            c.name,
            e.originalAmount.amount,
            e.originalAmount.currencyCode,
            e.baseAmount.amount,
            e.baseAmount.currencyCode,
            e.exchangeRate,
            e.exchangeRateDate,
            e.receiptNumber,
            e.description,
            e.entryDate,
            e.paymentMethod,
            CASE WHEN e.tenantWho IS NOT NULL THEN e.tenantWho.who.id ELSE NULL END,
            CASE WHEN e.tenantMainCategory IS NOT NULL THEN e.tenantMainCategory.mainCategory.id ELSE NULL END,
            e.recipient,
            e.country,
            e.city,
            e.specificLocation,
            e.vendor,
            e.createdById,
            e.createdAt,
            e.updatedAt,
            CASE WHEN SIZE(e.attachments) > 0 THEN true ELSE false END
        )
        FROM FinancialEntry e
        LEFT JOIN e.category c
        LEFT JOIN e.tenantWho tw
        LEFT JOIN e.tenantMainCategory tmc
        WHERE (CAST(:categoryId AS string) IS NULL OR c.id = :categoryId)
        AND (CAST(:entryType AS string) IS NULL OR e.entryType = :entryType)
        AND (CAST(:whoId AS string) IS NULL OR tw.who.id = :whoId)
        AND (CAST(:mainCategoryId AS string) IS NULL OR tmc.mainCategory.id = :mainCategoryId)
        AND (CAST(:startDate AS string) IS NULL OR e.entryDate >= :startDate)
        AND (CAST(:endDate AS string) IS NULL OR e.entryDate <= :endDate)
        ORDER BY e.entryDate DESC
        """)
    Page<EntryResponseDto> search(
            @Param("categoryId") UUID categoryId,
            @Param("entryType") RecordType entryType,
            @Param("whoId") Long whoId,
            @Param("mainCategoryId") Long mainCategoryId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable
    );

    @Query("""
        SELECT new com.marine.management.modules.finance.presentation.dto.EntryResponseDto(
            e.id,
            e.entryNumber.value,
            e.entryType,
            c.id,
            c.code,
            c.name,
            e.originalAmount.amount,
            e.originalAmount.currencyCode,
            e.baseAmount.amount,
            e.baseAmount.currencyCode,
            e.exchangeRate,
            e.exchangeRateDate,
            e.receiptNumber,
            e.description,
            e.entryDate,
            e.paymentMethod,
            CASE WHEN e.tenantWho IS NOT NULL THEN e.tenantWho.who.id ELSE NULL END,
            CASE WHEN e.tenantMainCategory IS NOT NULL THEN e.tenantMainCategory.mainCategory.id ELSE NULL END,
            e.recipient,
            e.country,
            e.city,
            e.specificLocation,
            e.vendor,
            e.createdById,
            e.createdAt,
            e.updatedAt,
            CASE WHEN SIZE(e.attachments) > 0 THEN true ELSE false END
        )
        FROM FinancialEntry e
        LEFT JOIN e.category c
        WHERE (LOWER(e.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
            OR LOWER(e.receiptNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')))
        AND (CAST(:entryType AS string) IS NULL OR e.entryType = :entryType)
        AND (CAST(:startDate AS string) IS NULL OR e.entryDate >= :startDate)
        AND (CAST(:endDate AS string) IS NULL OR e.entryDate <= :endDate)
        ORDER BY e.entryDate DESC
        """)
    Page<EntryResponseDto> searchByText(
            @Param("searchTerm") String searchTerm,
            @Param("entryType") RecordType entryType,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable
    );
}