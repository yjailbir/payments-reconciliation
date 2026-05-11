package com.yjailbir.core.service;

import com.yjailbir.core.dto.TransactionStatus;
import com.yjailbir.core.dto.ValidationResultDto;
import com.yjailbir.core.entity.PaymentEntity;
import com.yjailbir.core.entity.TransactionEntity;
import com.yjailbir.core.repository.PaymentsRepository;
import com.yjailbir.core.repository.TransactionsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ValidateService {
    private final PaymentsRepository paymentsRepository;
    private final TransactionsRepository transactionsRepository;

    public ValidationResultDto validate(TransactionEntity entity) {
        Optional<PaymentEntity> paymentEntity = paymentsRepository.findByTransactionId(entity.getTransactionId());
        if (paymentEntity.isPresent()) {
            List<TransactionEntity> transactionEntities = transactionsRepository.findAllByTransactionIdOrderByTimestampDesc(entity.getTransactionId());
            //Если в цепочке одна транзакция
            if(transactionEntities.size() == 1 ) {
                TransactionEntity transactionEntity = transactionEntities.getFirst();
                //Если это проверямая транзакция
                if (transactionEntity.getTransactionId().equals(entity.getTransactionId())) {
                    //То она успешн
                    transactionEntity.setStatus(TransactionStatus.SUCCESS);
                }
            }
        } else {
            return new ValidationResultDto(TransactionStatus.NOT_FOUND.getDescription(), entity.getTimestamp(), List.of());
        }

        return null;
    }

    private ValidationResultDto validateInnerTransactionData(TransactionEntity entity) {
        List<String> comments = new ArrayList<>();
        boolean isOk = true;
        if (entity.getCommissionPercents() != 0 && entity.getFixedCommission() == 0) {
            //Комиссия только в процентах
            Long actualSum = (entity.getSumIn() * (100 - entity.getCommissionPercents())) / 100;
            if(!actualSum.equals(entity.getSumOut())) {
                comments.add("Банком " + entity.getReceiver() + " неверно вычтена комиссия! Ожидаемый итог: " + actualSum + ", фактический итог: " + entity.getSumOut());
                isOk = false;
            }
            Long actualTax = entity.getSumIn() - entity.getSumOut();
            if(!actualTax.equals(entity.getCommissionValue())) {
                comments.add("Банком " + entity.getReceiver() + " неверно указано значение комиссии! Ожидаемая комиссия: " + actualTax + ", фактическая комиссия: " + entity.getCommissionValue());
                isOk = false;
            }
        } else if (entity.getCommissionPercents() == 0 && entity.getFixedCommission() != 0) {
            //Комиссия только фиксированная
            Long actualSum = entity.getSumIn() - entity.getFixedCommission();
            if(!actualSum.equals(entity.getSumOut())) {
                comments.add("Банком " + entity.getReceiver() + " неверно вычтена комиссия! Ожидаемый итог: " + actualSum + ", фактический итог: " + entity.getSumOut());
                isOk = false;
            }
            Long actualTax = entity.getSumIn() - entity.getSumOut();
            if(!actualTax.equals(entity.getCommissionValue())) {
                comments.add("Банком " + entity.getReceiver() + " неверно указано значение комиссии! Ожидаемая комиссия: " + actualTax + ", фактическая комиссия: " + entity.getCommissionValue());
                isOk = false;
            }
        } else if (entity.getCommissionPercents() != 0 && entity.getFixedCommission() != 0) {
             //Комиссия двойная (мы не знаем, что сначала вычитается - проценты или фиксированное значение. Проверяем оба варианта)
            Long actualSum1 = (entity.getSumIn() * (100 - entity.getCommissionPercents())) / 100 - entity.getFixedCommission();
            Long actualSum2 = ((entity.getSumIn() - entity.getFixedCommission()) * (100 - entity.getCommissionPercents())) / 100;
            if(!actualSum1.equals(entity.getSumOut()) && !actualSum2.equals(entity.getSumOut())) {
                comments.add("Банком " + entity.getReceiver() + " неверно вычтена комиссия! Возможные ожидаемые итоги : " + actualSum1 + " или " + actualSum2 + ", фактический итог: " + entity.getSumOut());
                isOk = false;
            }
            Long actualTax = entity.getSumIn() - entity.getSumOut();
            if(!actualTax.equals(entity.getCommissionValue())) {
                comments.add("Банком " + entity.getReceiver() + " неверно указано значение комиссии! Ожидаемая комиссия: " + actualTax + ", фактическая комиссия: " + entity.getCommissionValue());
                isOk = false;
            }
        }
        //В оставшемся случае комиссии нет и её можно не проверять
    }

}
