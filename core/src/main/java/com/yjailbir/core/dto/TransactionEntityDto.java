package com.yjailbir.core.dto;

import java.time.LocalDateTime;

public record TransactionEntityDto(
        TransactionStatus status,
        String sender,
        String receiver,
        Long sumIn,
        Long sumOut,
        Long commissionValue,
        Integer commissionPercents,
        Integer fixedCommission,
        LocalDateTime timestamp
) {
}
