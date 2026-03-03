package com.marine.management.modules.finance.application;

import com.marine.management.modules.finance.domain.entities.FinancialEntry;
import com.marine.management.modules.finance.domain.entities.Payment;
import com.marine.management.modules.finance.domain.enums.PaymentMethod;
import com.marine.management.modules.finance.domain.vo.Money;
import com.marine.management.modules.finance.infrastructure.FinancialEntryRepository;
import com.marine.management.modules.finance.infrastructure.PaymentRepository;
import com.marine.management.modules.finance.presentation.dto.EntryResponseDto;
import com.marine.management.modules.finance.presentation.dto.PaymentResponseDto;
import com.marine.management.modules.users.domain.User;
import com.marine.management.shared.exceptions.EntryNotFoundException;
import com.marine.management.shared.multitenant.TenantContext;
import com.marine.management.shared.security.EntryAccessPolicy;
import com.marine.management.shared.security.Permission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);

    private final FinancialEntryRepository entryRepository;
    private final PaymentRepository paymentRepository;
    private final EntryAccessPolicy accessPolicy;  // 🆕

    public PaymentService(
            FinancialEntryRepository entryRepository,
            PaymentRepository paymentRepository,
            EntryAccessPolicy accessPolicy
    ) {
        this.entryRepository = entryRepository;
        this.paymentRepository = paymentRepository;
        this.accessPolicy = accessPolicy;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // RECORD PAYMENT
    // ═══════════════════════════════════════════════════════════════════════════

    @Transactional
    public EntryResponseDto recordPayment(
            UUID entryId,
            Money amount,
            LocalDate paymentDate,
            String paymentReference,
            PaymentMethod paymentMethod,
            String notes,
            User recorder
    ) {
        guardTenantContext();
        verifyUserBelongsToCurrentTenant(recorder);

        // Permission check - permission-based, not role-based
        if (!recorder.getRoleEnum().hasPermission(Permission.PAYMENT_CREATE)) {
            throw new AccessDeniedException("User does not have permission to record payments");
        }

        FinancialEntry entry = findEntryOrThrow(entryId);

        // Entry must be payable (APPROVED or PARTIALLY_PAID)
        if (!entry.getStatus().isPayable()) {  // 🆕 isPayable() kullan
            throw new IllegalStateException(
                    String.format("Entry must be APPROVED or PARTIALLY_PAID to record payment. Current status: %s",
                            entry.getStatus())
            );
        }

        // Create payment record
        Payment payment = Payment.create(
                entry,
                amount,
                paymentDate,
                paymentReference,
                paymentMethod,
                notes,
                recorder
        );

        paymentRepository.save(payment);

        // Update entry paid amount (this auto-updates status)
        entry.recordPayment(amount);  // 🆕 User parametresi kaldırıldı

        logger.info("Payment recorded: entry={}, amount={}, date={}, status={}, by={}",
                entryId, amount, paymentDate, entry.getStatus(), recorder.getUsername());

        return EntryResponseDto.from(entry);
    }

    @Transactional
    public EntryResponseDto recordFullPayment(
            UUID entryId,
            LocalDate paymentDate,
            String paymentReference,
            PaymentMethod paymentMethod,
            String notes,
            User recorder
    ) {
        FinancialEntry entry = findEntryOrThrow(entryId);
        Money remainingAmount = entry.getRemainingAmount();

        if (remainingAmount.isZero()) {
            throw new IllegalStateException("Entry is already fully paid");
        }

        return recordPayment(
                entryId,
                remainingAmount,
                paymentDate,
                paymentReference,
                paymentMethod,
                notes,
                recorder
        );
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // UPDATE PAYMENT
    // ═══════════════════════════════════════════════════════════════════════════

    @Transactional
    public PaymentResponseDto updatePayment(
            UUID paymentId,
            String paymentReference,
            PaymentMethod paymentMethod,
            String notes,
            User updater
    ) {
        guardTenantContext();
        verifyUserBelongsToCurrentTenant(updater);

        if (!updater.getRoleEnum().hasPermission(Permission.PAYMENT_EDIT)) {  // 🆕
            throw new AccessDeniedException("User does not have permission to update payments");
        }

        Payment payment = findPaymentOrThrow(paymentId);

        payment.updateDetails(paymentReference, paymentMethod, notes);  // 🆕 User kaldırıldı

        logger.info("Payment updated: id={}, by={}", paymentId, updater.getUsername());

        return PaymentResponseDto.from(payment);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // DELETE PAYMENT (Reversal)
    // ═══════════════════════════════════════════════════════════════════════════

    @Transactional
    public EntryResponseDto deletePayment(UUID paymentId, User deleter) {
        guardTenantContext();
        verifyUserBelongsToCurrentTenant(deleter);

        // Only users with PAYMENT_DELETE permission can delete
        if (!deleter.getRoleEnum().hasPermission(Permission.PAYMENT_DELETE)) {  // 🆕
            throw new AccessDeniedException("User does not have permission to delete payments");
        }

        Payment payment = findPaymentOrThrow(paymentId);
        FinancialEntry entry = payment.getEntry();

        // Reverse payment on entry
        Money reversalAmount = payment.getAmount().negate();
        entry.recordPayment(reversalAmount);  // 🆕 User kaldırıldı

        // Delete payment record
        paymentRepository.delete(payment);

        logger.warn("Payment deleted (REVERSAL): id={}, entry={}, amount={}, by={}",
                paymentId, entry.getEntryNumber(), payment.getAmount(), deleter.getUsername());

        return EntryResponseDto.from(entry);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // QUERY METHODS
    // ═══════════════════════════════════════════════════════════════════════════

    public List<PaymentResponseDto> getPaymentsByEntry(UUID entryId, User currentUser) {
        guardTenantContext();

        // Access control - user must be able to view the entry
        FinancialEntry entry = findEntryOrThrow(entryId);
        accessPolicy.checkReadAccess(entry, currentUser);  // 🆕

        return paymentRepository.findByEntry_IdOrderByPaymentDateDesc(entryId)
                .stream()
                .map(PaymentResponseDto::from)
                .toList();
    }

    public List<PaymentResponseDto> getPaymentsByDateRange(
            LocalDate startDate,
            LocalDate endDate,
            User currentUser
    ) {
        guardTenantContext();

        // Only users with PAYMENT_VIEW can see all payments
        if (!currentUser.getRoleEnum().hasPermission(Permission.PAYMENT_VIEW)) {
            throw new AccessDeniedException("User does not have permission to view payments");
        }

        return paymentRepository.findByPaymentDateBetweenOrderByPaymentDateDesc(startDate, endDate)
                .stream()
                .map(PaymentResponseDto::from)
                .toList();
    }

    public List<PaymentResponseDto> getRecentPayments(User currentUser) {
        guardTenantContext();

        if (!currentUser.getRoleEnum().hasPermission(Permission.PAYMENT_VIEW)) {
            throw new AccessDeniedException("User does not have permission to view payments");
        }

        LocalDate thirtyDaysAgo = LocalDate.now().minusDays(30);

        return paymentRepository.findRecentPayments(thirtyDaysAgo)
                .stream()
                .map(PaymentResponseDto::from)
                .toList();
    }

    public PaymentSummary getPaymentSummary(UUID entryId, User currentUser) {
        guardTenantContext();

        FinancialEntry entry = findEntryOrThrow(entryId);
        accessPolicy.checkReadAccess(entry, currentUser);  // 🆕

        BigDecimal totalPaidAmount = paymentRepository.sumPaymentsByEntryId(entryId);

        Money totalPaid = Money.of(
                totalPaidAmount != null ? totalPaidAmount.toPlainString() : "0",
                FinancialEntry.BASE_CURRENCY
        );

        long paymentCount = paymentRepository.countByEntry_Id(entryId);

        return new PaymentSummary(
                entry.getApprovedBaseAmount(),
                totalPaid,
                entry.getRemainingAmount(),
                paymentCount,
                entry.isFullyPaid()
        );
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // HELPER METHODS
    // ═══════════════════════════════════════════════════════════════════════════

    private void guardTenantContext() {
        if (!TenantContext.hasTenantContext()) {
            throw new AccessDeniedException("No tenant context available");
        }
    }

    private void verifyUserBelongsToCurrentTenant(User user) {
        Long currentTenantId = TenantContext.getCurrentTenantId();
        Long userTenantId = user.getOrganizationId();

        if (!currentTenantId.equals(userTenantId)) {
            throw new AccessDeniedException("User does not belong to current tenant");
        }
    }

    private FinancialEntry findEntryOrThrow(UUID id) {
        return entryRepository.findById(id)
                .orElseThrow(() -> EntryNotFoundException.withId(id));
    }

    private Payment findPaymentOrThrow(UUID id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found: " + id));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // INNER CLASSES
    // ═══════════════════════════════════════════════════════════════════════════

    public record PaymentSummary(
            Money approvedAmount,
            Money totalPaid,
            Money remainingAmount,
            long paymentCount,
            boolean fullyPaid
    ) {}

    public static class PaymentNotFoundException extends RuntimeException {
        public PaymentNotFoundException(String message) {
            super(message);
        }
    }
}