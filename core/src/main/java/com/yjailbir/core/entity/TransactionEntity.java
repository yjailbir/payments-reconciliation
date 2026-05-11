package com.yjailbir.core.entity;

import com.yjailbir.core.dto.TransactionDetailsDto;
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

    public TransactionEntity(TransactionDto dto, TransactionChainEntity chain) {
        this.transactionId = dto.transactionId();
        this.chain = chain;
        this.status = TransactionStatus.PENDING;
        this.sender = dto.from();
        this.reciever = dto.to();
        this.sumIn = dto.sumIn();
        this.sumOut = dto.sumOut();
        this.commissionValue = dto.commissionValue();
        this.commissionPercents = dto.commissionPercents();
        this.fixedCommission = dto.fixedCommission();
        this.timestamp = dto.timestamp();
    }

    public TransactionDetailsDto toDto() {
        return new TransactionDetailsDto(
                this.status,
                this.sender,
                this.reciever,
                this.sumIn,
                this.sumOut,
                this.commissionValue,
                this.commissionPercents,
                this.fixedCommission,
                this.timestamp
        );
    }
}
