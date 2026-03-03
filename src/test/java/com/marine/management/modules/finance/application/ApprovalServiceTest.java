package com.marine.management.modules.finance.application;

import com.marine.management.modules.finance.TestDataBuilder;
import com.marine.management.modules.finance.domain.entities.FinancialEntry;
import com.marine.management.modules.finance.domain.enums.EntryStatus;
import com.marine.management.modules.finance.domain.vo.Money;
import com.marine.management.modules.finance.infrastructure.FinancialEntryRepository;
import com.marine.management.modules.organization.domain.Organization;
import com.marine.management.modules.users.domain.User;
import com.marine.management.shared.exceptions.EntryNotFoundException;
import com.marine.management.shared.multitenant.TenantContext;
import com.marine.management.shared.security.EntryAccessPolicy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ApprovalService
 * Tests approval workflow business logic
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ApprovalServiceTest {

    @Mock
    private FinancialEntryRepository entryRepository;

    @Mock
    private EntryAccessPolicy accessPolicy;

    @InjectMocks
    private ApprovalService approvalService;

    private User crew;
    private User captain;
    private User manager;
    private User admin;
    private Organization organization;
    private FinancialEntry draftEntry;
    private FinancialEntry pendingCaptainEntry;
    private FinancialEntry pendingManagerEntry;
    private FinancialEntry approvedEntry;

    @BeforeEach
    void setUp() {
        // Set tenant context
        TenantContext.setCurrentTenantId(1L);

        // Create organization with approval settings
        organization = TestDataBuilder.createOrganization(1L);
        organization.enableManagerApproval(BigDecimal.valueOf(500)); // Manager approval for amounts > 500 EUR

        // Create test users
        crew = TestDataBuilder.createCrew(1L);
        captain = TestDataBuilder.createCaptain(1L);
        manager = TestDataBuilder.createManager(1L);
        admin = TestDataBuilder.createAdmin(1L);

        // Create test entries
        draftEntry = TestDataBuilder.createDraftEntry(crew);
        pendingCaptainEntry = TestDataBuilder.createPendingCaptainEntry(crew);
        pendingManagerEntry = TestDataBuilder.createPendingManagerEntry(crew);
        approvedEntry = TestDataBuilder.createApprovedEntry(crew);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    // ============================================
    // SUBMIT FOR APPROVAL TESTS
    // ============================================

    @Test
    void shouldSubmitEntryForApproval_WhenEntryIsDraft() {
        // Given
        when(entryRepository.findById(draftEntry.getEntryId()))
                .thenReturn(Optional.of(draftEntry));
        doNothing().when(accessPolicy).checkSubmitAccess(any(), any());

        // When
        var result = approvalService.submit(draftEntry.getEntryId(), crew);

        // Then
        assertThat(result).isNotNull();
        assertThat(draftEntry.getStatus()).isEqualTo(EntryStatus.PENDING_CAPTAIN);
    }

    @Test
    void shouldThrowException_WhenSubmitNonDraftEntry() {
        // Given
        when(entryRepository.findById(pendingCaptainEntry.getEntryId()))
                .thenReturn(Optional.of(pendingCaptainEntry));
        doNothing().when(accessPolicy).checkSubmitAccess(any(), any());

        // When/Then
        assertThatThrownBy(() ->
                approvalService.submit(pendingCaptainEntry.getEntryId(), crew)
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DRAFT");
    }

    @Test
    void shouldThrowException_WhenNonCreatorSubmitsEntry() {
        // Given
        when(entryRepository.findById(draftEntry.getEntryId()))
                .thenReturn(Optional.of(draftEntry));
        doThrow(new AccessDeniedException("Only entry creator can submit"))
                .when(accessPolicy).checkSubmitAccess(draftEntry, captain);

        // When/Then
        assertThatThrownBy(() ->
                approvalService.submit(draftEntry.getEntryId(), captain)
        )
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Only entry creator");
    }

    @Test
    void shouldThrowException_WhenEntryNotFound() {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        when(entryRepository.findById(nonExistentId))
                .thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() ->
                approvalService.submit(nonExistentId, crew)
        )
                .isInstanceOf(EntryNotFoundException.class);
    }

    // ============================================
    // CAPTAIN APPROVAL TESTS
    // ============================================

    @Test
    void shouldApproveByCaptain_WhenAmountBelowThreshold() {
        // Given - Amount <= 500 EUR (no manager needed)
        Money smallAmount = Money.of("300.00", "EUR");
        FinancialEntry entry = TestDataBuilder.createPendingCaptainEntryWithAmount(crew, smallAmount);

        when(entryRepository.findById(entry.getEntryId()))
                .thenReturn(Optional.of(entry));
        doNothing().when(accessPolicy).checkApproveAccess(any(), any());

        // When
        var result = approvalService.approveByCaptain(entry.getEntryId(), captain);

        // Then
        assertThat(result).isNotNull();
        assertThat(entry.getStatus()).isEqualTo(EntryStatus.APPROVED);
        assertThat(entry.getApprovedBaseAmount()).isEqualTo(smallAmount);
        assertThat(entry.hasApprovedAmount()).isTrue();
    }

    @Test
    void shouldSendToManager_WhenAmountAboveThreshold() {
        Money largeAmount = Money.of("600.00", "EUR");
        User captainWithOrg = TestDataBuilder.createCaptainWithOrganization(organization); // ← DEĞİŞTİ
        FinancialEntry entry = TestDataBuilder.createPendingCaptainEntryWithAmount(crew, largeAmount);

        when(entryRepository.findById(entry.getEntryId())).thenReturn(Optional.of(entry));
        doNothing().when(accessPolicy).checkApproveAccess(any(), any());

        approvalService.approveByCaptain(entry.getEntryId(), captainWithOrg);

        assertThat(entry.getStatus()).isEqualTo(EntryStatus.PENDING_MANAGER);
    }

    @Test
    void shouldThrowException_WhenNonCaptainApprovesAtCaptainLevel() {
        // Given
        when(entryRepository.findById(pendingCaptainEntry.getEntryId()))
                .thenReturn(Optional.of(pendingCaptainEntry));
        doThrow(new AccessDeniedException("captain approval permission required"))
                .when(accessPolicy).checkApproveAccess(pendingCaptainEntry, crew);

        // When/Then
        assertThatThrownBy(() ->
                approvalService.approveByCaptain(pendingCaptainEntry.getEntryId(), crew)
        )
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("captain approval permission");
    }

    @Test
    void shouldThrowException_WhenEntryNotPendingCaptain() {
        // Given
        when(entryRepository.findById(draftEntry.getEntryId()))
                .thenReturn(Optional.of(draftEntry));

        // When/Then
        assertThatThrownBy(() ->
                approvalService.approveByCaptain(draftEntry.getEntryId(), captain)
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not pending captain approval");
    }

    // ============================================
    // MANAGER APPROVAL TESTS
    // ============================================

    @Test
    void shouldApproveByManager() {
        // Given
        when(entryRepository.findById(pendingManagerEntry.getEntryId()))
                .thenReturn(Optional.of(pendingManagerEntry));
        doNothing().when(accessPolicy).checkApproveAccess(any(), any());

        Money originalAmount = pendingManagerEntry.getBaseAmount();

        // When
        var result = approvalService.approveByManager(pendingManagerEntry.getEntryId(), manager);

        // Then
        assertThat(result).isNotNull();
        assertThat(pendingManagerEntry.getStatus()).isEqualTo(EntryStatus.APPROVED);
        assertThat(pendingManagerEntry.getApprovedBaseAmount()).isEqualTo(originalAmount);
    }

    @Test
    void shouldAllowCaptainToApproveAtManagerLevel() {
        // Given - Captain is "super approver"
        when(entryRepository.findById(pendingManagerEntry.getEntryId()))
                .thenReturn(Optional.of(pendingManagerEntry));
        doNothing().when(accessPolicy).checkApproveAccess(any(), any());

        // When
        var result = approvalService.approveByManager(pendingManagerEntry.getEntryId(), captain);

        // Then
        assertThat(result).isNotNull();
        assertThat(pendingManagerEntry.getStatus()).isEqualTo(EntryStatus.APPROVED);
    }

    @Test
    void shouldThrowException_WhenNonManagerApprovesAtManagerLevel() {
        // Given
        when(entryRepository.findById(pendingManagerEntry.getEntryId()))
                .thenReturn(Optional.of(pendingManagerEntry));
        doThrow(new AccessDeniedException("manager approval permission required"))
                .when(accessPolicy).checkApproveAccess(pendingManagerEntry, crew);

        // When/Then
        assertThatThrownBy(() ->
                approvalService.approveByManager(pendingManagerEntry.getEntryId(), crew)
        )
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("manager approval permission");
    }

    @Test
    void shouldThrowException_WhenEntryNotPendingManager() {
        // Given
        when(entryRepository.findById(pendingCaptainEntry.getEntryId()))
                .thenReturn(Optional.of(pendingCaptainEntry));

        // When/Then
        assertThatThrownBy(() ->
                approvalService.approveByManager(pendingCaptainEntry.getEntryId(), manager)
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not pending manager approval");
    }

    // ============================================
    // APPROVE (AUTO-DETECT LEVEL) TESTS
    // ============================================

    @Test
    void shouldAutoDetectCaptainLevel() {
        // Given
        when(entryRepository.findById(pendingCaptainEntry.getEntryId()))
                .thenReturn(Optional.of(pendingCaptainEntry));
        doNothing().when(accessPolicy).checkApproveAccess(any(), any());

        // When
        var result = approvalService.approve(pendingCaptainEntry.getEntryId(), captain);

        // Then
        assertThat(result).isNotNull();
        // Will either be APPROVED or PENDING_MANAGER depending on amount
    }

    @Test
    void shouldAutoDetectManagerLevel() {
        // Given
        when(entryRepository.findById(pendingManagerEntry.getEntryId()))
                .thenReturn(Optional.of(pendingManagerEntry));
        doNothing().when(accessPolicy).checkApproveAccess(any(), any());

        // When
        var result = approvalService.approve(pendingManagerEntry.getEntryId(), manager);

        // Then
        assertThat(result).isNotNull();
        assertThat(pendingManagerEntry.getStatus()).isEqualTo(EntryStatus.APPROVED);
    }

    @Test
    void shouldThrowException_WhenApproveNonPendingEntry() {
        // Given
        when(entryRepository.findById(approvedEntry.getEntryId()))
                .thenReturn(Optional.of(approvedEntry));

        // When/Then
        assertThatThrownBy(() ->
                approvalService.approve(approvedEntry.getEntryId(), captain)
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not pending approval");
    }

    // ============================================
    // REJECTION TESTS
    // ============================================

    @Test
    void shouldRejectEntry_ByCaptain() {
        // Given
        when(entryRepository.findById(pendingCaptainEntry.getEntryId()))
                .thenReturn(Optional.of(pendingCaptainEntry));
        doNothing().when(accessPolicy).checkRejectAccess(any(), any());

        String reason = "Insufficient justification";

        // When
        var result = approvalService.reject(pendingCaptainEntry.getEntryId(), reason, captain);

        // Then
        assertThat(result).isNotNull();
        assertThat(pendingCaptainEntry.getStatus()).isEqualTo(EntryStatus.REJECTED);
        assertThat(pendingCaptainEntry.getRejectionReason()).isEqualTo(reason);
    }

    @Test
    void shouldRejectEntry_ByManager() {
        // Given
        when(entryRepository.findById(pendingManagerEntry.getEntryId()))
                .thenReturn(Optional.of(pendingManagerEntry));
        doNothing().when(accessPolicy).checkRejectAccess(any(), any());

        String reason = "Budget exceeded";

        // When
        var result = approvalService.reject(pendingManagerEntry.getEntryId(), reason, manager);

        // Then
        assertThat(pendingManagerEntry.getStatus()).isEqualTo(EntryStatus.REJECTED);
        assertThat(pendingManagerEntry.getRejectionReason()).isEqualTo(reason);
    }

    @Test
    void shouldThrowException_WhenRejectingNonPendingEntry() {
        // Given
        when(entryRepository.findById(draftEntry.getEntryId()))
                .thenReturn(Optional.of(draftEntry));
        doNothing().when(accessPolicy).checkRejectAccess(any(), any());

        // When/Then
        assertThatThrownBy(() ->
                approvalService.reject(draftEntry.getEntryId(), "Some reason", captain)
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("pending");
    }

    @Test
    void shouldThrowException_WhenRejectionReasonIsBlank() {
        // Given
        when(entryRepository.findById(pendingCaptainEntry.getEntryId()))
                .thenReturn(Optional.of(pendingCaptainEntry));

        // When/Then
        assertThatThrownBy(() ->
                approvalService.reject(pendingCaptainEntry.getEntryId(), "", captain)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Rejection reason is required");
    }

    @Test
    void shouldThrowException_WhenRejectionReasonIsNull() {
        // Given
        when(entryRepository.findById(pendingCaptainEntry.getEntryId()))
                .thenReturn(Optional.of(pendingCaptainEntry));

        // When/Then
        assertThatThrownBy(() ->
                approvalService.reject(pendingCaptainEntry.getEntryId(), null, captain)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Rejection reason is required");
    }

    // ============================================
    // BULK OPERATIONS TESTS
    // ============================================

    @Test
    void shouldBulkApprove_MultipleEntries() {
        // Given
        FinancialEntry entry1 = TestDataBuilder.createPendingCaptainEntry(crew);
        FinancialEntry entry2 = TestDataBuilder.createPendingCaptainEntry(crew);

        when(entryRepository.findById(entry1.getEntryId()))
                .thenReturn(Optional.of(entry1));
        when(entryRepository.findById(entry2.getEntryId()))
                .thenReturn(Optional.of(entry2));
        doNothing().when(accessPolicy).checkApproveAccess(any(), any());

        // When
        var result = approvalService.bulkApprove(
                java.util.List.of(entry1.getEntryId(), entry2.getEntryId()),
                captain
        );

        // Then
        assertThat(result.success()).isEqualTo(2);
        assertThat(result.failed()).isZero();
    }

    @Test
    void shouldBulkApprove_WithSomeFailures() {
        // Given
        FinancialEntry entry1 = TestDataBuilder.createPendingCaptainEntry(crew);
        UUID nonExistentId = UUID.randomUUID();

        when(entryRepository.findById(entry1.getEntryId()))
                .thenReturn(Optional.of(entry1));
        when(entryRepository.findById(nonExistentId))
                .thenReturn(Optional.empty());
        doNothing().when(accessPolicy).checkApproveAccess(any(), any());

        // When
        var result = approvalService.bulkApprove(
                java.util.List.of(entry1.getEntryId(), nonExistentId),
                captain
        );

        // Then
        assertThat(result.success()).isEqualTo(1);
        assertThat(result.failed()).isEqualTo(1);
    }

    // ============================================
    // TENANT ISOLATION TESTS
    // ============================================

    @Test
    void shouldThrowException_WhenUserFromDifferentTenant() {
        // Given
        User differentTenantUser = TestDataBuilder.createCaptain(999L);
        when(entryRepository.findById(pendingCaptainEntry.getEntryId()))
                .thenReturn(Optional.of(pendingCaptainEntry));

        // When/Then
        assertThatThrownBy(() ->
                approvalService.approveByCaptain(
                        pendingCaptainEntry.getEntryId(),
                        differentTenantUser
                )
        )
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("tenant");
    }

    @Test
    void shouldThrowException_WhenNoTenantContext() {
        // Given
        TenantContext.clear();

        // When/Then
        assertThatThrownBy(() ->
                approvalService.submit(UUID.randomUUID(), crew)
        )
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("No tenant context");
    }

    // ============================================
    // MANAGER APPROVAL THRESHOLD TESTS
    // ============================================

    @Test
    void shouldNotRequireManagerApproval_WhenFeatureDisabled() {
        organization.setManagerApprovalEnabled(false);
        Money largeAmount = Money.of("1000.00", "EUR");
        User captainWithOrg = TestDataBuilder.createCaptainWithOrganization(organization); // ← DEĞİŞTİ
        FinancialEntry entry = TestDataBuilder.createPendingCaptainEntryWithAmount(crew, largeAmount);

        when(entryRepository.findById(entry.getEntryId())).thenReturn(Optional.of(entry));
        doNothing().when(accessPolicy).checkApproveAccess(any(), any());

        approvalService.approveByCaptain(entry.getEntryId(), captainWithOrg);

        assertThat(entry.getStatus()).isEqualTo(EntryStatus.APPROVED);
    }

    @Test
    void shouldNotRequireManagerApproval_WhenNoLimitSet() {
        organization.setManagerApprovalEnabled(true);
        organization.setApprovalLimit(null);
        Money largeAmount = Money.of("10000.00", "EUR");
        User captainWithOrg = TestDataBuilder.createCaptainWithOrganization(organization); // ← DEĞİŞTİ
        FinancialEntry entry = TestDataBuilder.createPendingCaptainEntryWithAmount(crew, largeAmount);

        when(entryRepository.findById(entry.getEntryId())).thenReturn(Optional.of(entry));
        doNothing().when(accessPolicy).checkApproveAccess(any(), any());

        approvalService.approveByCaptain(entry.getEntryId(), captainWithOrg);

        assertThat(entry.getStatus()).isEqualTo(EntryStatus.APPROVED);
    }
}