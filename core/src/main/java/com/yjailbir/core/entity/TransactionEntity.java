package com.yjailbir.core.entity;

import com.yjailbir.core.dto.TransactionDtoForFrontend;
import com.yjailbir.core.dto.TransactionDtoFromBank;
import com.yjailbir.core.dto.TransactionStatus;
import com.yjailbir.core.dto.TransactionType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor
public class TransactionEntity {
    @Id
    private UUID id;
    @Column(name = "payment_id")
    private UUID paymentId;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chain_id")
    private PaymentEntity chain;
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private TransactionStatus status;
    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private TransactionType type;
    @Column(name = "sender")
    private String sender;
    @Column(name = "receiver")
    private String receiver;
    @Column(name = "sum_in")
    private Long sumIn;
    @Column(name = "sum_out")
    private Long sumOut;
    @Column(name = "commission_value")
    private Long commissionValue;
    @Column(name = "commission_percents")
    private Integer commissionPercents;
    @Column(name = "fixed_commission")
    private Integer fixedCommission;
    @Column(name = "rounding_mode")
    private String roundingMode;
    @Column(name = "percents_first")
    private Boolean percentsFirst;
    @Column(name = "timestamp")
    private LocalDateTime timestamp;
    @Column(name = "from_currency")
    private String fromCurrency;
    @Column(name = "to_currency")
    private String toCurrency;
    @Column(name = "course")
    private Double multiplier;
    @Column(name = "not_counted_history")
    String notCountedHistory;
    @Column(name = "warning_comments")
    private String warningComments;
    @Column(name = "error_comments")
    private String errorComments;

    public TransactionEntity(TransactionDtoFromBank dto, PaymentEntity chain) {
        this.id = dto.transactionId();
        this.paymentId = dto.paymentId();
        this.chain = chain;
        this.status = TransactionStatus.PENDING;
        this.type = dto.transactionType();
        this.sender = dto.from();
        this.receiver = dto.to();
        this.sumIn = dto.sumIn();
        this.sumOut = dto.sumOut();
        this.commissionValue = dto.commissionValue();
        this.commissionPercents = dto.commissionPercents();
        this.fixedCommission = dto.fixedCommission();
        this.roundingMode = dto.roundingMode();
        this.percentsFirst = dto.percentsFirst();
        this.timestamp = dto.timestamp();
        this.fromCurrency = dto.fromCurrency();
        this.toCurrency = dto.toCurrency();
        this.multiplier = dto.multiplier();
        this.notCountedHistory = dto.notCountedHistory();
    }

    public TransactionDtoForFrontend toDto() {
        return new TransactionDtoForFrontend(
                this.id,
                this.status.getDescription(),
                this.type.getDescription(),
                this.sender,
                this.receiver,
                this.sumIn,
                this.sumOut,
                this.commissionValue,
                this.commissionPercents,
                this.fixedCommission,
                this.timestamp,
                this.fromCurrency,
                this.toCurrency,
                BigDecimal.valueOf(this.multiplier).setScale(4, RoundingMode.valueOf(this.roundingMode)).toString(),
                List.of(this.warningComments.split(", ")),
                List.of(this.errorComments.split(", "))
        );
    }
}
