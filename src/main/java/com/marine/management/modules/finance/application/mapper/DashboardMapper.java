package com.marine.management.modules.finance.application.mapper;

import com.marine.management.modules.finance.application.dto.CumulativeBalanceDTO;
import com.marine.management.modules.finance.domain.vo.CumulativeBalance;
import org.springframework.stereotype.Component;

import java.util.List;

// Separate mapper
@Component
public class DashboardMapper {

    public CumulativeBalanceDTO toDTO(CumulativeBalance cumulative) {
        return new CumulativeBalanceDTO(
                cumulative.getMonth().toString(),
                cumulative.getIncome().getAmount(),
                cumulative.getExpense().getAmount(),
                cumulative.getCumulativeBalance().getAmount(),
                cumulative.isDeficit(),
                cumulative.isCritical(),
                cumulative.isWarning()
        );
    }

    public List<CumulativeBalanceDTO> toDTOList(List<CumulativeBalance> cumulativeBalances) {
        return cumulativeBalances.stream()
                .map(this::toDTO)
                .toList();
    }
}
