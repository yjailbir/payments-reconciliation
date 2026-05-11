package com.yjailbir.core.repository;

import com.yjailbir.core.entity.TransactionChainEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionChainRepository extends JpaRepository<TransactionChainEntity, Long> {
    Optional<TransactionChainEntity> findByTransactionId(UUID transactionId);
}
