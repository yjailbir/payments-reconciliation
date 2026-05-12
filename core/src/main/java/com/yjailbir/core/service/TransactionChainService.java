package com.yjailbir.core.service;

import com.yjailbir.core.dto.*;
import com.yjailbir.core.entity.PaymentEntity;
import com.yjailbir.core.entity.TransactionEntity;
import com.yjailbir.core.repository.PaymentsRepository;
import com.yjailbir.core.repository.TransactionsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionChainService {
    private final ValidateService validateService;
    private final PaymentsRepository paymentsRepository;
    private final TransactionsRepository transactionsRepository;
    private final TransactionTemplate transactionTemplate;

    /**
     * Только сохранение в БД в короткой транзакции.
     */
    public TransactionEntity saveOnly(TransactionDtoFromBank dto) {
        return transactionTemplate.execute(status -> {
            PaymentEntity chain = paymentsRepository
                    .findByPaymentId(dto.getPaymentId())
                    .orElseGet(() -> paymentsRepository.save(new PaymentEntity(dto.getPaymentId())));

            TransactionEntity step = new TransactionEntity(dto, chain);
            chain.addStep(step);
            paymentsRepository.save(chain);
            return step;
        });
    }

    /**
     * Публичный метод: сначала сохраняем (быстро), потом валидируем без транзакции.
     */
    public ValidationResultDto saveAndValidate(TransactionDtoFromBank dto) {
        TransactionEntity step = saveOnly(dto);
        return validateService.validate(step);
    }

    public List<PaymentDtoForFrontend> getAllPayments() {
        return paymentsRepository.findTop1000O().stream()
                .map(PaymentEntity::toDto).toList();
    }

    public PaymentDtoForFrontend getPaymentById(UUID paymentId) {
        return paymentsRepository.findByPaymentId(paymentId).orElseThrow().toDto();
    }

    public TransactionDtoForFrontend getTransactionById(UUID transactionId) {
        return transactionsRepository.findById(transactionId).orElseThrow().toDto();
    }

    public List<TransactionDtoForFrontend> getAllTransactionsByPaymentId(UUID paymentId) {
        return transactionsRepository.findAllByPaymentIdOrderByTimestampAsc(paymentId).stream()
                .map(TransactionEntity::toDto).toList();
    }
}