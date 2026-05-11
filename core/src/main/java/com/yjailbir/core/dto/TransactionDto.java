package com.yjailbir.core.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record TransactionDto(
        UUID transactionId,
        TransactionStatus status,
        TransactionType transactionType,
        String from,
        String to,
        Long sumIn,
        Long sumOut,
        Long commissionValue,
        Integer commissionPercents,
        Integer fixedCommission,
        LocalDateTime timestamp
) {
}
