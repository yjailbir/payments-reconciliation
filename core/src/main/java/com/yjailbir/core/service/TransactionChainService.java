package com.yjailbir.core.service;

import com.yjailbir.core.dto.*;
import com.yjailbir.core.entity.PaymentEntity;
import com.yjailbir.core.entity.TransactionEntity;
import com.yjailbir.core.repository.PaymentsRepository;
import com.yjailbir.core.repository.TransactionsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

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
       return paymentsRepository.findByPaymentId(paymentId).get().getSteps().stream().map(TransactionEntity::toDto).toList();
    }

    /*@Scheduled(fixedDelay = 15000)
    public void sendMock() {
        Integer success = ThreadLocalRandom.current().nextInt(0, 1000);
        Integer failure = ThreadLocalRandom.current().nextInt(0, 1000);
        Integer warning = ThreadLocalRandom.current().nextInt(0, 1000);

        List<OneTransactionComment> comments = new ArrayList<>();

        for (int i = 1; i <= failure; i++) {
            List<String> warnings = List.of("Warning text", "Another Warning text");
            List<String> errors = List.of("Error text", "Another Error text");
            comments.add(new OneTransactionComment(
                    UUID.randomUUID(),
                    errors,
                    warnings
            ));
        }

        messagingTemplate.convertAndSend("/topic/transactions", new DtoForWebSocket(success, warning, failure, comments));
        System.out.println("SEND MOCK");
    }*/
}