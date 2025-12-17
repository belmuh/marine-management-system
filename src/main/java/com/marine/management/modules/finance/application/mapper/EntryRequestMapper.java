package com.marine.management.modules.finance.application.mapper;

import com.marine.management.modules.finance.application.FinancialEntryService;
import com.marine.management.modules.finance.presentation.dto.MoneyDto;
import com.marine.management.modules.finance.presentation.dto.controller.CreateEntryRequest;
import com.marine.management.modules.finance.presentation.dto.controller.UpdateEntryRequest;
import com.marine.management.modules.users.domain.User;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class EntryRequestMapper {

    public FinancialEntryService.CreateEntryCommand toCreateEntryCommand(
            CreateEntryRequest request,
            User creator
    ) {
        var moneyDto = new MoneyDto(request.amount(), request.currency());
        var amount = moneyDto.toMoney();

        return new FinancialEntryService.CreateEntryCommand(
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

    public FinancialEntryService.UpdateEntryCommand toUpdateEntryCommand(
            UUID entryId,
            UpdateEntryRequest request,
            User updater
    ) {
        var moneyDto = new MoneyDto(request.amount(), request.currency());
        var amount = moneyDto.toMoney();

        return new FinancialEntryService.UpdateEntryCommand(
                entryId,
                request.entryType(),
                request.categoryId(),
                amount,
                request.entryDate(),
                request.paymentMethod(),
                request.description(),
                updater
        );
    }
}