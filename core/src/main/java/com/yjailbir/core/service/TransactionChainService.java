package com.yjailbir.core.service;

import com.yjailbir.core.dto.DtoForFrontend;
import com.yjailbir.core.dto.OneTransactionComment;
import com.yjailbir.core.dto.TransactionDtoFromBank;
import com.yjailbir.core.entity.PaymentEntity;
import com.yjailbir.core.entity.TransactionEntity;
import com.yjailbir.core.repository.PaymentsRepository;
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
    private final SimpMessagingTemplate messagingTemplate;


    @Transactional
    public void saveAndValidate(TransactionDtoFromBank dto) {
        PaymentEntity chain = paymentsRepository
                .findByPaymentId(dto.paymentId())
                .orElseGet(() -> {
                    PaymentEntity newChain = new PaymentEntity(dto.paymentId());
                    return paymentsRepository.save(newChain);
                });

        TransactionEntity step = new TransactionEntity(dto, chain);
        chain.addStep(step);
        paymentsRepository.save(chain);
        //ValidationResultDto result = validateService.validate(step);

       // messagingTemplate.convertAndSend("/topic/transactions", result);
    }

    @Scheduled(fixedDelay = 15000)
    public void sendMock() {
        Integer success = ThreadLocalRandom.current().nextInt(0, 1000);
        Integer failure = ThreadLocalRandom.current().nextInt(0, 1000);
        Integer warning = ThreadLocalRandom.current().nextInt(0, 1000);

        List<OneTransactionComment> comments = new ArrayList<>();

        for (int i = 1; i <= failure; i++) {
            List<String> warnings = List.of("Warning text", "Another Warning text");
            List<String> errors = List.of("Error text, Another Error text");
            comments.add(new OneTransactionComment(
                    UUID.randomUUID(),
                    errors,
                    warnings
            ));
        }

        messagingTemplate.convertAndSend("/topic/transactions", new DtoForFrontend(success, warning, failure, comments));
        System.out.println("SEND MOCK");
    }
}