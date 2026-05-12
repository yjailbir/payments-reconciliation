package com.yjailbir.core.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentDtoForFrontend(
        UUID paymentId,
        String status,
        LocalDateTime created,
        LocalDateTime updated
) {
}
