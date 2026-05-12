package com.yjailbir.core.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ValidationResultDto(
        String status,
        LocalDateTime time,
        List<String> warnings,
        List<String> errors
) {
}
