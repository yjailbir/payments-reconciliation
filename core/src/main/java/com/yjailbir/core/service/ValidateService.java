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

        // 1. Извлечение основных полей
        Long sumInObj = entity.getSumIn();
        Long sumOutObj = entity.getSumOut();
        Integer commissionPercents = entity.getCommissionPercents();
        Integer fixedCommissionObj = entity.getFixedCommission();
        Long statedCommissionObj = entity.getCommissionValue();
        String roundingModeStr = entity.getRoundingMode();
        Boolean percentsFirstObj = entity.getPercentsFirst();
        String fromCurrency = entity.getFromCurrency();
        String toCurrency = entity.getToCurrency();
        Double multiplier = entity.getMultiplier();

        // Критические поля
        if (sumInObj == null || sumOutObj == null) {
            comments.add("Сумма входа/выхода не задана");
            isOk = false;
        }

        long sumInL = sumInObj;
        long sumOutL = sumOutObj;
        int perc = (commissionPercents != null) ? commissionPercents : 0;
        long fixed = (fixedCommissionObj != null) ? fixedCommissionObj : 0L;
        long statedComm = (statedCommissionObj != null) ? statedCommissionObj : 0L;
        boolean percentsFirst = (percentsFirstObj != null) ? percentsFirstObj : true;

        if (sumInL < 0 || sumOutL < 0 || perc < 0 || fixed < 0 || statedComm < 0) {
            comments.add("Обнаружены отрицательные значения сумм/комиссий");
            isOk = false;
        }

        // 2. Режим округления (общий и для конвертации, и для комиссий)
        RoundingMode rm = null;
        if (roundingModeStr == null || roundingModeStr.isBlank()) {
            comments.add("Не указан режим округления");
            isOk = false;
        }
        try {
            rm = RoundingMode.valueOf(roundingModeStr);
        } catch (IllegalArgumentException e) {
            comments.add("Некорректный режим округления: " + roundingModeStr);
            isOk = false;
        }

        final int SCALE = 2;
        BigDecimal sumIn = BigDecimal.valueOf(sumInL, SCALE);
        BigDecimal sumOut = BigDecimal.valueOf(sumOutL, SCALE);
        BigDecimal statedCommission = BigDecimal.valueOf(statedComm, SCALE);
        BigDecimal fixedFee = BigDecimal.valueOf(fixed, SCALE);

        // 3. Определение необходимости конвертации
        boolean conversionNeeded = isNotBlank(fromCurrency) && isNotBlank(toCurrency)
                && !fromCurrency.equals(toCurrency);

        if (conversionNeeded && (multiplier == null || multiplier <= 0.0f)) {
            comments.add("Некорректный множитель конверсии для разных валют: " + multiplier);
            isOk = false;
        }

        // Базовая сумма, от которой будут считаться комиссии
        BigDecimal baseAmount;
        if (!conversionNeeded) {
            baseAmount = sumIn;
        } else {
            baseAmount = convertCurrency(sumIn, multiplier, rm);
        }

        // 4. Вычисление ожидаемой выходной суммы (в валюте получателя)
        BigDecimal expectedSumOut = calculateExpectedSumOut(baseAmount, perc, fixedFee, percentsFirst, rm);

        // 5. Сравнение выходных сумм
        if (expectedSumOut.compareTo(sumOut) != 0) {
            comments.add(String.format(
                    "Банком %s неверно рассчитана итоговая сумма (ожидалось: %s %s, получено: %s)",
                    entity.getReceiver(),
                    expectedSumOut.toPlainString(),
                    conversionNeeded ? toCurrency : fromCurrency,
                    sumOut.toPlainString()));
            isOk = false;
        }

        // 6. Проверка заявленной комиссии
        BigDecimal actualCommission = baseAmount.subtract(sumOut);
        if (actualCommission.compareTo(statedCommission) != 0) {
            comments.add(String.format(
                    "Банком %s неверно указано значение комиссии (ожидалось: %s %s, получено: %s)",
                    entity.getReceiver(),
                    actualCommission.toPlainString(),
                    conversionNeeded ? toCurrency : fromCurrency,
                    statedCommission.toPlainString()));
            isOk = false;
        }
        if (isOk) {
            return new ValidationResultDto(TransactionStatus.SUCCESS.getDescription(),
                    entity.getTimestamp(), comments);
        } else {
            return new ValidationResultDto(TransactionStatus.FAILURE.getDescription(),
                    entity.getTimestamp(), comments);
        }
    }

// ------------------- Хелперы -------------------

    /**
     * Конвертация суммы в валюту получателя с округлением до минимальных единиц (копеек).
     */
    private BigDecimal convertCurrency(BigDecimal amount, Double multiplier, RoundingMode rm) {
        // Используем точное представление float через строку, чтобы избежать двойного преобразования
        BigDecimal mult = new BigDecimal(Double.toString(multiplier));
        return amount.multiply(mult).setScale(2, rm);
    }

    /**
     * Вычисление суммы после всех комиссий.
     */
    private BigDecimal calculateExpectedSumOut(BigDecimal amount, int percent,
                                               BigDecimal fixedFee, boolean percentFirst,
                                               RoundingMode rm) {
        if (percent == 0 && fixedFee.compareTo(BigDecimal.ZERO) == 0) {
            return amount;
        }
        if (percent != 0 && fixedFee.compareTo(BigDecimal.ZERO) == 0) {
            return applyPercentOnly(amount, percent, rm);
        }
        if (percent == 0 && fixedFee.compareTo(BigDecimal.ZERO) != 0) {
            return applyFixedOnly(amount, fixedFee);
        }
        return applyBothCommissions(amount, percent, fixedFee, percentFirst, rm);
    }

    private BigDecimal applyPercentOnly(BigDecimal amount, int percent, RoundingMode rm) {
        BigDecimal rate = BigDecimal.valueOf(percent)
                .divide(BigDecimal.valueOf(100), 10, RoundingMode.UNNECESSARY);
        BigDecimal commission = amount.multiply(rate).setScale(2, rm);
        return amount.subtract(commission);
    }

    private BigDecimal applyFixedOnly(BigDecimal amount, BigDecimal fixedFee) {
        return amount.subtract(fixedFee);
    }

    private BigDecimal applyBothCommissions(BigDecimal amount, int percent,
                                            BigDecimal fixedFee, boolean percentFirst,
                                            RoundingMode rm) {
        BigDecimal rate = BigDecimal.valueOf(percent)
                .divide(BigDecimal.valueOf(100), 10, RoundingMode.UNNECESSARY);
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

    private boolean isNotBlank(String str) {
        return str != null && !str.isBlank();
    }
}
