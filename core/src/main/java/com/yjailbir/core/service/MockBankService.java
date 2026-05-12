package com.yjailbir.core.service;

import com.yjailbir.core.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Scope("prototype")
public class MockBankService {
    private final TransactionChainService transactionChainService;
    private final SimpMessagingTemplate messagingTemplate;
    List<TransactionType> types =  new ArrayList<>(List.of(TransactionType.values()));
    List<String> banks = new ArrayList<>(List.of("Asakabank", "InFinBank", " Ipak Yuli Bank", "Hamkorbank"));

    @Scheduled(fixedRate = 1000)
    private void generateDto() throws InterruptedException {
        Integer a = ThreadLocalRandom.current().nextInt(0, 11);

        UUID paymentUUID = UUID.randomUUID();
        Collections.shuffle(types);
        Collections.shuffle(banks);

        TransactionDtoFromBank dto1 = new TransactionDtoFromBank(
                paymentUUID,
                UUID.randomUUID(),
                types.getFirst(),
                banks.getFirst(),
                banks.getLast(),
                250000L,
                245000L,
                5000L,
                2,
                0,
                "HALF_UP",
                true,
                LocalDateTime.now(),
                "UZS",
                "UZS",
                1D,
                ""
        );

        Collections.shuffle(types);
        Collections.shuffle(banks);

        TransactionDtoFromBank dto2 = new TransactionDtoFromBank(
                paymentUUID,
                UUID.randomUUID(),
                types.getFirst(),
                dto1.getTo(),
                banks.getFirst(),
                245000L,
                240000L,
                5000L,
                0,
                5000,
                "HALF_UP",
                true,
                LocalDateTime.now().plusSeconds(123),
                "UZS",
                "UZS",
                1D,
                ""
        );

        Integer successCount = 2;
        Integer failureCount = 0;
        Integer warningCount = 0;

        if (a == 4) {
            dto2.setSumOut(22000L);
            failureCount++;
            successCount--;
        }
        if (a == 8) {
            dto1.setCommissionPercents(4);
            failureCount++;
            successCount--;
        }
        if (a == 10) {
            Collections.shuffle(banks);
            dto2.setFrom(banks.getFirst());
            warningCount++;
            successCount--;
        }

        ValidationResultDto res1 = transactionChainService.saveAndValidate(dto1);
        Thread.sleep(10);
        ValidationResultDto res2 = transactionChainService.saveAndValidate(dto2);

        OneTransactionComment comment1 = new OneTransactionComment(
                dto1.getTransactionId(),
                res1.errors(),
                res1.warnings()
        );
        OneTransactionComment comment2 = new OneTransactionComment(
                dto2.getTransactionId(),
                res2.errors(),
                res2.warnings()
        );

        messagingTemplate.convertAndSend("/topic/transactions", new DtoForWebSocket(successCount, warningCount, failureCount, List.of(comment1, comment2)));
    }
}
