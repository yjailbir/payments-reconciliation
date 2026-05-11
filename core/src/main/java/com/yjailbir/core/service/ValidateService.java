package com.yjailbir.core.service;

import com.yjailbir.core.dto.TransactionStatus;
import com.yjailbir.core.dto.ValidationResultDto;
import com.yjailbir.core.entity.PaymentEntity;
import com.yjailbir.core.entity.TransactionEntity;
import com.yjailbir.core.repository.PaymentsRepository;
import com.yjailbir.core.repository.TransactionsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
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
            if (transactionEntities.size() == 1) {
                TransactionEntity transactionEntity = transactionEntities.getFirst();
                //Если это проверямая транзакция
                if (transactionEntity.getTransactionId().equals(entity.getTransactionId())) {
                    //Валидируем её
                    ValidationResultDto result = validateInnerTransactionData(entity);
                    if (result.comments().isEmpty()) {
                        transactionEntity.setStatus(TransactionStatus.SUCCESS);
                    } else {
                        transactionEntity.setStatus(TransactionStatus.FAILURE);
                    }
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

        Long sumInObj = entity.getSumIn();
        Long sumOutObj = entity.getSumOut();
        Integer commissionPercents = entity.getCommissionPercents();
        Integer fixedCommissionObj = entity.getFixedCommission();
        Long statedCommissionObj = entity.getCommissionValue();
        String roundingModeStr = entity.getRoundingMode();
        Boolean percentsFirstObj = entity.getPercentsFirst();

        if (sumInObj == null || sumOutObj == null) {
            comments.add("Сумма до/после платежа не задана");
            return new ValidationResultDto(TransactionStatus.FAILURE.getDescription(), entity.getTimestamp(), comments);
        }

        long sumInL = sumInObj;
        long sumOutL = sumOutObj;
        int perc = (commissionPercents != null) ? commissionPercents : 0;
        long fixed = (fixedCommissionObj != null) ? fixedCommissionObj : 0L;
        long statedComm = (statedCommissionObj != null) ? statedCommissionObj : 0L;
        boolean percentsFirst = (percentsFirstObj != null) ? percentsFirstObj : true;

        if (sumInL < 0 || sumOutL < 0 || perc < 0 || fixed < 0 || statedComm < 0) {
            comments.add("Обнаружены отрицательные значения сумм/комиссий");
            return new ValidationResultDto(TransactionStatus.FAILURE.getDescription(), entity.getTimestamp(), comments);
        }

        RoundingMode rm;
        try {
            rm = RoundingMode.valueOf(roundingModeStr);
        } catch (Exception e) {
            comments.add("Некорректный режим округления: " + roundingModeStr);
            return new ValidationResultDto(TransactionStatus.FAILURE.getDescription(), entity.getTimestamp(), comments);
        }

        final int SCALE = 2;

        BigDecimal sumIn = BigDecimal.valueOf(sumInL, SCALE);
        BigDecimal sumOut = BigDecimal.valueOf(sumOutL, SCALE);
        BigDecimal statedCommission = BigDecimal.valueOf(statedComm, SCALE);
        BigDecimal fixedFee = BigDecimal.valueOf(fixed, SCALE);

        // 2. Вычисление ожидаемой выходной суммы
        BigDecimal expectedSum;
        if (perc == 0 && fixed == 0) {
            expectedSum = sumIn;
        } else if (perc != 0 && fixed == 0) {
            expectedSum = applyPercentOnly(sumIn, perc, rm);
        } else if (perc == 0 && fixed != 0) {
            expectedSum = applyFixedOnly(sumIn, fixedFee);
        } else { // perc != 0 && fixed != 0
            expectedSum = applyBothCommissions(sumIn, perc, fixedFee, percentsFirst, rm);
        }

        // 3. Сравнение выходных сумм
        if (expectedSum.compareTo(sumOut) != 0) {
            comments.add(String.format("Банком %s неверно вычтена комиссия! Ожидаемый итог: %s, фактический итог: %s",
                    entity.getReceiver(), expectedSum.toPlainString(), sumOut.toPlainString()));
            isOk = false;
        }

        // 4. Сравнение заявленной комиссии с фактической
        BigDecimal actualCommission = sumIn.subtract(sumOut);
        if (actualCommission.compareTo(statedCommission) != 0) {
            comments.add(String.format("Банком %s неверно указано значение комиссии! Ожидаемая: %s, фактическая: %s",
                    entity.getReceiver(), actualCommission.toPlainString(), statedCommission.toPlainString()));
            isOk = false;
        }

        if (isOk) {
            return new ValidationResultDto(TransactionStatus.SUCCESS.getDescription(), entity.getTimestamp(), comments);
        } else {
            return new ValidationResultDto(TransactionStatus.FAILURE.getDescription(), entity.getTimestamp(), comments);
        }
    }

// ------------------- Хелперы -------------------

    /**
     * Только процентная комиссия (фиксированная отсутствует).
     *
     * @param amount  исходная сумма (BigDecimal с масштабом 2)
     * @param percent целое число процентов (например, 12 означает 12%)
     * @param rm      режим округления для комиссии
     * @return сумма после вычета процента
     */
    private BigDecimal applyPercentOnly(BigDecimal amount, int percent, RoundingMode rm) {
        BigDecimal rate = BigDecimal.valueOf(percent)
                .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);
        BigDecimal commission = amount.multiply(rate).setScale(2, rm);
        return amount.subtract(commission);
    }

    /**
     * Только фиксированная комиссия.
     */
    private BigDecimal applyFixedOnly(BigDecimal amount, BigDecimal fixedFee) {
        return amount.subtract(fixedFee);
    }

    /**
     * Смешанная комиссия: фиксированная + процентная.
     *
     * @param percentFirst true – сначала процент, потом фикс; false – сначала фикс, потом процент
     */
    private BigDecimal applyBothCommissions(
            BigDecimal amount,
            int percent,
            BigDecimal fixedFee,
            boolean percentFirst,
            RoundingMode rm
    ) {
        BigDecimal rate = BigDecimal.valueOf(percent)
                .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);

        if (percentFirst) {
            BigDecimal percentCommission = amount.multiply(rate).setScale(2, rm);
            BigDecimal afterPercent = amount.subtract(percentCommission);
            return afterPercent.subtract(fixedFee);
        } else {
            BigDecimal afterFixed = amount.subtract(fixedFee);
            BigDecimal percentCommission = afterFixed.multiply(rate).setScale(2, rm);
            return afterFixed.subtract(percentCommission);
        }
    }
}
