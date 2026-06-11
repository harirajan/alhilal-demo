package com.alhilal.saga.account.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountSagaService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    // Mock account balances
    private final Map<String, Double> balances = new ConcurrentHashMap<>(Map.of(
        "ACC-001", 5000.0,
        "ACC-002", 1000.0,
        "ACC-003", 100.0    // low balance — for testing failure
    ));

    // ─── LISTEN: PAYMENT INITIATED ───────────────────────
    @KafkaListener(topics = "saga.payment.initiated",
                   groupId = "account-service")
    public void onPaymentInitiated(String payload) throws Exception {

        var event = objectMapper.readValue(payload, Map.class);
        String sagaId = (String) event.get("sagaId");
        String fromAccount = (String) event.get("fromAccount");
        String toAccount = (String) event.get("toAccount");
        Double amount = ((Number) event.get("amount")).doubleValue();

        log.info("[ACCOUNT-SAGA] Received PaymentInitiated sagaId={} " +
                 "from={} to={} amount={}", sagaId, fromAccount, toAccount, amount);

        Double currentBalance = balances.getOrDefault(fromAccount, 0.0);

        if (currentBalance >= amount) {
            // ── DEBIT SOURCE ACCOUNT ──
            balances.put(fromAccount, currentBalance - amount);
            Double newBalance = balances.get(fromAccount);

            log.info("[ACCOUNT-SAGA] Debited {} from {} new balance={}",
                amount, fromAccount, newBalance);

            // Publish success event
            String debitedEvent = objectMapper.writeValueAsString(Map.of(
                "sagaId", sagaId,
                "accountId", fromAccount,
                "amount", amount,
                "newBalance", newBalance
            ));
            kafkaTemplate.send("saga.account.debited", sagaId, debitedEvent);
            log.info("[ACCOUNT-SAGA] Published AccountDebitedEvent sagaId={}", sagaId);

            // ── CREDIT DESTINATION ACCOUNT ──
            Double destBalance = balances.getOrDefault(toAccount, 0.0);
            balances.put(toAccount, destBalance + amount);
            log.info("[ACCOUNT-SAGA] Credited {} to {} new balance={}",
                amount, toAccount, balances.get(toAccount));

            // Publish credited event
            String creditedEvent = objectMapper.writeValueAsString(Map.of(
                "sagaId", sagaId,
                "accountId", toAccount,
                "amount", amount,
                "newBalance", balances.get(toAccount)
            ));
            kafkaTemplate.send("saga.account.credited", sagaId, creditedEvent);
            log.info("[ACCOUNT-SAGA] Published AccountCreditedEvent sagaId={}", sagaId);

        } else {
            // ── INSUFFICIENT FUNDS ──
            log.warn("[ACCOUNT-SAGA] Insufficient funds sagaId={} " +
                     "balance={} required={}", sagaId, currentBalance, amount);

            String failedEvent = objectMapper.writeValueAsString(Map.of(
                "sagaId", sagaId,
                "accountId", fromAccount,
                "reason", "INSUFFICIENT_FUNDS",
                "currentBalance", currentBalance,
                "requiredAmount", amount
            ));
            kafkaTemplate.send("saga.account.debit.failed", sagaId, failedEvent);
            log.info("[ACCOUNT-SAGA] Published AccountDebitFailedEvent sagaId={}", sagaId);
        }
    }

    public Map<String, Double> getAllBalances() {
        return balances;
    }
}
