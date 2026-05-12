package com.yjailbir.core.repository;

import com.yjailbir.core.entity.PaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentsRepository extends JpaRepository<PaymentEntity, Long> {
    Optional<PaymentEntity> findByPaymentId(UUID transactionId);
    @Query(
            nativeQuery = true,
            value = "SELECT * FROM payments ORDER BY created DESC LIMIT 1000"
    )
    List<PaymentEntity> findTop1000O();
}
