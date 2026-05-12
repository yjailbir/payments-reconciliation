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
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class ValidateService {
    private final PaymentsRepository paymentsRepository;
    private final TransactionsRepository transactionsRepository;

    public ValidationResultDto validate(TransactionEntity entity) {
        Optional<PaymentEntity> paymentEntity = paymentsRepository.findByPaymentId(entity.getPaymentId());
        boolean failure = false;
        boolean warning = false;
        if (paymentEntity.isPresent()) {
            List<TransactionEntity> transactionEntities = transactionsRepository.findAllByPaymentIdOrderByTimestampDesc(entity.getPaymentId());
            List<String> result = new ArrayList<>();
            //Если в цепочке одна транзакция
            if (transactionEntities.size() == 1) {
                TransactionEntity transactionEntity = transactionEntities.getFirst();
                //Если это проверямая транзакция
                if (transactionEntity.getPaymentId().equals(entity.getPaymentId()) && transactionEntity.getStatus().equals(TransactionStatus.PENDING)) {
                    //Валидируем её
                    result.addAll(validateInnerTransactionData(entity));

                    if (!result.isEmpty()) {
                        entity.setStatus(TransactionStatus.FAILURE);
                        entity.setErrorComments(String.join(", ", result));
                        failure = true;
                    }
                } else {
                    failure = true;
                    entity.setStatus(TransactionStatus.FAILURE);
                    result.add("Дубликат транзакции!");
                }
            } else {
                //Транзакций несколько, надо проверять соседей
                //Проверяем текущую и предыдущую
                TransactionEntity previousTransaction = null;
                for (int i = 0; i < transactionEntities.size(); i++) {
                    if (transactionEntities.get(i).getId().equals(entity.getId())) {
                        if (i > 0) {
                            previousTransaction = transactionEntities.get(i - 1);
                        }
                        break;  // выходим из цикла, как только нашли
                    }
                }
                if (previousTransaction != null) {
                    List<String> neighboringErrors = validateNeighboringTransactions(previousTransaction, entity);
                    List<String> innerErrors = validateInnerTransactionData(entity);


                    if (previousTransaction.getStatus().equals(TransactionStatus.FAILURE)) {
                        warning = true;
                        neighboringErrors.add("Требуется внимание. В одной из предыдущих транзакций обнаружена ошибка!");
                    }

                    if (!neighboringErrors.isEmpty()) {
                        entity.setStatus(TransactionStatus.WARNING);
                        entity.setErrorComments(String.join(", ", neighboringErrors));
                        warning = true;
                    }
                    if (!innerErrors.isEmpty()) {
                        entity.setStatus(TransactionStatus.FAILURE);
                        entity.setErrorComments(String.join(", ", innerErrors));
                        failure = true;
                    }

                    result.addAll(neighboringErrors);
                    result.addAll(innerErrors);
                }
            }

            if (failure) {
                entity.setStatus(TransactionStatus.FAILURE);
                transactionsRepository.save(entity);
                return new ValidationResultDto(TransactionStatus.FAILURE.getDescription(), entity.getTimestamp(), result);
            } else if (warning) {
                entity.setStatus(TransactionStatus.WARNING);
                transactionsRepository.save(entity);
                return new ValidationResultDto(TransactionStatus.WARNING.getDescription(), entity.getTimestamp(), result);
            } else {
                entity.setStatus(TransactionStatus.SUCCESS);
                transactionsRepository.save(entity);
                return new ValidationResultDto(TransactionStatus.SUCCESS.getDescription(), entity.getTimestamp(), result);
            }
        } else {
            //Иначе такой транзакции нет (по идее эта ветка никогда не сработает)
            return new ValidationResultDto(TransactionStatus.NOT_FOUND.getDescription(), entity.getTimestamp(), List.of());
        }
    }

    private List<String> validateNeighboringTransactions(TransactionEntity firstTransaction, TransactionEntity secondTransaction) {
        List<String> result = new ArrayList<>();

        if (secondTransaction.getNotCountedHistory().isEmpty()) {
            if (!firstTransaction.getReceiver().equals(secondTransaction.getReceiver())) {
                result.add("Требуется внимание! Не совпадают получатель и отправитель! Возможно предыдущая транзакция ещё не обработана");
            }
            if (!firstTransaction.getSumOut().equals(secondTransaction.getSumIn())) {
                result.add(String.format(
                        "Не совпадают отправленная и полученная суммы! Ожидаемая сумма: %s, полученная: %s",
                        secondTransaction.getSumIn(),
                        firstTransaction.getSumOut()
                ));
            }
        } else {
            BigDecimal expectedValue = BigDecimal.valueOf(firstTransaction.getSumOut(), 2);
            List<String> banks = List.of(secondTransaction.getNotCountedHistory().split(", "));
            for (String bank : banks) {
                expectedValue = applyPercentOnly(expectedValue, getMockedPercent(bank), RoundingMode.HALF_EVEN);
            }

            if (expectedValue.compareTo(BigDecimal.valueOf(secondTransaction.getSumIn(), 2)) != 0) {
                result.add(String.format(
                        "Требуется проверка! Предварительно не сходится значение суммы! После прохождения транзакций по банкам вне системы предварительно ожидалась сумма платежа %s, но получено: %s",
                        expectedValue,
                        secondTransaction.getSumIn()
                ));
            }
        }

        return result;
    }

    private Integer getMockedPercent(String bankName) {
        return ThreadLocalRandom.current().nextInt(1, 21);
    }

    private List<String> validateInnerTransactionData(TransactionEntity entity) {
        List<String> comments = new ArrayList<>();

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
        }

        long sumInL = sumInObj;
        long sumOutL = sumOutObj;
        int perc = (commissionPercents != null) ? commissionPercents : 0;
        long fixed = (fixedCommissionObj != null) ? fixedCommissionObj : 0L;
        long statedComm = (statedCommissionObj != null) ? statedCommissionObj : 0L;
        boolean percentsFirst = (percentsFirstObj != null) ? percentsFirstObj : true;

        if (sumInL < 0 || sumOutL < 0 || perc < 0 || fixed < 0 || statedComm < 0) {
            comments.add("Обнаружены отрицательные значения сумм/комиссий");
        }

        // 2. Режим округления (общий и для конвертации, и для комиссий)
        RoundingMode rm = null;
        if (roundingModeStr == null || roundingModeStr.isBlank()) {
            comments.add("Не указан режим округления");
        }
        try {
            rm = RoundingMode.valueOf(roundingModeStr);
        } catch (IllegalArgumentException e) {
            comments.add("Некорректный режим округления: " + roundingModeStr);
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
        }
        return comments;
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
