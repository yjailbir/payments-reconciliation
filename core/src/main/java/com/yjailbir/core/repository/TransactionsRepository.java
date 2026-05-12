package com.yjailbir.core.repository;

import com.yjailbir.core.entity.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TransactionsRepository extends JpaRepository<TransactionEntity, UUID> {
    //От поздних к ранним
    List<TransactionEntity> findAllByPaymentIdOrderByTimestampDesc(UUID transactionId);
}
