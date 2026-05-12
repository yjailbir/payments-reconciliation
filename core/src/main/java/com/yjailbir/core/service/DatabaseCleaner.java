package com.yjailbir.core.service;

import com.yjailbir.core.repository.PaymentsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DatabaseCleaner {
    private final PaymentsRepository paymentsRepository;
    @Scheduled(fixedRate = 10000000)
    public void clean() {
        paymentsRepository.deleteAll();
    }
}
