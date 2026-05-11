package com.yjailbir.core.dto;

import java.util.List;
import java.util.UUID;

public record ChainDetailsDto(
        UUID uuid,
        List<TransactionDetailsDto> transactionDetails
) {
}
