package com.yjailbir.core.dto;

import com.fasterxml.jackson.annotation.JsonValue;

public enum TransactionType {
    CARD("Оплата картой"),
    QR("Оплата QR кодом"),
    NFC("Бесконтактная оплата"),
    BY_PHONE_NUMBER("Перевод по номеру телефона"),
    BY_CARD_NUMBER("Перевод по номеру карты"),
    CASH("Снятие наличных"),
    BIOMETRY("Оплата биометрией"),
    BANK_THING("Межбанковское взаимодействие");

    private final String description;

    TransactionType(String description) {
        this.description = description;
    }

    @JsonValue
    public String getDescription() {
        return description;
    }
}
