package com.yjailbir.core.service;

import com.yjailbir.core.dto.TransactionDtoFromBank;
import com.yjailbir.core.dto.TransactionStatus;
import com.yjailbir.core.dto.ValidationResultDto;
import com.yjailbir.core.entity.PaymentEntity;
import com.yjailbir.core.entity.TransactionEntity;
import com.yjailbir.core.repository.PaymentsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
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
        ValidationResultDto result = validateService.validate(step);

        messagingTemplate.convertAndSend("/topic/transactions", result);
    }

    @Scheduled(fixedDelay = 15000)
    public void sendMock() {
        int a = ThreadLocalRandom.current().nextInt();
        TransactionStatus status;
        if (a % 2 == 0) {
            status = TransactionStatus.FAILURE;
        } else {
            status = TransactionStatus.SUCCESS;
        }

        ValidationResultDto dto = new ValidationResultDto(status.getDescription(), LocalDateTime.now(), List.of());
        messagingTemplate.convertAndSend("/topic/transactions", dto);
        System.out.println("SEND MOCK");
    }
}