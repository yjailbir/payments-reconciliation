package com.yjailbir.core.service;

import com.yjailbir.core.dto.DtoForFrontend;
import com.yjailbir.core.dto.TransactionDtoFromBank;
import com.yjailbir.core.entity.PaymentEntity;
import com.yjailbir.core.entity.TransactionEntity;
import com.yjailbir.core.repository.PaymentsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
                .findByTransactionId(dto.paymentId())
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
        messagingTemplate.convertAndSend("/topic/transactions", new DtoForFrontend(ThreadLocalRandom.current().nextInt(0,1000), ThreadLocalRandom.current().nextInt(0,1000), ThreadLocalRandom.current().nextInt(0,1000)));
        System.out.println("SEND MOCK");
    }
}