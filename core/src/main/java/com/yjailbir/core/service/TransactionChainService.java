package com.yjailbir.core.service;

import com.yjailbir.core.dto.TransactionChainDto;
import com.yjailbir.core.dto.TransactionDto;
import com.yjailbir.core.entity.TransactionChainEntity;
import com.yjailbir.core.repository.TransactionChainRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TransactionChainService {
    @Autowired
    private final TransactionChainRepository transactionChainRepository;

    public void  save(TransactionDto transactionDto){
        Optional<TransactionChainEntity> chainEntity = transactionChainRepository.findByTransactionId(transactionDto.transactionId());
        TransactionChainEntity res = null;
        if(chainEntity.isPresent()){
            chainEntity.get().addStep(transactionDto);
           res = transactionChainRepository.save(chainEntity.get());
        } else {
            TransactionChainEntity transactionChainEntity = new TransactionChainEntity(transactionDto.transactionId());
            transactionChainEntity.addStep(transactionDto);
            res =transactionChainRepository.save(transactionChainEntity);
        }

        System.out.println("saved: " + res.toString());
    }
}
