package com.yjailbir.core.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record TransactionDtoForFrontend(
        UUID transactionId,
        String status,
        String transactionType,
        String sender,
        String receiver,
        Long sumIn,
        Long sumOut,
        Long commissionValue,
        Integer commissionPercents,
        Integer fixedCommission,
        LocalDateTime timestamp,
        String fromCurrency,
        String toCurrency,
        String course,
        List<String> warnings,
        List<String> errors
) {
}
