package com.yjailbir.core.dto;

import java.util.List;
import java.util.UUID;

public record OneTransactionComment(
        UUID transactionId,
        List<String> errors,
        List<String> warnings
) {
}
