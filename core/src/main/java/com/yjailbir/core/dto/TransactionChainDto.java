package com.yjailbir.core.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record TransactionChainDto(
        UUID transactionId,
        List<TransactionDto> chain,
        LocalDateTime created,
        LocalDateTime lastUpdated,
        Long startSum,
        Long lastSum
) {
}
