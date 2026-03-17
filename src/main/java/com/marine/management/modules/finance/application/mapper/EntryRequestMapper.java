package com.marine.management.modules.finance.application.mapper;

import com.marine.management.modules.finance.application.commands.*;
import com.marine.management.modules.finance.infrastructure.query.EntrySearchCriteria;
import com.marine.management.modules.finance.presentation.dto.MoneyDto;
import com.marine.management.modules.finance.presentation.dto.controller.*;
import com.marine.management.modules.users.domain.User;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class EntryRequestMapper {

    public CreateEntryCommand toCreateEntryCommand(
            CreateEntryRequest request,
            User creator
    ) {
        var moneyDto = new MoneyDto(request.amount(), request.currency());
        var amount = moneyDto.toMoney();

        return new CreateEntryCommand(
                request.entryType(),
                request.categoryId(),
                amount,
                request.entryDate(),
                request.paymentMethod(),
                request.description(),
                creator,
                request.whoId(),
                request.mainCategoryId(),
                request.recipient(),
                request.country(),
                request.city(),
                request.specificLocation(),
                request.vendor()
        );
    }

    public UpdateEntryCommand toUpdateEntryCommand(
            UUID entryId,
            UpdateEntryRequest request,
            User updater
    ) {
        var moneyDto = new MoneyDto(request.amount(), request.currency());
        var amount = moneyDto.toMoney();

        return new UpdateEntryCommand(
                entryId,
                request.entryType(),
                request.categoryId(),
                amount,
                request.entryDate(),
                request.paymentMethod(),
                request.description(),
                updater,
                request.whoId(),
                request.mainCategoryId(),
                request.recipient(),
                request.country(),
                request.city(),
                request.specificLocation(),
                request.vendor(),
                request.receiptNumber()


        );
    }

    // ============================================
    // PARTIAL UPDATES (Patch)
    // ============================================

    public UpdateEntryContextCommand toUpdateEntryContextCommand(
            UUID entryId,
            UpdateEntryContextRequest request,
            User updater
    ) {
        return new UpdateEntryContextCommand(
                entryId,
                request.whoId(),
                request.mainCategoryId(),
                request.recipient(),
                request.country(),
                request.city(),
                request.specificLocation(),
                request.vendor(),
                updater
        );
    }

    public UpdateEntryMetadataCommand toUpdateEntryMetadataCommand(
            UUID entryId,
            UpdateEntryMetadataRequest request,
            User updater
    ) {
        return new UpdateEntryMetadataCommand(
                entryId,
                request.frequency(),
                request.priority(),
                request.tags(),
                updater
        );
    }

    public UpdateReceiptNumberCommand toUpdateReceiptNumberCommand(
            UUID entryId,
            UpdateReceiptNumberRequest request,
            User updater
    ) {
        return new UpdateReceiptNumberCommand(
                entryId,
                request.receiptNumber(),
                updater
        );
    }

    public UpdateExchangeRateCommand toUpdateExchangeRateCommand(
            UUID entryId,
            UpdateExchangeRateRequest request,
            User updater
    ) {
        return new UpdateExchangeRateCommand(
                entryId,
                request.rate(),
                request.rateDate(),
                updater
        );
    }

    // ============================================
    // DELETE
    // ============================================

    public DeleteEntryCommand toDeleteEntryCommand(
            UUID entryId,
            User user
    ) {
        return new DeleteEntryCommand(entryId, user);
    }

    // ============================================
    // SEARCH
    // ============================================

    /**
     * Controller request'inden EntrySearchCriteria oluşturur.
     * Tüm search parametrelerini (filter + sort + pagination) içerir.
     */
    public EntrySearchCriteria toSearchCriteria(EntrySearchRequest request) {
        return EntrySearchCriteria.builder()
                .categoryId(request.categoryId())
                .entryType(request.entryType())
                .whoId(request.whoId())
                .mainCategoryId(request.mainCategoryId())
                .status(request.status())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .searchTerm(request.searchTerm())
                .sortColumn(request.sortColumn())
                .sortDirection(request.sortDirection())
                .page(request.page())
                .size(request.size())
                .build();
    }

    public EntrySearchCriteria toSearchCriteria(TextSearchRequest request) {
        return EntrySearchCriteria.builder()
                .searchTerm(request.searchTerm())
                .entryType(request.entryType())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .sortColumn(request.sortColumn())
                .sortDirection(request.sortDirection())
                .page(request.page())
                .size(request.size())
                .build();
    }
}