package com.yjailbir.core.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record TransactionDtoFromBank(
        UUID paymentId,
        UUID transactionId,
        TransactionType transactionType,
        String from,
        String to,
        Long sumIn,
        Long sumOut,
        Long commissionValue,
        Integer commissionPercents,
        Integer fixedCommission,
        String roundingMode,
        Boolean percentsFirst,
        LocalDateTime timestamp,
        String fromCurrency,
        String toCurrency,
        Float multiplier
) {
}
