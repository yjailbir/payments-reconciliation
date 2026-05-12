package com.yjailbir.core.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
public class TransactionDtoFromBank {
    UUID paymentId;
    UUID transactionId;
    TransactionType transactionType;
    String from;
    String to;
    Long sumIn;
    Long sumOut;
    Long commissionValue;
    Integer commissionPercents;
    Integer fixedCommission;
    String roundingMode;
    Boolean percentsFirst;
    LocalDateTime timestamp;
    String fromCurrency;
    String toCurrency;
    Double multiplier;
    String notCountedHistory;
}
