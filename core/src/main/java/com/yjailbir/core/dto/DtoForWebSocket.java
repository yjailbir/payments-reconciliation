package com.yjailbir.core.dto;

import java.util.List;

public record DtoForWebSocket(
        Integer success,
        Integer warning,
        Integer failure,
        List<OneTransactionComment>  comments
) {
}
