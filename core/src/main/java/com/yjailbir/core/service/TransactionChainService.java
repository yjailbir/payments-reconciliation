package com.yjailbir.core.service;

import com.yjailbir.core.dto.*;
import com.yjailbir.core.entity.PaymentEntity;
import com.yjailbir.core.entity.TransactionEntity;
import com.yjailbir.core.repository.PaymentsRepository;
import com.yjailbir.core.repository.TransactionsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionChainService {
    private final ValidateService validateService;
    private final PaymentsRepository paymentsRepository;
    private final TransactionsRepository transactionsRepository;

    @Transactional
    public ValidationResultDto saveAndValidate(TransactionDtoFromBank dto) {
        PaymentEntity chain = paymentsRepository
                .findByPaymentId(dto.getPaymentId())
                .orElseGet(() -> {
                    PaymentEntity newChain = new PaymentEntity(dto.getPaymentId());
                    return paymentsRepository.save(newChain);
                });

        TransactionEntity step = new TransactionEntity(dto, chain);
        chain.addStep(step);
        paymentsRepository.save(chain);
        return validateService.validate(step);
    }

    public List<PaymentDtoForFrontend> getAllPayments() {
        return paymentsRepository.findTop1000O().stream().map(PaymentEntity::toDto).toList();
    }

    public PaymentDtoForFrontend getPaymentById(UUID paymentId) {
        return paymentsRepository.findByPaymentId(paymentId).get().toDto();
    }

    public TransactionDtoForFrontend getTransactionById(UUID transactionId) {
        return transactionsRepository.findById(transactionId).get().toDto();
    }

    public List<TransactionDtoForFrontend> getAllTransactionsByPaymentId(UUID paymentId) {
       return transactionsRepository.findAllByPaymentIdOrderByTimestampAsc(paymentId).stream().map(TransactionEntity::toDto).toList();
    }
}