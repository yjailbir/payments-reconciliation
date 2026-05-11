package com.yjailbir.core.dto;

import java.time.LocalDateTime;

public record ValidationResultDto(
        String status,
        LocalDateTime time
) {
}
