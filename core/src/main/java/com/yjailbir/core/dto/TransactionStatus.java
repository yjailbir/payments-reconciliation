package com.yjailbir.core.dto;

import com.fasterxml.jackson.annotation.JsonValue;

public enum TransactionStatus {
    SUCCESS("Успешно"),
    FAILURE("Ошибка"),
    PENDING("В обработке"),
    NOT_FOUND("Транзакиця не найдена");

    private final String description;

    TransactionStatus(String description) {
        this.description = description;
    }

    @JsonValue
    public String getDescription() {
        return description;
    }
}
