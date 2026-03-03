package com.marine.management.modules.finance.infrastructure;

import com.marine.management.modules.finance.domain.entities.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    /**
     * Find all payments for an entry
     */
    List<Payment> findByEntry_IdOrderByPaymentDateDesc(UUID entryId);

    /**
     * Find payments within date range
     */
    List<Payment> findByPaymentDateBetweenOrderByPaymentDateDesc(
            LocalDate startDate,
            LocalDate endDate
    );

    /**
     * Find payments by recorded user
     */
    List<Payment> findByRecordedBy_IdOrderByRecordedAtDesc(UUID userId);

    /**
     * Sum of payments for an entry
     */
// PaymentRepository.java
    @Query("""
    SELECT COALESCE(SUM(p.amount.amount), 0)
    FROM Payment p
    WHERE p.entry.id = :entryId
""")
    BigDecimal sumPaymentsByEntryId(@Param("entryId") UUID entryId);

    /**
     * Count payments for an entry
     */
    long countByEntry_Id(UUID entryId);

    /**
     * Find recent payments (last 30 days)
     */
    @Query("""
        SELECT p FROM Payment p
        WHERE p.paymentDate >= :fromDate
        ORDER BY p.paymentDate DESC
    """)
    List<Payment> findRecentPayments(@Param("fromDate") LocalDate fromDate);
}