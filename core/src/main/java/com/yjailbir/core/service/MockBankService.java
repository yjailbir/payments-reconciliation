package com.yjailbir.core.service;

import com.yjailbir.core.dto.TransactionDtoFromBank;
import com.yjailbir.core.dto.TransactionType;
import com.yjailbir.core.dto.ValidationResultDto;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class MockBankService {
    private final TransactionChainService transactionChainService;
    List<TransactionType> types =  new ArrayList<>(List.of(TransactionType.values()));
    List<String> banks = new ArrayList<>(List.of("Asakabank", "InFinBank", " Ipak Yuli Bank", "Hamkorbank"));

    @Scheduled(fixedRate = 1000)
    private void generateDto() throws InterruptedException {
        Integer a = ThreadLocalRandom.current().nextInt(0, 11);
        boolean isRight;
        if (a == 6 || a == 7 || a == 8) {
            isRight = true;
        } else {
            isRight = false;
        }

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
                dto1.to(),
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

        ValidationResultDto res1= transactionChainService.saveAndValidate(dto1);
        Thread.sleep(1000);
        transactionChainService.saveAndValidate(dto2);
    }
}
