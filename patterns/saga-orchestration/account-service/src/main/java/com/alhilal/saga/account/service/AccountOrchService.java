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
public class AccountOrchService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private final Map<String, Double> balances = new ConcurrentHashMap<>(Map.of(
        "ACC-001", 5000.0,
        "ACC-002", 1000.0,
        "ACC-003", 100.0
    ));

    // ─── LISTEN: DEBIT COMMAND ───────────────────────────
    @KafkaListener(topics = "saga.cmd.debit.account",
                   groupId = "orch-account-service")
    public void onDebitCommand(String payload) throws Exception {
        var cmd = objectMapper.readValue(payload, Map.class);
        String sagaId   = (String) cmd.get("sagaId");
        String accountId = (String) cmd.get("accountId");
        Double amount   = ((Number) cmd.get("amount")).doubleValue();

        log.info("[ORCH-ACCOUNT] ← DEBIT command sagaId={} account={} amount={}",
            sagaId, accountId, amount);

        Double balance = balances.getOrDefault(accountId, 0.0);

        if (balance >= amount) {
            balances.put(accountId, balance - amount);
            log.info("[ORCH-ACCOUNT] ✓ Debited {} from {} newBalance={}",
                amount, accountId, balances.get(accountId));

            // Reply SUCCESS to orchestrator
            kafkaTemplate.send("saga.reply.debit.success", sagaId,
                objectMapper.writeValueAsString(Map.of(
                    "sagaId", sagaId,
                    "accountId", accountId,
                    "amount", amount,
                    "newBalance", balances.get(accountId)
                )));
            log.info("[ORCH-ACCOUNT] → Reply: DEBIT SUCCESS sagaId={}", sagaId);

        } else {
            log.warn("[ORCH-ACCOUNT] ✗ Insufficient funds sagaId={} balance={} required={}",
                sagaId, balance, amount);

            // Reply FAILED to orchestrator
            kafkaTemplate.send("saga.reply.debit.failed", sagaId,
                objectMapper.writeValueAsString(Map.of(
                    "sagaId", sagaId,
                    "accountId", accountId,
                    "reason", "INSUFFICIENT_FUNDS",
                    "currentBalance", balance
                )));
            log.info("[ORCH-ACCOUNT] → Reply: DEBIT FAILED sagaId={}", sagaId);
        }
    }

    // ─── LISTEN: CREDIT COMMAND ──────────────────────────
    @KafkaListener(topics = "saga.cmd.credit.account",
                   groupId = "orch-account-service")
    public void onCreditCommand(String payload) throws Exception {
        var cmd = objectMapper.readValue(payload, Map.class);
        String sagaId    = (String) cmd.get("sagaId");
        String accountId = (String) cmd.get("accountId");
        Double amount    = ((Number) cmd.get("amount")).doubleValue();

        log.info("[ORCH-ACCOUNT] ← CREDIT command sagaId={} account={} amount={}",
            sagaId, accountId, amount);

        balances.merge(accountId, amount, Double::sum);
        log.info("[ORCH-ACCOUNT] ✓ Credited {} to {} newBalance={}",
            amount, accountId, balances.get(accountId));

        // Reply SUCCESS to orchestrator
        kafkaTemplate.send("saga.reply.credit.success", sagaId,
            objectMapper.writeValueAsString(Map.of(
                "sagaId", sagaId,
                "accountId", accountId,
                "amount", amount,
                "newBalance", balances.get(accountId)
            )));
        log.info("[ORCH-ACCOUNT] → Reply: CREDIT SUCCESS sagaId={}", sagaId);
    }

    public Map<String, Double> getBalances() { return balances; }
}
