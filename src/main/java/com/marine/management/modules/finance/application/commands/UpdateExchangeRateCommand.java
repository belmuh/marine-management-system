package com.marine.management.modules.finance.application.commands;

import com.marine.management.modules.users.domain.User;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record UpdateExchangeRateCommand(
        UUID entryId,
        BigDecimal rate,
        LocalDate rateDate,
        User updater
) {
    
}
