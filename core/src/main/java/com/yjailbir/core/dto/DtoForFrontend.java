package com.yjailbir.core.dto;

import java.util.List;

public record DtoForFrontend(
        Integer success,
        Integer warning,
        Integer failure,
        List<OneTransactionComment>  comments
) {
}
