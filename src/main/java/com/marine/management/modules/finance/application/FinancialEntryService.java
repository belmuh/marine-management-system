package com.marine.management.modules.finance.application;

import com.marine.management.modules.finance.domain.entity.FinancialCategory;
import com.marine.management.modules.finance.domain.entity.FinancialEntry;
import com.marine.management.modules.finance.domain.entity.FinancialEntryAttachment;
import com.marine.management.modules.finance.domain.enums.PaymentMethod;
import com.marine.management.modules.finance.domain.enums.RecordType;
import com.marine.management.modules.finance.domain.vo.EntryNumber;
import com.marine.management.modules.finance.domain.vo.Money;
import com.marine.management.modules.finance.infrastructure.FinancialCategoryRepository;
import com.marine.management.modules.finance.infrastructure.FinancialEntryRepository;
import com.marine.management.modules.users.domain.User;
import com.marine.management.shared.exceptions.EntryNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    private final EntryFactory entryFactory;
    private final EntryUpdater entryUpdater;
    private final ExchangeRateService exchangeRateService;

    public FinancialEntryService(
            FinancialEntryRepository entryRepository,
            FinancialCategoryRepository categoryRepository,
            ExchangeRateService exchangeRateService
    ) {
        this.entryRepository = entryRepository;
        this.categoryRepository = categoryRepository;
        this.entryFactory = new EntryFactory(entryRepository, categoryRepository);
        this.entryUpdater = new EntryUpdater();
        this.exchangeRateService = exchangeRateService;
    }

    // ============================================
    // COMMAND METHODS (Transactional)
    // ============================================

    @Transactional
    public FinancialEntry createEntry(CreateEntryCommand command) {
        // 1. Create entry (factory method)
        FinancialEntry entry = entryFactory.createEntry(command);

        // 2. Calculate base amount (DOMAIN METHOD!) ✅
        entry.calculateBaseAmount(exchangeRateService);

        // 3. Save
        return entryRepository.saveAndFlush(entry);
    }

    @Transactional
    public FinancialEntry updateEntry(UpdateEntryCommand command) {
        FinancialEntry entry = findEntryOrThrow(command.entryId());
        FinancialCategory category = findCategoryOrThrow(command.categoryId());

        // Update entry details
        entryUpdater.updateDetails(
                entry,
                command.entryType(),
                category,
                command.amount(),
                command.entryDate(),
                command.paymentMethod(),
                command.description(),
                command.updater()
        );

        // Recalculate base amount (DOMAIN METHOD!) ✅
        entry.calculateBaseAmount(exchangeRateService);

        return entry;
    }

    @Transactional
    public FinancialEntry updateEntryContext(UpdateEntryContextCommand command) {
        FinancialEntry entry = findEntryOrThrow(command.entryId());

        entry.updateContext(
                command.whoId(),
                command.mainCategoryId(),
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
        FinancialEntry entry = findEntryOrThrow(command.entryId());

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
        FinancialEntry entry = findEntryOrThrow(command.entryId());
        entry.updateReceiptNumber(command.receiptNumber(), command.updater());
        return entry;
    }

    @Transactional
    public FinancialEntry updateExchangeRate(UpdateExchangeRateCommand command) {
        FinancialEntry entry = findEntryOrThrow(command.entryId());
        entry.updateExchangeRate(command.rate(), command.rateDate(), command.updater());
        return entry;
    }

    @Transactional
    public void deleteEntry(DeleteEntryCommand command) {
        FinancialEntry entry = findEntryOrThrow(command.entryId());
        verifyEditPermission(entry, command.user());
        entryRepository.delete(entry);
    }

    // ============================================
    // ATTACHMENT COMMANDS
    // ============================================

    @Transactional
    public FinancialEntry addAttachment(AddAttachmentCommand command) {
        FinancialEntry entry = findEntryOrThrow(command.entryId());
        entry.addAttachment(command.attachment(), command.updater());
        return entry;
    }

    @Transactional
    public FinancialEntry removeAttachment(RemoveAttachmentCommand command) {
        FinancialEntry entry = findEntryOrThrow(command.entryId());
        FinancialEntryAttachment attachment = findAttachmentOrThrow(entry, command.attachmentId());
        entry.removeAttachment(attachment, command.updater());
        return entry;
    }

    // ============================================
    // QUERY METHODS (Read-only)
    // ============================================

    public Optional<FinancialEntry> findById(UUID id) {
        return entryRepository.findById(id);
    }

    public Optional<FinancialEntry> findByEntryNumber(String entryNumber) {
        return entryRepository.findByEntryNumber_Value(entryNumber);
    }

    public Page<FinancialEntry> findByUser(User user, Pageable pageable) {
        return entryRepository.findByCreatedBy(user, pageable);
    }

    public Page<FinancialEntry> findByDateRange(LocalDate startDate, LocalDate endDate, Pageable pageable) {
        return entryRepository.findByEntryDateBetween(startDate, endDate, pageable);
    }

    public Page<FinancialEntry> findByType(RecordType type, Pageable pageable) {
        return entryRepository.findByEntryType(type, pageable);
    }

    public Page<FinancialEntry> findByCategory(UUID categoryId, Pageable pageable) {
        FinancialCategory category = findCategoryOrThrow(categoryId);
        return entryRepository.findByCategory(category, pageable);
    }

    public Page<FinancialEntry> findByWho(Long whoId, Pageable pageable) {
        return entryRepository.findByWhoId(whoId, pageable);
    }

    public Page<FinancialEntry> findByMainCategory(Long mainCategoryId, Pageable pageable) {
        return entryRepository.findByMainCategoryId(mainCategoryId, pageable);
    }

    public Page<FinancialEntry> search(EntrySearchCriteria criteria, Pageable pageable) {
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

    // ============================================
    // INNER CLASSES
    // ============================================

    /**
     * Factory for creating FinancialEntry instances
     */
    private static class EntryFactory {
        private final FinancialEntryRepository entryRepository;
        private final FinancialCategoryRepository categoryRepository;

        EntryFactory(
                FinancialEntryRepository entryRepository,
                FinancialCategoryRepository categoryRepository
        ) {
            this.entryRepository = entryRepository;
            this.categoryRepository = categoryRepository;
        }

        FinancialEntry createEntry(CreateEntryCommand command) {
            FinancialCategory category = categoryRepository.findById(command.categoryId())
                    .orElseThrow(() -> new CategoryNotFoundException(
                            "Category not found with id: " + command.categoryId()
                    ));

            EntryNumber entryNumber = generateEntryNumber();

            return FinancialEntry.create(
                    entryNumber,
                    command.entryType(),
                    category,
                    command.amount(),
                    command.entryDate(),
                    command.paymentMethod(),
                    command.description(),
                    command.creator(),
                    command.whoId(),
                    command.mainCategoryId(),
                    command.recipient(),
                    command.country(),
                    command.city(),
                    command.specificLocation(),
                    command.vendor()
            );
        }

        private EntryNumber generateEntryNumber() {
            int sequence = entryRepository.getNextSequence();
            return EntryNumber.generate(sequence);
        }
    }

    /**
     * Updater for modifying FinancialEntry instances
     */
    private static class EntryUpdater {
        void updateDetails(
                FinancialEntry entry,
                RecordType entryType,
                FinancialCategory category,
                Money amount,
                LocalDate entryDate,
                PaymentMethod paymentMethod,
                String description,
                User updater
        ) {
            entry.updateDetails(
                    entryType,
                    category,
                    amount,
                    entryDate,
                    paymentMethod,
                    description,
                    updater
            );
        }
    }

    // ============================================
    // COMMAND RECORDS
    // ============================================

    public record CreateEntryCommand(
            RecordType entryType,
            UUID categoryId,
            Money amount,
            LocalDate entryDate,
            PaymentMethod paymentMethod,
            String description,
            User creator,
            Long whoId,
            Long mainCategoryId,
            String recipient,
            String country,
            String city,
            String specificLocation,
            String vendor
    ) {}

    public record UpdateEntryCommand(
            UUID entryId,
            RecordType entryType,
            UUID categoryId,
            Money amount,
            LocalDate entryDate,
            PaymentMethod paymentMethod,
            String description,
            User updater
    ) {}

    public record UpdateEntryContextCommand(
            UUID entryId,
            Long whoId,
            Long mainCategoryId,
            String recipient,
            String country,
            String city,
            String specificLocation,
            String vendor,
            User updater
    ) {}

    public record UpdateEntryMetadataCommand(
            UUID entryId,
            String frequency,
            String priority,
            String tags,
            User updater
    ) {}

    public record UpdateReceiptNumberCommand(
            UUID entryId,
            String receiptNumber,
            User updater
    ) {}

    public record UpdateExchangeRateCommand(
            UUID entryId,
            BigDecimal rate,
            LocalDate rateDate,
            User updater
    ) {}

    public record DeleteEntryCommand(
            UUID entryId,
            User user
    ) {}

    public record AddAttachmentCommand(
            UUID entryId,
            FinancialEntryAttachment attachment,
            User updater
    ) {}

    public record RemoveAttachmentCommand(
            UUID entryId,
            UUID attachmentId,
            User updater
    ) {}

    // ============================================
    // CRITERIA RECORDS
    // ============================================

    public record EntrySearchCriteria(
            UUID categoryId,
            RecordType entryType,
            Long whoId,
            Long mainCategoryId,
            LocalDate startDate,
            LocalDate endDate
    ) {}

    public record TextSearchCriteria(
            String searchTerm,
            RecordType entryType,
            LocalDate startDate,
            LocalDate endDate
    ) {}

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