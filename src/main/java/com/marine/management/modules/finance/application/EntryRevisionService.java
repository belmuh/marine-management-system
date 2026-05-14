package com.marine.management.modules.finance.application;

import com.marine.management.modules.finance.domain.entities.FinancialCategory;
import com.marine.management.modules.finance.domain.entities.FinancialEntry;
import com.marine.management.modules.finance.domain.entities.TenantMainCategory;
import com.marine.management.modules.finance.domain.entities.TenantWhoSelection;
import com.marine.management.modules.finance.domain.enums.PaymentMethod;
import com.marine.management.modules.finance.domain.enums.RecordType;
import com.marine.management.modules.finance.domain.vo.Money;
import com.marine.management.modules.finance.presentation.dto.EntryHistoryItemDto;
import com.marine.management.modules.finance.presentation.dto.EntryHistoryItemDto.RevisionDetails;
import com.marine.management.modules.finance.presentation.dto.EntryHistoryItemDto.RevisionDetails.FieldChange;
import com.marine.management.shared.domain.CustomRevisionEntity;
import jakarta.persistence.EntityManager;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.query.AuditEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Builds revision-based history events from Hibernate Envers audit data.
 *
 * Responsibilities:
 * - Query AuditReader for all revisions of a FinancialEntry
 * - Compare consecutive snapshots to detect meaningful field changes
 * - Map raw diffs to business-level events (amount updated, category changed, etc.)
 * - Resolve reference UUIDs to human-readable names (category, who, main category)
 *
 * Design decisions:
 * - Separate service (SRP): revision querying != timeline aggregation
 * - Status changes excluded from revision events (covered by approval events)
 * - Immutable fields excluded (createdAt, createdById, entryNumber)
 * - Auto-calculated fields excluded (exchangeRate, exchangeRateDate)
 * - Same revision = same transaction = grouped into single event
 */
@Service
@Transactional(readOnly = true)
public class EntryRevisionService {

    private static final Logger logger = LoggerFactory.getLogger(EntryRevisionService.class);

    /**
     * Fields excluded from diff comparison.
     * Status → covered by approval events
     * createdAt/createdById → immutable, no meaningful diff
     * entryNumber → immutable after creation
     * exchangeRate/exchangeRateDate → auto-calculated from amount/date
     */
    private static final Set<String> EXCLUDED_FIELDS = Set.of(
            "status", "createdAt", "createdById", "entryNumber",
            "exchangeRate", "exchangeRateDate"
    );

    private final EntityManager entityManager;

    public EntryRevisionService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * Build revision events for a financial entry.
     *
     * @param entryId the entry to query revisions for
     * @return list of revision-based history events (not sorted — caller merges and sorts)
     */
    @SuppressWarnings("unchecked")
    public List<EntryHistoryItemDto> buildRevisionEvents(UUID entryId) {
        var reader = AuditReaderFactory.get(entityManager);

        List<Object[]> revisions;
        try {
            revisions = reader.createQuery()
                    .forRevisionsOfEntity(FinancialEntry.class, false, true)
                    .add(AuditEntity.id().eq(entryId))
                    .addOrder(AuditEntity.revisionNumber().asc())
                    .getResultList();
        } catch (Exception e) {
            logger.warn("Failed to query Envers revisions for entry {}: {}", entryId, e.getMessage());
            return List.of();
        }

        if (revisions.isEmpty()) {
            return List.of();
        }

        List<EntryHistoryItemDto> events = new ArrayList<>();
        FinancialEntry previous = null;

        for (Object[] row : revisions) {
            FinancialEntry snapshot = (FinancialEntry) row[0];
            CustomRevisionEntity rev = (CustomRevisionEntity) row[1];
            RevisionType revType = (RevisionType) row[2];

            LocalDateTime timestamp = Instant.ofEpochMilli(rev.getTimestamp())
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();

            String userName = resolveRevisionUser(rev);

            switch (revType) {
                case ADD -> events.add(EntryHistoryItemDto.created(timestamp, userName));

                case MOD -> {
                    if (previous != null) {
                        List<FieldChange> changes = computeFieldChanges(previous, snapshot);
                        if (!changes.isEmpty()) {
                            String description = buildChangeDescription(changes);
                            events.add(EntryHistoryItemDto.updated(
                                    timestamp,
                                    userName,
                                    description,
                                    new RevisionDetails(rev.getId(), changes)
                            ));
                        }
                    }
                }

                case DEL -> events.add(EntryHistoryItemDto.deleted(timestamp, userName));
            }

            previous = snapshot;
        }

        logger.debug("Built {} revision events for entry {}", events.size(), entryId);
        return events;
    }

    // ─── Field change detection ───

    /**
     * Compare two consecutive snapshots and return meaningful field changes.
     * Skips excluded fields and only includes fields that actually changed.
     */
    private List<FieldChange> computeFieldChanges(FinancialEntry prev, FinancialEntry curr) {
        List<FieldChange> changes = new ArrayList<>();

        // Entry type
        compareAndAdd(changes, "Entry Type",
                formatEnum(prev.getEntryType()), formatEnum(curr.getEntryType()));

        // Amounts
        compareAndAdd(changes, "Original Amount",
                formatMoney(prev.getOriginalAmount()), formatMoney(curr.getOriginalAmount()));
        compareAndAdd(changes, "Base Amount",
                formatMoney(prev.getBaseAmount()), formatMoney(curr.getBaseAmount()));
        compareAndAdd(changes, "Approved Amount",
                formatMoney(prev.getApprovedBaseAmount()), formatMoney(curr.getApprovedBaseAmount()));
        compareAndAdd(changes, "Paid Amount",
                formatMoney(prev.getPaidBaseAmount()), formatMoney(curr.getPaidBaseAmount()));

        // Business fields
        compareAndAdd(changes, "Entry Date",
                formatDate(prev.getEntryDate()), formatDate(curr.getEntryDate()));
        compareAndAdd(changes, "Payment Method",
                formatPaymentMethod(prev.getPaymentMethod()), formatPaymentMethod(curr.getPaymentMethod()));
        compareAndAdd(changes, "Receipt Number",
                prev.getReceiptNumber(), curr.getReceiptNumber());

        // Reference fields (with name resolution)
        compareAndAdd(changes, "Category",
                resolveCategoryName(prev.getCategory()), resolveCategoryName(curr.getCategory()));
        compareAndAdd(changes, "Who",
                resolveWhoName(prev.getTenantWho()), resolveWhoName(curr.getTenantWho()));
        compareAndAdd(changes, "Main Category",
                resolveMainCategoryName(prev.getTenantMainCategory()),
                resolveMainCategoryName(curr.getTenantMainCategory()));

        return changes;
    }

    /**
     * Add a field change only if old and new values differ.
     */
    private void compareAndAdd(List<FieldChange> changes, String fieldName,
                               String oldValue, String newValue) {
        String old = normalizeValue(oldValue);
        String curr = normalizeValue(newValue);

        if (!Objects.equals(old, curr)) {
            changes.add(new FieldChange(fieldName, old, curr));
        }
    }

    private String normalizeValue(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    // ─── Formatting helpers ───

    private String formatMoney(Money money) {
        if (money == null) return null;
        return money.getAmount().toPlainString() + " " + money.getCurrencyCode();
    }

    private String formatEnum(Enum<?> value) {
        if (value == null) return null;
        if (value instanceof RecordType rt) {
            return rt.name().substring(0, 1) + rt.name().substring(1).toLowerCase();
        }
        return value.name();
    }

    private String formatPaymentMethod(PaymentMethod pm) {
        if (pm == null) return null;
        // Convert CREDIT_CARD → Credit Card
        return Arrays.stream(pm.name().split("_"))
                .map(word -> word.substring(0, 1) + word.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }

    private String formatDate(LocalDate date) {
        return date != null ? date.toString() : null;
    }

    // ─── Reference name resolution ───

    /**
     * Resolve category entity to display name.
     * Uses proxy — getId() doesn't trigger lazy load, getName() does.
     * Within @Transactional context, lazy loading works fine.
     */
    private String resolveCategoryName(FinancialCategory category) {
        if (category == null) return null;
        try {
            return category.getName();
        } catch (Exception e) {
            // Fallback if entity can't be loaded (soft-deleted, filter issues)
            logger.debug("Could not resolve category name: {}", e.getMessage());
            return "Unknown category";
        }
    }

    /**
     * Resolve TenantWhoSelection → Who.nameEn for display.
     */
    private String resolveWhoName(TenantWhoSelection tenantWho) {
        if (tenantWho == null) return null;
        try {
            return tenantWho.getWho() != null ? tenantWho.getWho().getNameEn() : null;
        } catch (Exception e) {
            logger.debug("Could not resolve who name: {}", e.getMessage());
            return "Unknown who";
        }
    }

    /**
     * Resolve TenantMainCategory → MainCategory.nameEn for display.
     */
    private String resolveMainCategoryName(TenantMainCategory tenantMainCategory) {
        if (tenantMainCategory == null) return null;
        try {
            return tenantMainCategory.getMainCategory() != null
                    ? tenantMainCategory.getMainCategory().getNameEn()
                    : null;
        } catch (Exception e) {
            logger.debug("Could not resolve main category name: {}", e.getMessage());
            return "Unknown main category";
        }
    }

    // ─── Description builder ───

    /**
     * Build a human-readable description summarizing field changes.
     * Examples:
     * - "Amount updated" (single amount field)
     * - "Category changed, amount updated" (multiple fields)
     * - "3 fields updated" (many fields)
     */
    private String buildChangeDescription(List<FieldChange> changes) {
        if (changes.size() == 1) {
            return changes.get(0).fieldName() + " updated";
        }
        if (changes.size() <= 3) {
            return changes.stream()
                    .map(FieldChange::fieldName)
                    .collect(Collectors.joining(", ")) + " updated";
        }
        return changes.size() + " fields updated";
    }

    /**
     * Resolve user display name from revision entity.
     * Prefers userDisplayName (snapshot at revision time), falls back to username.
     */
    private String resolveRevisionUser(CustomRevisionEntity rev) {
        if (rev.getUserDisplayName() != null && !rev.getUserDisplayName().isBlank()) {
            return rev.getUserDisplayName();
        }
        if (rev.getUsername() != null && !rev.getUsername().isBlank()) {
            return rev.getUsername();
        }
        return null;
    }
}
