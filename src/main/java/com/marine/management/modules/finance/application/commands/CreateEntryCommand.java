package com.marine.management.modules.finance.application.commands;


import com.marine.management.modules.finance.domain.enums.PaymentMethod;
import com.marine.management.modules.finance.domain.enums.RecordType;
import com.marine.management.modules.finance.domain.vo.Money;
import com.marine.management.modules.users.domain.User;

import java.time.LocalDate;
import java.util.UUID;

public record CreateEntryCommand(
        RecordType entryType,
        UUID categoryId,
        Money amount,
        LocalDate entryDate,
        PaymentMethod paymentMethod,
        String description,
        User creator,
        UUID whoId,
        UUID mainCategoryId,
        String recipient,
        String country,
        String city,
        String specificLocation,
        String vendor
) {}

