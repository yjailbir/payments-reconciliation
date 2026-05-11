package com.yjailbir.core.service;

import com.yjailbir.core.dto.TransactionStatus;
import com.yjailbir.core.dto.ValidationResultDto;
import com.yjailbir.core.entity.PaymentEntity;
import com.yjailbir.core.entity.TransactionEntity;
import com.yjailbir.core.repository.TransactionChainRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ValidateService {
    private final TransactionChainRepository repository;

    public ValidationResultDto validate(TransactionEntity entity) {
        Optional<PaymentEntity> paymentEntity = repository.findByTransactionId(entity.getTransactionId());
        if (paymentEntity.isPresent()) {

        } else {
            return new ValidationResultDto(TransactionStatus.NOT_FOUND.getDescription(), entity.getTimestamp());
        }

        return null;
    }
}
