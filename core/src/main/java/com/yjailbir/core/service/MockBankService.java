package com.yjailbir.core.service;

import com.yjailbir.core.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class MockBankService {
    private final TransactionChainService transactionChainService;
    private final SimpMessagingTemplate messagingTemplate;

    private static final List<TransactionType> TYPES = List.of(TransactionType.values());
    private static final List<String> BANKS = List.of("Asakabank", "InFinBank", "Ipak Yuli Bank", "Hamkorbank");

    @Scheduled(fixedRate = 1000)
    public void generateDto() {
        try {
            int a = ThreadLocalRandom.current().nextInt(0, 11);
            UUID paymentUUID = UUID.randomUUID();

            List<TransactionType> shuffledTypes = new ArrayList<>(TYPES);
            List<String> shuffledBanks = new ArrayList<>(BANKS);
            Collections.shuffle(shuffledTypes);
            Collections.shuffle(shuffledBanks);

            TransactionDtoFromBank dto1 = new TransactionDtoFromBank(
                    paymentUUID, UUID.randomUUID(),
                    shuffledTypes.get(0), shuffledBanks.get(0), shuffledBanks.get(1),
                    250000L, 245000L, 5000L, 2, 0, "HALF_UP", true,
                    LocalDateTime.now(), "UZS", "UZS", 1D, "");

            Collections.shuffle(shuffledTypes);
            Collections.shuffle(shuffledBanks);

            TransactionDtoFromBank dto2 = new TransactionDtoFromBank(
                    paymentUUID, UUID.randomUUID(),
                    shuffledTypes.get(0), dto1.getTo(), shuffledBanks.get(0),
                    245000L, 240000L, 5000L, 0, 5000, "HALF_UP", true,
                    LocalDateTime.now().plusSeconds(123), "UZS", "UZS", 1D, "");

            int successCount = 2;
            int failureCount = 0;
            int warningCount = 0;

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
                List<String> banksCopy = new ArrayList<>(BANKS);
                Collections.shuffle(banksCopy);
                dto2.setFrom(banksCopy.get(0));
                warningCount++;
                successCount--;
            }

            ValidationResultDto res1 = transactionChainService.saveAndValidate(dto1);
            ValidationResultDto res2 = transactionChainService.saveAndValidate(dto2);

            OneTransactionComment comment1 = new OneTransactionComment(
                    dto1.getTransactionId(), res1.errors(), res1.warnings());
            OneTransactionComment comment2 = new OneTransactionComment(
                    dto2.getTransactionId(), res2.errors(), res2.warnings());

            DtoForWebSocket payload = new DtoForWebSocket(successCount, warningCount, failureCount,
                    List.of(comment1, comment2));

            // Отправляем неблокирующе – если RabbitMQ притормозит, планировщик не встанет
            CompletableFuture.runAsync(() -> messagingTemplate.convertAndSend("/topic/transactions", payload));

        } catch (Exception e) {
            // Логируйте ошибку! Иначе если что-то упало, вы не узнаете.
            // logger.error("Ошибка генерации mock-транзакции", e);
        }
    }
}