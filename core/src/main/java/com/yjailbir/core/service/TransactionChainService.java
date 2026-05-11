package com.yjailbir.core.service;

import com.yjailbir.core.dto.TransactionDto;
import com.yjailbir.core.entity.TransactionChainEntity;
import com.yjailbir.core.repository.TransactionChainRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TransactionChainService {
    private final TransactionChainRepository transactionChainRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public void  save(TransactionDto transactionDto){
        Optional<TransactionChainEntity> chainEntity = transactionChainRepository.findByTransactionId(transactionDto.transactionId());
        if(chainEntity.isPresent()){
            chainEntity.get().addStep(transactionDto);
        } else {
            TransactionChainEntity transactionChainEntity = new TransactionChainEntity(transactionDto.transactionId());
            transactionChainEntity.addStep(transactionDto);
        }

        messagingTemplate.convertAndSend("/topic/transactions",  transactionChainRepository.findByTransactionId(transactionDto.transactionId()).get());
    }
}
