package com.yjailbir.core.service;

import com.yjailbir.core.dto.TransactionDto;
import com.yjailbir.core.entity.TransactionChainEntity;
import com.yjailbir.core.entity.TransactionEntity;
import com.yjailbir.core.repository.TransactionChainRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TransactionChainService {
    private final TransactionChainRepository transactionChainRepository;
   private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public void save(TransactionDto dto) {
        TransactionChainEntity chain = transactionChainRepository
                .findByTransactionId(dto.transactionId())
                .orElseGet(() -> {
                    TransactionChainEntity newChain = new TransactionChainEntity(dto.transactionId());
                    // сразу сохраняем, чтобы можно было добавить шаги
                    return transactionChainRepository.save(newChain);
                });

        TransactionEntity step = new TransactionEntity(dto, chain);
        chain.addStep(step);
        TransactionChainEntity newChain = transactionChainRepository.save(chain);
        System.out.println("added: " + newChain);

        // Валидация (замокана)

       messagingTemplate.convertAndSend("/topic/transactions", newChain);
    }
}