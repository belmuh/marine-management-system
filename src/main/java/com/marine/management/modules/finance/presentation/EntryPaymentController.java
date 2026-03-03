package com.marine.management.modules.finance.presentation;

import com.marine.management.modules.finance.application.PaymentService;
import com.marine.management.modules.finance.domain.enums.PaymentMethod;
import com.marine.management.modules.finance.domain.vo.Money;
import com.marine.management.modules.finance.presentation.dto.EntryResponseDto;
import com.marine.management.modules.finance.presentation.dto.PaymentResponseDto;
import com.marine.management.modules.finance.presentation.dto.controller.RecordPaymentRequest;
import com.marine.management.modules.users.domain.User;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/finance/entries")
public class EntryPaymentController {

    private final PaymentService paymentService;

    public EntryPaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // RECORD PAYMENT
    // ═══════════════════════════════════════════════════════════════════════════

    @PostMapping("/{id}/payments")
    @PreAuthorize("hasAuthority('PAYMENT_CREATE')")
    public ResponseEntity<EntryResponseDto> recordPayment(
            @PathVariable UUID id,
            @Valid @RequestBody RecordPaymentRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        Money paymentAmount = Money.of(
                request.amount().toPlainString(),
                "EUR"
        );

        return ResponseEntity.ok(
                paymentService.recordPayment(
                        id,
                        paymentAmount,
                        request.paymentDate(),
                        request.paymentReference(),
                        request.paymentMethod(),
                        request.notes(),
                        currentUser
                )
        );
    }

    @PostMapping("/{id}/payments/full")
    @PreAuthorize("hasAuthority('PAYMENT_CREATE')")
    public ResponseEntity<EntryResponseDto> recordFullPayment(
            @PathVariable UUID id,
            @Valid @RequestBody RecordFullPaymentRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(
                paymentService.recordFullPayment(
                        id,
                        request.paymentDate(),
                        request.paymentReference(),
                        request.paymentMethod(),
                        request.notes(),
                        currentUser
                )
        );
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // QUERY PAYMENTS
    // ═══════════════════════════════════════════════════════════════════════════

    @GetMapping("/{id}/payments")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<PaymentResponseDto>> getPayments(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(paymentService.getPaymentsByEntry(id, currentUser));
    }

    @GetMapping("/{id}/payments/summary")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PaymentService.PaymentSummary> getPaymentSummary(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(paymentService.getPaymentSummary(id, currentUser));
    }

    @GetMapping("/payments/recent")
    @PreAuthorize("hasAuthority('PAYMENT_VIEW')")
    public ResponseEntity<List<PaymentResponseDto>> getRecentPayments(
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(paymentService.getRecentPayments(currentUser));
    }

    @GetMapping("/payments/by-date")
    @PreAuthorize("hasAuthority('PAYMENT_VIEW')")
    public ResponseEntity<List<PaymentResponseDto>> getPaymentsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(paymentService.getPaymentsByDateRange(startDate, endDate, currentUser));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // UPDATE PAYMENT
    // ═══════════════════════════════════════════════════════════════════════════

    @PatchMapping("/payments/{paymentId}")
    @PreAuthorize("hasAuthority('PAYMENT_EDIT')")
    public ResponseEntity<PaymentResponseDto> updatePayment(
            @PathVariable UUID paymentId,
            @Valid @RequestBody UpdatePaymentRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(
                paymentService.updatePayment(
                        paymentId,
                        request.paymentReference(),
                        request.paymentMethod(),
                        request.notes(),
                        currentUser
                )
        );
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // DELETE PAYMENT
    // ═══════════════════════════════════════════════════════════════════════════

    @DeleteMapping("/payments/{paymentId}")
    @PreAuthorize("hasAuthority('PAYMENT_DELETE')")
    public ResponseEntity<EntryResponseDto> deletePayment(
            @PathVariable UUID paymentId,
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(paymentService.deletePayment(paymentId, currentUser));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // DTOs
    // ═══════════════════════════════════════════════════════════════════════════

    public record RecordFullPaymentRequest(
            LocalDate paymentDate,
            String paymentReference,
            PaymentMethod paymentMethod,
            String notes
    ) {}

    public record UpdatePaymentRequest(
            String paymentReference,
            PaymentMethod paymentMethod,
            String notes
    ) {}
}