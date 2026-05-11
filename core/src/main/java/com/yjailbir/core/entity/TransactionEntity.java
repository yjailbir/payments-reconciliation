package com.yjailbir.core.entity;

import com.yjailbir.core.dto.TransactionEntityDto;
import com.yjailbir.core.dto.TransactionDtoFromBank;
import com.yjailbir.core.dto.TransactionStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor
public class TransactionEntity {
    @Id
    private UUID id;
    @Column(name = "transaction_id")
    private UUID transactionId;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chain_id")
    private PaymentEntity chain;
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private TransactionStatus status;
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
    private  Float multiplier;

    public TransactionEntity(TransactionDtoFromBank dto, PaymentEntity chain) {
        this.id = dto.paymentId();
        this.transactionId = dto.paymentId();
        this.chain = chain;
        this.status = TransactionStatus.PENDING;
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
    }

    public TransactionEntityDto toDto() {
        return new TransactionEntityDto(
                this.status,
                this.sender,
                this.receiver,
                this.sumIn,
                this.sumOut,
                this.commissionValue,
                this.commissionPercents,
                this.fixedCommission,
                this.timestamp
        );
    }
}
