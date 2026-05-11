package com.yjailbir.core.dto;

import java.time.LocalDateTime;

public record ValidationResultDto(
        TransactionStatus status,
        LocalDateTime time
) {
}
