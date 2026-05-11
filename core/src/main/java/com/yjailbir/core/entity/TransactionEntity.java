package com.yjailbir.core.entity;

import com.yjailbir.core.dto.TransactionDto;
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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "transaction_id")
    private UUID transactionId;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chain_id")
    private TransactionChainEntity chain;
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private TransactionStatus status;
    @Column(name = "sender")
    private String sender;
    @Column(name = "receiver")
    private String reciever;
    @Column(name = "sum_in")
    private Long sumIn;
    @Column(name = "sum_out")
    private Long sumOut;
    @Column(name = "commission_value")
    Long commissionValue;
    @Column(name = "commission_percents")
    Integer commissionPercents;
    @Column(name = "fixed_commission")
    Integer fixedCommission;
    @Column(name = "timestamp")
    LocalDateTime timestamp;

    public TransactionEntity(TransactionDto transactionDto, TransactionChainEntity transactionChainEntity) {
        this.transactionId = transactionDto.transactionId();
        this.chain = transactionChainEntity;
        this.status = TransactionStatus.PENDING;
        this.sender = transactionDto.from();
        this.reciever = transactionDto.to();
        this.sumIn = transactionDto.sumIn();
        this.sumOut = transactionDto.sumOut();
        this.commissionValue = transactionDto.commissionValue();
        this.commissionPercents = transactionDto.commissionPercents();
        this.fixedCommission = transactionDto.fixedCommission();
        this.timestamp = transactionDto.timestamp();
    }
}
