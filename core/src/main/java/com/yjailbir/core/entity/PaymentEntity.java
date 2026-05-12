package com.yjailbir.core.entity;

import com.yjailbir.core.dto.PaymentDtoForFrontend;
import com.yjailbir.core.dto.TransactionStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "transaction_chains")
@Getter
@Setter
@NoArgsConstructor
public class PaymentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "payment_id")
    private UUID paymentId;
    @Column(name = "created", updatable = false)
    private LocalDateTime created;
    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;
    @OneToMany(mappedBy = "chain", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TransactionEntity> steps = new ArrayList<>();

    public PaymentEntity(UUID paymentId) {
        this.paymentId = paymentId;
        this.created = LocalDateTime.now();
        this.lastUpdated = LocalDateTime.now();
    }

    public void addStep(TransactionEntity step) {
        steps.add(step);
        step.setChain(this);
        if (step.getTimestamp().isAfter(this.lastUpdated)) {
            this.lastUpdated = step.getTimestamp();
        }
    }

    public PaymentDtoForFrontend toDto() {
        TransactionStatus status = TransactionStatus.SUCCESS;

        for (TransactionEntity step : steps) {
            if (step.getStatus() == TransactionStatus.WARNING) {
                status = TransactionStatus.WARNING;
                continue;
            }
            if (step.getStatus() == TransactionStatus.FAILURE) {
                status = TransactionStatus.FAILURE;
                break;
            }

            return new PaymentDtoForFrontend(paymentId, status.getDescription(), created, lastUpdated);
        }

        return new PaymentDtoForFrontend(paymentId, status.getDescription(), created, lastUpdated);
    }
}
