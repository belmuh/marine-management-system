package com.marine.management.modules.finance.application;

import com.marine.management.modules.finance.application.commands.*;
import com.marine.management.modules.finance.domain.entity.*;
import com.marine.management.modules.finance.domain.enums.RecordType;
import com.marine.management.modules.finance.infrastructure.*;
import com.marine.management.modules.finance.presentation.dto.EntryResponseDto;
import com.marine.management.modules.users.domain.User;
import com.marine.management.shared.exceptions.EntryNotFoundException;
import com.marine.management.shared.multitenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class FinancialEntryService {

    private static final Logger logger = LoggerFactory.getLogger(FinancialEntryService.class);

    private final FinancialEntryRepository entryRepository;
    private final FinancialEntrySearchRepository searchRepository;
    private final FinancialCategoryRepository categoryRepository;
    private final TenantWhoSelectionRepository tenantWhoRepository;
    private final TenantMainCategoryRepository tenantMainCategoryRepository;
    private final FinancialEntryFactory entryFactory;
    private final ExchangeRateService exchangeRateService;

    public FinancialEntryService(
            FinancialEntryRepository entryRepository,
            FinancialEntrySearchRepository searchRepository,
            FinancialCategoryRepository categoryRepository,
            TenantWhoSelectionRepository tenantWhoRepository,
            TenantMainCategoryRepository tenantMainCategoryRepository,
            FinancialEntryFactory entryFactory,
            ExchangeRateService exchangeRateService
    ) {
        this.entryRepository = entryRepository;
        this.searchRepository = searchRepository;
        this.categoryRepository = categoryRepository;
        this.tenantWhoRepository = tenantWhoRepository;
        this.tenantMainCategoryRepository = tenantMainCategoryRepository;
        this.entryFactory = entryFactory;
        this.exchangeRateService = exchangeRateService;
    }

    // ============================================
    // COMMAND METHODS (Transactional) - ⭐ Return DTO
    // ============================================

    @Transactional
    public EntryResponseDto createEntry(CreateEntryCommand command) {
        guardTenantContext();
        logger.debug("Creating entry for tenant: {}", TenantContext.getCurrentTenantId());
        verifyUserBelongsToCurrentTenant(command.creator());

        FinancialEntry entry = entryFactory.createEntry(command);
        entry.calculateBaseAmount(exchangeRateService);
        FinancialEntry saved = entryRepository.saveAndFlush(entry);

        logger.info("Entry created: id={}, number={}, tenant={}",
                saved.getId(),
                saved.getEntryNumber().getValue(),
                TenantContext.getCurrentTenantId());

        return EntryResponseDto.from(saved); // ⭐ DTO conversion in service
    }

    @Transactional
    public EntryResponseDto updateEntry(UpdateEntryCommand command) {
        guardTenantContext();

        FinancialEntry entry = findEntryOrThrow(command.entryId());
        FinancialCategory category = findCategoryOrThrow(command.categoryId());
        verifyUserBelongsToCurrentTenant(command.updater());

        TenantWhoSelection tenantWho = resolveTenantWho(command.whoId());
        TenantMainCategory tenantMainCategory = resolveTenantMainCategory(command.mainCategoryId());

        entry.updateDetails(
                command.entryType(),
                category,
                command.amount(),
                command.entryDate(),
                command.paymentMethod(),
                command.description(),
                command.updater()
        );

        entry.updateContext(
                tenantWho,
                tenantMainCategory,
                command.recipient(),
                command.country(),
                command.city(),
                command.specificLocation(),
                command.vendor(),
                command.updater()
        );

        if (command.receiptNumber() != null && !command.receiptNumber().isBlank()) {
            entry.updateReceiptNumber(command.receiptNumber(), command.updater());
        }

        entry.calculateBaseAmount(exchangeRateService);

        logger.debug("Entry updated: id={}, tenant={}", entry.getId(), TenantContext.getCurrentTenantId());

        return EntryResponseDto.from(entry); // ⭐ DTO conversion in service
    }

    @Transactional
    public EntryResponseDto updateEntryContext(UpdateEntryContextCommand command) {
        guardTenantContext();

        FinancialEntry entry = findEntryOrThrow(command.entryId());
        verifyUserBelongsToCurrentTenant(command.updater());

        TenantWhoSelection tenantWho = resolveTenantWho(command.whoId());
        TenantMainCategory tenantMainCategory = resolveTenantMainCategory(command.mainCategoryId());

        entry.updateContext(
                tenantWho,
                tenantMainCategory,
                command.recipient(),
                command.country(),
                command.city(),
                command.specificLocation(),
                command.vendor(),
                command.updater()
        );

        return EntryResponseDto.from(entry); // ⭐ DTO conversion in service
    }

    @Transactional
    public EntryResponseDto updateEntryMetadata(UpdateEntryMetadataCommand command) {
        guardTenantContext();

        FinancialEntry entry = findEntryOrThrow(command.entryId());
        verifyUserBelongsToCurrentTenant(command.updater());

        entry.updateMetadata(
                command.frequency(),
                command.priority(),
                command.tags(),
                command.updater()
        );

        return EntryResponseDto.from(entry); // ⭐ DTO conversion in service
    }

    @Transactional
    public EntryResponseDto updateReceiptNumber(UpdateReceiptNumberCommand command) {
        guardTenantContext();

        FinancialEntry entry = findEntryOrThrow(command.entryId());
        verifyUserBelongsToCurrentTenant(command.updater());

        entry.updateReceiptNumber(command.receiptNumber(), command.updater());

        return EntryResponseDto.from(entry); // ⭐ DTO conversion in service
    }

    @Transactional
    public EntryResponseDto updateExchangeRate(UpdateExchangeRateCommand command) {
        guardTenantContext();

        FinancialEntry entry = findEntryOrThrow(command.entryId());
        verifyUserBelongsToCurrentTenant(command.updater());

        entry.updateExchangeRate(command.rate(), command.rateDate(), command.updater());

        return EntryResponseDto.from(entry); // ⭐ DTO conversion in service
    }

    @Transactional
    public void deleteEntry(DeleteEntryCommand command) {
        guardTenantContext();

        FinancialEntry entry = findEntryOrThrow(command.entryId());
        verifyUserBelongsToCurrentTenant(command.user());
        verifyEditPermission(entry, command.user());

        entryRepository.delete(entry);

        logger.info("Entry deleted: id={}, tenant={}", command.entryId(), TenantContext.getCurrentTenantId());
    }



    // ============================================
// ATTACHMENT COMMAND METHODS
// ============================================

    @Transactional
    public FinancialEntry addAttachment(AddAttachmentCommand command) {
        guardTenantContext();

        FinancialEntry entry = findEntryOrThrow(command.entryId());
        verifyUserBelongsToCurrentTenant(command.updater());

        entry.addAttachment(command.attachment(), command.updater());
        return entry;  // ⚠️ Returns entity for internal use by AttachmentService
    }

    @Transactional
    public FinancialEntry removeAttachment(RemoveAttachmentCommand command) {
        guardTenantContext();

        FinancialEntry entry = findEntryOrThrow(command.entryId());
        verifyUserBelongsToCurrentTenant(command.updater());

        FinancialEntryAttachment attachment = findAttachmentOrThrow(entry, command.attachmentId());
        entry.removeAttachment(attachment, command.updater());
        return entry;  // ⚠️ Returns entity for internal use by AttachmentService
    }


    // ============================================
    // QUERY METHODS (Read-only) - ⭐ Return DTO
    // ============================================

    Optional<FinancialEntry> findById(UUID id) {
        guardTenantContext();
        return entryRepository.findById(id);
    }

    public EntryResponseDto getById(UUID id) {
        guardTenantContext();
        FinancialEntry entry = entryRepository.findById(id)
                .orElseThrow(() -> EntryNotFoundException.withId(id));

        return EntryResponseDto.from(entry); // ⭐ DTO conversion in service
    }

    public EntryResponseDto getByEntryNumber(String entryNumber) {
        guardTenantContext();
        FinancialEntry entry = entryRepository.findByEntryNumber_Value(entryNumber)
                .orElseThrow(() -> EntryNotFoundException.withEntryNumber(entryNumber));

        return EntryResponseDto.from(entry); // ⭐ DTO conversion in service
    }

    public Page<EntryResponseDto> findByUser(User user, Pageable pageable) {
        guardTenantContext();
        verifyUserBelongsToCurrentTenant(user);

        return entryRepository.findByCreatedById(user.getUserId(), pageable)
                .map(EntryResponseDto::from); // ⭐ DTO conversion in service
    }

    public Page<EntryResponseDto> findByDateRange(LocalDate startDate, LocalDate endDate, Pageable pageable) {
        guardTenantContext();

        return entryRepository.findByEntryDateBetween(startDate, endDate, pageable)
                .map(EntryResponseDto::from); // ⭐ DTO conversion in service
    }

    public Page<EntryResponseDto> findByType(RecordType type, Pageable pageable) {
        guardTenantContext();

        return entryRepository.findByEntryType(type, pageable)
                .map(EntryResponseDto::from); // ⭐ DTO conversion in service
    }

    public Page<EntryResponseDto> findByCategory(UUID categoryId, Pageable pageable) {
        guardTenantContext();
        FinancialCategory category = findCategoryOrThrow(categoryId);

        return entryRepository.findByCategory(category, pageable)
                .map(EntryResponseDto::from); // ⭐ DTO conversion in service
    }

    public Page<EntryResponseDto> findByWho(UUID whoId, Pageable pageable) {
        guardTenantContext();

        return entryRepository.findByTenantWho_Id(whoId, pageable)
                .map(EntryResponseDto::from); // ⭐ DTO conversion in service
    }

    public Page<EntryResponseDto> findByMainCategory(UUID mainCategoryId, Pageable pageable) {
        guardTenantContext();

        return entryRepository.findByTenantMainCategory_Id(mainCategoryId, pageable)
                .map(EntryResponseDto::from); // ⭐ DTO conversion in service
    }

    public Page<EntryResponseDto> search(EntrySearchCriteria criteria, Pageable pageable) {
        guardTenantContext();

        return searchRepository.search(
                criteria.categoryId(),
                criteria.entryType(),
                criteria.whoId(),
                criteria.mainCategoryId(),
                criteria.startDate(),
                criteria.endDate(),
                pageable
        ); // Already returns DTO from repository
    }

    public Page<EntryResponseDto> searchByText(TextSearchCriteria criteria, Pageable pageable) {
        guardTenantContext();

        return searchRepository.searchByText(
                criteria.searchTerm(),
                criteria.entryType(),
                criteria.startDate(),
                criteria.endDate(),
                pageable
        ); // Already returns DTO from repository
    }



    // ============================================
    // HELPER METHODS
    // ============================================

    private void guardTenantContext() {
        if (!TenantContext.hasTenantContext()) {
            logger.error("CRITICAL: Service method called without tenant context!");
            throw new AccessDeniedException(
                    "No tenant context available. This is a programming error - " +
                            "service should only be called within tenant context."
            );
        }
    }

    private void verifyUserBelongsToCurrentTenant(User user) {
        Long currentTenantId = TenantContext.getCurrentTenantId();
        Long userTenantId = user.getOrganization().getOrganizationId();

        if (!currentTenantId.equals(userTenantId)) {
            logger.error("SECURITY VIOLATION: User {} belongs to tenant {} but current context is {}",
                    user.getUsername(), userTenantId, currentTenantId);
            throw new AccessDeniedException(
                    "User does not belong to current tenant. This is a security violation."
            );
        }
    }

    private FinancialEntry findEntryOrThrow(UUID id) {
        return entryRepository.findById(id)
                .orElseThrow(() -> EntryNotFoundException.withId(id));
    }

    private FinancialCategory findCategoryOrThrow(UUID id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException("Category not found with id: " + id));
    }

    private void verifyEditPermission(FinancialEntry entry, User user) {
        entry.canBeEditedBy(user);
    }

    private TenantWhoSelection resolveTenantWho(UUID whoId) {
        if (whoId == null) {
            return null;
        }
        return tenantWhoRepository.findById(whoId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "TenantWhoSelection not found with id: " + whoId
                ));
    }

    private TenantMainCategory resolveTenantMainCategory(UUID mainCategoryId) {
        if (mainCategoryId == null) {
            return null;
        }
        return tenantMainCategoryRepository.findById(mainCategoryId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "TenantMainCategory not found with id: " + mainCategoryId
                ));
    }


    private FinancialEntryAttachment findAttachmentOrThrow(FinancialEntry entry, UUID attachmentId) {
        return entry.getAttachments().stream()
                .filter(a -> a.getId().equals(attachmentId))
                .findFirst()
                .orElseThrow(() -> new AttachmentNotFoundException(
                        "Attachment not found with id: " + attachmentId
                ));
    }

    // ============================================
    // CUSTOM EXCEPTIONS
    // ============================================

    public static class CategoryNotFoundException extends RuntimeException {
        public CategoryNotFoundException(String message) {
            super(message);
        }
    }

    public static class AttachmentNotFoundException extends RuntimeException {
        public AttachmentNotFoundException(String message) {
            super(message);
        }
    }
}