package com.marine.management.modules.finance.application;

import com.marine.management.modules.finance.application.commands.*;
import com.marine.management.modules.finance.domain.entity.*;
import com.marine.management.modules.finance.domain.enums.PaymentMethod;
import com.marine.management.modules.finance.domain.enums.RecordType;
import com.marine.management.modules.finance.domain.vo.Money;
import com.marine.management.modules.finance.infrastructure.*;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class FinancialEntryService {

    private static final Logger logger = LoggerFactory.getLogger(FinancialEntryService.class);

    private final FinancialEntryRepository entryRepository;
    private final FinancialCategoryRepository categoryRepository;
    private final TenantWhoSelectionRepository tenantWhoRepository;
    private final TenantMainCategoryRepository tenantMainCategoryRepository;
    private final FinancialEntryFactory entryFactory;  // ✅ Injected
    private final ExchangeRateService exchangeRateService;

    public FinancialEntryService(
            FinancialEntryRepository entryRepository,
            FinancialCategoryRepository categoryRepository,
            TenantWhoSelectionRepository tenantWhoRepository,
            TenantMainCategoryRepository tenantMainCategoryRepository,
            FinancialEntryFactory entryFactory,  // ✅ Injected
            ExchangeRateService exchangeRateService
    ) {
        this.entryRepository = entryRepository;
        this.categoryRepository = categoryRepository;
        this.tenantWhoRepository = tenantWhoRepository;
        this.tenantMainCategoryRepository = tenantMainCategoryRepository;
        this.entryFactory = entryFactory;
        this.exchangeRateService = exchangeRateService;
    }

    // ============================================
    // COMMAND METHODS (Transactional)
    // ============================================

    @Transactional
    public FinancialEntry createEntry(CreateEntryCommand command) {
        guardTenantContext();

        logger.debug("Creating entry for tenant: {}", TenantContext.getCurrentTenantId());

        verifyUserBelongsToCurrentTenant(command.creator());

        // ✅ Factory creates entry
        FinancialEntry entry = entryFactory.createEntry(command);

        // Calculate base amount
        entry.calculateBaseAmount(exchangeRateService);

        // Save
        FinancialEntry saved = entryRepository.saveAndFlush(entry);

        logger.info("Entry created: id={}, number={}, tenant={}",
                saved.getId(),
                saved.getEntryNumber().getValue(),
                TenantContext.getCurrentTenantId());

        return saved;
    }

    @Transactional
    public FinancialEntry updateEntry(UpdateEntryCommand command) {
        guardTenantContext();

        FinancialEntry entry = findEntryOrThrow(command.entryId());
        FinancialCategory category = findCategoryOrThrow(command.categoryId());
        verifyUserBelongsToCurrentTenant(command.updater());

        // Resolve entities
        TenantWhoSelection tenantWho = resolveTenantWho(command.whoId());
        TenantMainCategory tenantMainCategory = resolveTenantMainCategory(command.mainCategoryId());

        // Update
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

        logger.debug("Entry updated: id={}, tenant={}",
                entry.getId(),
                TenantContext.getCurrentTenantId());

        return entry;
    }

    @Transactional
    public FinancialEntry updateEntryContext(UpdateEntryContextCommand command) {
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

        return entry;
    }

    @Transactional
    public FinancialEntry updateEntryMetadata(UpdateEntryMetadataCommand command) {
        guardTenantContext();

        FinancialEntry entry = findEntryOrThrow(command.entryId());
        verifyUserBelongsToCurrentTenant(command.updater());

        entry.updateMetadata(
                command.frequency(),
                command.priority(),
                command.tags(),
                command.updater()
        );

        return entry;
    }

    @Transactional
    public FinancialEntry updateReceiptNumber(UpdateReceiptNumberCommand command) {
        guardTenantContext();

        FinancialEntry entry = findEntryOrThrow(command.entryId());
        verifyUserBelongsToCurrentTenant(command.updater());

        entry.updateReceiptNumber(command.receiptNumber(), command.updater());
        return entry;
    }

    @Transactional
    public FinancialEntry updateExchangeRate(UpdateExchangeRateCommand command) {
        guardTenantContext();

        FinancialEntry entry = findEntryOrThrow(command.entryId());
        verifyUserBelongsToCurrentTenant(command.updater());

        entry.updateExchangeRate(command.rate(), command.rateDate(), command.updater());
        return entry;
    }

    @Transactional
    public void deleteEntry(DeleteEntryCommand command) {
        guardTenantContext();

        FinancialEntry entry = findEntryOrThrow(command.entryId());
        verifyUserBelongsToCurrentTenant(command.user());
        verifyEditPermission(entry, command.user());

        entryRepository.delete(entry);

        logger.info("Entry deleted: id={}, tenant={}",
                command.entryId(),
                TenantContext.getCurrentTenantId());
    }

    // ============================================
    // ATTACHMENT COMMANDS
    // ============================================

    @Transactional
    public FinancialEntry addAttachment(AddAttachmentCommand command) {
        guardTenantContext();

        FinancialEntry entry = findEntryOrThrow(command.entryId());
        verifyUserBelongsToCurrentTenant(command.updater());

        entry.addAttachment(command.attachment(), command.updater());
        return entry;
    }

    @Transactional
    public FinancialEntry removeAttachment(RemoveAttachmentCommand command) {
        guardTenantContext();

        FinancialEntry entry = findEntryOrThrow(command.entryId());
        verifyUserBelongsToCurrentTenant(command.updater());

        FinancialEntryAttachment attachment = findAttachmentOrThrow(entry, command.attachmentId());
        entry.removeAttachment(attachment, command.updater());
        return entry;
    }

    // ============================================
    // QUERY METHODS (Read-only)
    // ============================================

    public Optional<FinancialEntry> findById(UUID id) {
        guardTenantContext();
        return entryRepository.findById(id);
    }

    public Optional<FinancialEntry> findByEntryNumber(String entryNumber) {
        guardTenantContext();
        return entryRepository.findByEntryNumber_Value(entryNumber);
    }

    public Page<FinancialEntry> findByUser(User user, Pageable pageable) {
        guardTenantContext();
        verifyUserBelongsToCurrentTenant(user);
        return entryRepository.findByCreatedBy(user, pageable);
    }

    public Page<FinancialEntry> findByDateRange(LocalDate startDate, LocalDate endDate, Pageable pageable) {
        guardTenantContext();
        return entryRepository.findByEntryDateBetween(startDate, endDate, pageable);
    }

    public Page<FinancialEntry> findByType(RecordType type, Pageable pageable) {
        guardTenantContext();
        return entryRepository.findByEntryType(type, pageable);
    }

    public Page<FinancialEntry> findByCategory(UUID categoryId, Pageable pageable) {
        guardTenantContext();
        FinancialCategory category = findCategoryOrThrow(categoryId);
        return entryRepository.findByCategory(category, pageable);
    }

    public Page<FinancialEntry> findByWho(UUID whoId, Pageable pageable) {
        guardTenantContext();
        return entryRepository.findByTenantWho_Id(whoId, pageable);
    }

    public Page<FinancialEntry> findByMainCategory(UUID mainCategoryId, Pageable pageable) {
        guardTenantContext();
        return entryRepository.findByTenantMainCategory_Id(mainCategoryId, pageable);
    }

    public Page<FinancialEntry> search(EntrySearchCriteria criteria, Pageable pageable) {
        guardTenantContext();
        return entryRepository.search(
                criteria.categoryId(),
                criteria.entryType(),
                criteria.whoId(),
                criteria.mainCategoryId(),
                criteria.startDate(),
                criteria.endDate(),
                pageable
        );
    }

    public Page<FinancialEntry> searchByText(TextSearchCriteria criteria, Pageable pageable) {
        guardTenantContext();
        return entryRepository.searchByText(
                criteria.searchTerm(),
                criteria.entryType(),
                criteria.startDate(),
                criteria.endDate(),
                pageable
        );
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
        Long userTenantId = user.getOrganization().getId();

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

    private FinancialEntryAttachment findAttachmentOrThrow(FinancialEntry entry, UUID attachmentId) {
        return entry.getAttachments().stream()
                .filter(a -> a.getId().equals(attachmentId))
                .findFirst()
                .orElseThrow(() -> new AttachmentNotFoundException(
                        "Attachment not found with id: " + attachmentId
                ));
    }

    private void verifyEditPermission(FinancialEntry entry, User user) {
        entry.canBeEditedBy(user);
    }

    private TenantWhoSelection resolveTenantWho(Long whoId) {
        if (whoId == null) {
            return null;
        }
        return tenantWhoRepository.findById(whoId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "TenantWhoSelection not found with id: " + whoId
                ));
    }

    private TenantMainCategory resolveTenantMainCategory(Long mainCategoryId) {
        if (mainCategoryId == null) {
            return null;
        }
        return tenantMainCategoryRepository.findById(mainCategoryId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "TenantMainCategory not found with id: " + mainCategoryId
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