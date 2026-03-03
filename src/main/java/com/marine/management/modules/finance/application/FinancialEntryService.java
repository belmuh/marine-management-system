package com.marine.management.modules.finance.application;

import com.marine.management.modules.finance.application.commands.*;
import com.marine.management.modules.finance.domain.entities.*;
import com.marine.management.modules.finance.domain.enums.EntryStatus;
import com.marine.management.modules.finance.infrastructure.*;
import com.marine.management.modules.finance.infrastructure.query.EntrySearchCriteria;
import com.marine.management.modules.finance.infrastructure.query.SortableFields;
import com.marine.management.modules.finance.infrastructure.specifications.FinancialEntrySpecs;
import com.marine.management.modules.finance.presentation.dto.EntryResponseDto;
import com.marine.management.modules.users.domain.User;
import com.marine.management.shared.exceptions.EntryNotFoundException;
import com.marine.management.shared.multitenant.TenantContext;
import com.marine.management.shared.security.EntryAccessPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

/**
 * Service for FinancialEntry CRUD operations.
 *
 * Responsibilities:
 * - CRUD operations with access control
 * - Search and query operations
 * - Coordinate domain logic
 *
 * Note: Approval workflow is handled by ApprovalService.
 */
@Service
@Transactional(readOnly = true)
public class FinancialEntryService {

    private static final Logger logger = LoggerFactory.getLogger(FinancialEntryService.class);

    private final FinancialEntryRepository entryRepository;
    private final FinancialCategoryRepository categoryRepository;
    private final TenantWhoSelectionRepository tenantWhoRepository;
    private final TenantMainCategoryRepository tenantMainCategoryRepository;
    private final FinancialEntryFactory entryFactory;
    private final ExchangeRateService exchangeRateService;
    private final EntryAccessPolicy accessPolicy;

    public FinancialEntryService(
            FinancialEntryRepository entryRepository,
            FinancialCategoryRepository categoryRepository,
            TenantWhoSelectionRepository tenantWhoRepository,
            TenantMainCategoryRepository tenantMainCategoryRepository,
            FinancialEntryFactory entryFactory,
            ExchangeRateService exchangeRateService,
            EntryAccessPolicy accessPolicy
    ) {
        this.entryRepository = entryRepository;
        this.categoryRepository = categoryRepository;
        this.tenantWhoRepository = tenantWhoRepository;
        this.tenantMainCategoryRepository = tenantMainCategoryRepository;
        this.entryFactory = entryFactory;
        this.exchangeRateService = exchangeRateService;
        this.accessPolicy = accessPolicy;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CREATE
    // ═══════════════════════════════════════════════════════════════════════════

    @Transactional
    public EntryResponseDto createEntry(CreateEntryCommand command) {
        guardTenantContext();
        verifyUserBelongsToCurrentTenant(command.creator());

        FinancialEntry entry = entryFactory.createEntry(command);
        entry.calculateBaseAmount(exchangeRateService);
        FinancialEntry saved = entryRepository.saveAndFlush(entry);

        logger.info("Entry created: id={}, number={}", saved.getId(), saved.getEntryNumber().getValue());

        return EntryResponseDto.from(saved);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // READ - Single Entry
    // ═══════════════════════════════════════════════════════════════════════════

    public EntryResponseDto getById(UUID id, User currentUser) {
        guardTenantContext();

        FinancialEntry entry = findEntryOrThrow(id);
        accessPolicy.checkReadAccess(entry, currentUser);

        return EntryResponseDto.from(entry);
    }

    public EntryResponseDto getByEntryNumber(String entryNumber, User currentUser) {
        guardTenantContext();

        FinancialEntry entry = entryRepository.findByEntryNumber_Value(entryNumber)
                .orElseThrow(() -> EntryNotFoundException.withEntryNumber(entryNumber));

        accessPolicy.checkReadAccess(entry, currentUser);

        return EntryResponseDto.from(entry);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // READ - Lists with Access Control
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Search expenses with role-based filtering.
     */
    public Page<EntryResponseDto> searchExpenses(EntrySearchCriteria criteria, User currentUser) {
        guardTenantContext();
        verifyUserBelongsToCurrentTenant(currentUser);

        Specification<FinancialEntry> spec = FinancialEntrySpecs.fromCriteria(criteria)
                .and(accessPolicy.getExpenseReadSpecification(currentUser));

        Sort sort = SortableFields.createSort(criteria.sortColumn(), criteria.sortDirection());
        Pageable pageable = PageRequest.of(criteria.page(), criteria.size(), sort);

        return entryRepository.findAll(spec, pageable)
                .map(EntryResponseDto::from);
    }

    /**
     * Search incomes (only for users with INCOME_VIEW permission).
     */
    public Page<EntryResponseDto> searchIncomes(EntrySearchCriteria criteria, User currentUser) {
        guardTenantContext();
        verifyUserBelongsToCurrentTenant(currentUser);

        Specification<FinancialEntry> spec = FinancialEntrySpecs.fromCriteria(criteria)
                .and(accessPolicy.getIncomeReadSpecification(currentUser));

        Sort sort = SortableFields.createSort(criteria.sortColumn(), criteria.sortDirection());
        Pageable pageable = PageRequest.of(criteria.page(), criteria.size(), sort);

        return entryRepository.findAll(spec, pageable)
                .map(EntryResponseDto::from);
    }

    /**
     * Legacy search method - delegates to searchExpenses.
     */
    public Page<EntryResponseDto> search(EntrySearchCriteria criteria, User currentUser) {
        return searchExpenses(criteria, currentUser);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // UPDATE
    // ═══════════════════════════════════════════════════════════════════════════

    @Transactional
    public EntryResponseDto updateEntry(UpdateEntryCommand command) {
        guardTenantContext();
        verifyUserBelongsToCurrentTenant(command.updater());

        FinancialEntry entry = findEntryOrThrow(command.entryId());
        accessPolicy.checkWriteAccess(entry, command.updater());

        FinancialCategory category = findCategoryOrThrow(command.categoryId());
        TenantWhoSelection tenantWho = resolveTenantWho(command.whoId());
        TenantMainCategory tenantMainCategory = resolveTenantMainCategory(command.mainCategoryId());

        entry.updateDetails(
                command.entryType(),
                category,
                command.amount(),
                command.entryDate(),
                command.paymentMethod(),
                command.description()
        );

        entry.updateContext(
                tenantWho,
                tenantMainCategory,
                command.recipient(),
                command.country(),
                command.city(),
                command.specificLocation(),
                command.vendor()
        );

        if (command.receiptNumber() != null && !command.receiptNumber().isBlank()) {
            entry.updateReceiptNumber(command.receiptNumber());
        }

        entry.calculateBaseAmount(exchangeRateService);

        logger.debug("Entry updated: id={}", entry.getId());

        return EntryResponseDto.from(entry);
    }

    @Transactional
    public EntryResponseDto updateEntryContext(UpdateEntryContextCommand command) {
        guardTenantContext();
        verifyUserBelongsToCurrentTenant(command.updater());

        FinancialEntry entry = findEntryOrThrow(command.entryId());
        accessPolicy.checkWriteAccess(entry, command.updater());

        TenantWhoSelection tenantWho = resolveTenantWho(command.whoId());
        TenantMainCategory tenantMainCategory = resolveTenantMainCategory(command.mainCategoryId());

        entry.updateContext(
                tenantWho,
                tenantMainCategory,
                command.recipient(),
                command.country(),
                command.city(),
                command.specificLocation(),
                command.vendor()
        );


        return EntryResponseDto.from(entry);
    }

    @Transactional
    public EntryResponseDto updateEntryMetadata(UpdateEntryMetadataCommand command) {
        guardTenantContext();
        verifyUserBelongsToCurrentTenant(command.updater());

        FinancialEntry entry = findEntryOrThrow(command.entryId());
        accessPolicy.checkWriteAccess(entry, command.updater());

        entry.updateMetadata(command.frequency(), command.priority(), command.tags());

        return EntryResponseDto.from(entry);
    }

    @Transactional
    public EntryResponseDto updateReceiptNumber(UpdateReceiptNumberCommand command) {
        guardTenantContext();
        verifyUserBelongsToCurrentTenant(command.updater());

        FinancialEntry entry = findEntryOrThrow(command.entryId());
        accessPolicy.checkWriteAccess(entry, command.updater());

        entry.updateReceiptNumber(command.receiptNumber());

        return EntryResponseDto.from(entry);
    }

    @Transactional
    public EntryResponseDto updateExchangeRate(UpdateExchangeRateCommand command) {
        guardTenantContext();
        verifyUserBelongsToCurrentTenant(command.updater());

        FinancialEntry entry = findEntryOrThrow(command.entryId());
        accessPolicy.checkWriteAccess(entry, command.updater());

        entry.updateExchangeRate(command.rate(), command.rateDate());

        return EntryResponseDto.from(entry);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // DELETE
    // ═══════════════════════════════════════════════════════════════════════════

    @Transactional
    public void deleteEntry(DeleteEntryCommand command) {
        guardTenantContext();
        verifyUserBelongsToCurrentTenant(command.user());

        FinancialEntry entry = findEntryOrThrow(command.entryId());
        accessPolicy.checkDeleteAccess(entry, command.user());

        entryRepository.delete(entry);

        logger.info("Entry deleted: id={}", command.entryId());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ATTACHMENT METHODS
    // ═══════════════════════════════════════════════════════════════════════════

    @Transactional
    public EntryResponseDto addAttachment(AddAttachmentCommand command) {
        guardTenantContext();
        verifyUserBelongsToCurrentTenant(command.updater());

        FinancialEntry entry = findEntryOrThrow(command.entryId());
        accessPolicy.checkWriteAccess(entry, command.updater());

        entry.addAttachment(command.attachment());

        return EntryResponseDto.from(entry);
    }

    @Transactional
    public EntryResponseDto removeAttachment(RemoveAttachmentCommand command) {
        guardTenantContext();
        verifyUserBelongsToCurrentTenant(command.updater());

        FinancialEntry entry = findEntryOrThrow(command.entryId());
        accessPolicy.checkWriteAccess(entry, command.updater());

        FinancialEntryAttachment attachment = findAttachmentOrThrow(entry, command.attachmentId());
        entry.removeAttachment(attachment);

        return EntryResponseDto.from(entry);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // STATUS-BASED QUERIES (no access control - use for reporting)
    // ═══════════════════════════════════════════════════════════════════════════

    public Page<EntryResponseDto> findByStatus(EntryStatus status, Pageable pageable) {
        guardTenantContext();
        return entryRepository.findByStatus(status, pageable)
                .map(EntryResponseDto::from);
    }

    public Page<EntryResponseDto> findByStatuses(Set<EntryStatus> statuses, Pageable pageable) {
        guardTenantContext();
        return entryRepository.findByStatusIn(statuses, pageable)
                .map(EntryResponseDto::from);
    }

    public long countByStatus(EntryStatus status) {
        guardTenantContext();
        return entryRepository.countByStatus(status);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // USER CAPABILITIES (for frontend)
    // ═══════════════════════════════════════════════════════════════════════════

    public UserCapabilities getUserCapabilities(User user) {
        return new UserCapabilities(
                accessPolicy.canViewAllEntries(user),
                accessPolicy.canViewIncomes(user),
                accessPolicy.canCreateIncomes(user),
                accessPolicy.canViewReports(user),
                accessPolicy.canExportReports(user),
                accessPolicy.canApproveCaptainLevel(user),
                accessPolicy.canApproveManagerLevel(user),
                accessPolicy.canManagePayments(user),
                accessPolicy.canManageUsers(user)
        );
    }

    public record UserCapabilities(
            boolean canViewAllEntries,
            boolean canViewIncomes,
            boolean canCreateIncomes,
            boolean canViewReports,
            boolean canExportReports,
            boolean canApproveCaptainLevel,
            boolean canApproveManagerLevel,
            boolean canManagePayments,
            boolean canManageUsers
    ) {}

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
        Long userTenantId = user.getOrganization().getOrganizationId();

        if (!currentTenantId.equals(userTenantId)) {
            throw new AccessDeniedException("User does not belong to current tenant");
        }
    }

    private FinancialEntry findEntryOrThrow(UUID id) {
        return entryRepository.findById(id)
                .orElseThrow(() -> EntryNotFoundException.withId(id));
    }

    private FinancialCategory findCategoryOrThrow(UUID id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException("Category not found: " + id));
    }

    private TenantWhoSelection resolveTenantWho(UUID whoId) {
        if (whoId == null) return null;
        return tenantWhoRepository.findById(whoId)
                .orElseThrow(() -> new IllegalArgumentException("TenantWhoSelection not found: " + whoId));
    }

    private TenantMainCategory resolveTenantMainCategory(UUID mainCategoryId) {
        if (mainCategoryId == null) return null;
        return tenantMainCategoryRepository.findById(mainCategoryId)
                .orElseThrow(() -> new IllegalArgumentException("TenantMainCategory not found: " + mainCategoryId));
    }

    private FinancialEntryAttachment findAttachmentOrThrow(FinancialEntry entry, UUID attachmentId) {
        return entry.getAttachments().stream()
                .filter(a -> a.getId().equals(attachmentId))
                .findFirst()
                .orElseThrow(() -> new AttachmentNotFoundException("Attachment not found: " + attachmentId));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // EXCEPTIONS
    // ═══════════════════════════════════════════════════════════════════════════

    public static class CategoryNotFoundException extends RuntimeException {
        public CategoryNotFoundException(String message) { super(message); }
    }

    public static class AttachmentNotFoundException extends RuntimeException {
        public AttachmentNotFoundException(String message) { super(message); }
    }
}