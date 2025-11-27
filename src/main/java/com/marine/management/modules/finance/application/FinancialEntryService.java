package com.marine.management.modules.finance.application;

import com.marine.management.modules.finance.domain.*;
import com.marine.management.modules.finance.domain.model.Money;
import com.marine.management.modules.finance.infrastructure.FinancialCategoryRepository;
import com.marine.management.modules.finance.infrastructure.FinancialEntryRepository;
import com.marine.management.modules.finance.presentation.dto.reports.*;
import com.marine.management.modules.users.domain.User;
import com.marine.management.shared.exceptions.EntryNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.IntStream;

@Service
@Transactional(readOnly = true)
public class FinancialEntryService {

    private final FinancialEntryRepository entryRepository;
    private final FinancialCategoryRepository categoryRepository;

    public FinancialEntryService(FinancialEntryRepository entryRepository,
                                 FinancialCategoryRepository categoryRepository) {
        this.entryRepository = entryRepository;
        this.categoryRepository = categoryRepository;
    }


    @Transactional
    public FinancialEntry create(
            EntryType entryType,
            UUID categoryId,
            Money amount,
            LocalDate entryDate,
            User creator,
            String description
    ) {
        // 1. Validate and get category
        FinancialCategory category = getCategoryByIdOrThrow(categoryId);

        // 2. Generate entry number (database sequence - thread-safe)
        EntryNumber entryNumber = generateEntryNumber();

        // 3. Create entry
        FinancialEntry entry = FinancialEntry.create(
                entryNumber,
                entryType,
                category,
                amount,
                entryDate,
                creator,
                description
        );

        // 4. Save
        return entryRepository.save(entry);
    }

    @Transactional
    public FinancialEntry createIncome(
            UUID categoryId,
            Money amount,
            LocalDate entryDate,
            User creator,
            String description
    ) {
        return create(EntryType.INCOME, categoryId, amount, entryDate, creator, description);
    }

    @Transactional
    public FinancialEntry createExpense(
            UUID categoryId,
            Money amount,
            LocalDate entryDate,
            User creator,
            String description
    ) {
        return create(EntryType.EXPENSE, categoryId, amount, entryDate, creator, description);
    }

    // UPDATE
    @Transactional
    public FinancialEntry update(
            UUID entryId,
            UUID categoryId,
            Money amount,
            LocalDate entryDate,
            String description,
            User user
    ) {
        FinancialEntry entry = getByIdOrThrow(entryId);
        FinancialCategory category = getCategoryByIdOrThrow(categoryId);

        entry.updateDetails(category, amount, entryDate, description, user);
        return entry; // JPA dirty checking
    }

    @Transactional
    public FinancialEntry updateReceiptNumber(
            UUID entryId,
            String receiptNumber,
            User user
    ) {
        FinancialEntry entry = getByIdOrThrow(entryId);
        entry.updateReceiptNumber(receiptNumber, user);
        return entry;
    }

    @Transactional
    public FinancialEntry setExchangeRate(
            UUID entryId,
            BigDecimal rate,
            LocalDate rateDate,
            User user
    ) {
        FinancialEntry entry = getByIdOrThrow(entryId);
        entry.setExchangeRate(rate, rateDate, user);
        return entry;
    }

    // DELETE
    @Transactional
    public void delete(UUID entryId, User user) {
        FinancialEntry entry = getByIdOrThrow(entryId);
        entry.canBeEditedBy(user); // Permission check
        entryRepository.delete(entry);
    }

    // ATTACHMENTS

    @Transactional
    public FinancialEntry addAttachment(
            UUID entryId,
            FinancialEntryAttachment attachment,
            User user
    ) {
        FinancialEntry entry = getByIdOrThrow(entryId);
        entry.addAttachment(attachment, user);
        return entry;
    }

    @Transactional
    public FinancialEntry removeAttachment(
            UUID entryId,
            UUID attachmentId,
            User user
    ) {
        FinancialEntry entry = getByIdOrThrow(entryId);
        FinancialEntryAttachment attachment = entry.getAttachments().stream()
                .filter(a -> a.getId().equals(attachmentId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Attachment not found"));

        entry.removeAttachment(attachment, user);
        return entry;
    }

    // QUERY
    public Optional<FinancialEntry> findById(UUID id) {
        return entryRepository.findById(id);
    }

    public Optional<FinancialEntry> findByEntryNumber(String entryNumber) {
        return entryRepository.findByEntryNumber_Value(entryNumber);
    }

    public Page<FinancialEntry> findByUser(User user, Pageable pageable) {
        return entryRepository.findByCreatedBy(user, pageable);
    }

    public Page<FinancialEntry> findByDateRange(
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable
    ) {
        return entryRepository.findByEntryDateBetween(startDate, endDate, pageable);
    }

    public Page<FinancialEntry> findByType(EntryType type, Pageable pageable) {
        return entryRepository.findByEntryType(type, pageable);
    }

    public Page<FinancialEntry> findByCategory(UUID categoryId, Pageable pageable) {
        FinancialCategory category = getCategoryByIdOrThrow(categoryId);
        return entryRepository.findByCategory(category, pageable);
    }

    // SEARCH
    public Page<FinancialEntry> search(
            UUID categoryId,
            EntryType entryType,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable
    ) {
        return entryRepository.search(categoryId, entryType, startDate, endDate, pageable);
    }

    public Page<FinancialEntry> searchByText(
            String searchTerm,
            EntryType entryType,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable
    ) {
        return entryRepository.searchByText(searchTerm, entryType, startDate, endDate, pageable);
    }

    // DASHBOARD
    public List<FinancialEntryRepository.PeriodTotalProjection> getPeriodTotals(
            LocalDate startDate,
            LocalDate endDate
    ) {
        return entryRepository.findPeriodTotals(startDate, endDate);
    }

    public List<FinancialEntryRepository.CategoryTotalProjection> getCategoryTotals(
            EntryType entryType,
            LocalDate startDate,
            LocalDate endDate
    ) {
        return entryRepository.findCategoryTotals(entryType, startDate, endDate);
    }

    public List<FinancialEntryRepository.CategoryTotalProjection> getExpenseTotals(
            LocalDate startDate,
            LocalDate endDate
    ) {
        return entryRepository.findExpenseTotals(startDate, endDate);
    }

    public List<FinancialEntryRepository.CategoryTotalProjection> getIncomeTotals(
            LocalDate startDate,
            LocalDate endDate
    ) {
        return entryRepository.findIncomeTotals(startDate, endDate);
    }

    public List<FinancialEntryRepository.MonthlyTotalProjection> getMonthlyTotals(
            LocalDate startDate,
            LocalDate endDate
    ) {
        return entryRepository.findMonthlyTotals(startDate, endDate);
    }

    public BigDecimal getTotalByType(
            EntryType entryType,
            LocalDate startDate,
            LocalDate endDate
    ) {
        return entryRepository.sumByEntryTypeAndDateRange(entryType, startDate, endDate);
    }

    public long countByType(
            EntryType entryType,
            LocalDate startDate,
            LocalDate endDate
    ) {
        return entryRepository.countByEntryTypeAndEntryDateBetween(entryType, startDate, endDate);
    }

    // DASHBOARD SUMMARY
    public DashboardSummary getDashboardSummary(LocalDate startDate, LocalDate endDate) {
        BigDecimal totalIncome = getTotalByType(EntryType.INCOME, startDate, endDate);
        BigDecimal totalExpense = getTotalByType(EntryType.EXPENSE, startDate, endDate);
        BigDecimal balance = totalIncome.subtract(totalExpense);

        long incomeCount = countByType(EntryType.INCOME, startDate, endDate);
        long expenseCount = countByType(EntryType.EXPENSE, startDate, endDate);

        return new DashboardSummary(
                totalIncome,
                totalExpense,
                balance,
                incomeCount,
                expenseCount
        );
    }

    // HELPER
    private FinancialEntry getByIdOrThrow(UUID id) {
        return entryRepository.findById(id)
                .orElseThrow(() -> EntryNotFoundException.withId(id));
    }

    private FinancialCategory getCategoryByIdOrThrow(UUID id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found with id: " + id));
    }

    private EntryNumber generateEntryNumber() {
        int sequence = entryRepository.getNextSequence();
        return EntryNumber.generate(sequence);
    }

    // RECORD


}
