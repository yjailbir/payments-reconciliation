package com.yjailbir.core.entity;

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
    @Column(name = "start_sum")
    private Long startSum;
    @Column(name = "last_sum")
    private Long lastSum;

    public PaymentEntity(UUID paymentId) {
        this.paymentId = paymentId;
        this.created = LocalDateTime.now();
        this.lastUpdated = LocalDateTime.now();
    }

    public void addStep(TransactionEntity step) {
        steps.add(step);
        step.setChain(this);
        this.lastUpdated = LocalDateTime.now();
    }
}
