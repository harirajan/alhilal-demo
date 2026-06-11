package com.alhilal.saga.orchestrator.service;

import com.alhilal.saga.orchestrator.model.SagaState;
import com.alhilal.saga.orchestrator.model.SagaState.SagaStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class SagaOrchestrator {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final Map<String, SagaState> sagas = new ConcurrentHashMap<>();

    // ─── STEP 1: START SAGA ──────────────────────────────
    public SagaState startSaga(String fromAccount, String toAccount,
                                Double amount, String customerId) throws Exception {
        String sagaId = "ORCH-" + UUID.randomUUID()
            .toString().substring(0, 8).toUpperCase();

        SagaState saga = new SagaState();
        saga.setSagaId(sagaId);
        saga.setFromAccount(fromAccount);
        saga.setToAccount(toAccount);
        saga.setAmount(amount);
        saga.setCustomerId(customerId);
        saga.setStatus(SagaStatus.STARTED);
        sagas.put(sagaId, saga);

        log.info("[ORCHESTRATOR] ▶ Saga started sagaId={} from={} to={} amount={}",
            sagaId, fromAccount, toAccount, amount);

        // Step 1: COMMAND — debit source account
        sendDebitCommand(saga);
        return saga;
    }

    // ─── SEND DEBIT COMMAND ──────────────────────────────
    private void sendDebitCommand(SagaState saga) throws Exception {
        saga.setStatus(SagaStatus.DEBIT_REQUESTED);
        saga.setCurrentStep("DEBIT");

        String command = objectMapper.writeValueAsString(Map.of(
            "sagaId", saga.getSagaId(),
            "accountId", saga.getFromAccount(),
            "amount", saga.getAmount(),
            "commandType", "DEBIT"
        ));

        kafkaTemplate.send("saga.cmd.debit.account",
            saga.getSagaId(), command);
        log.info("[ORCHESTRATOR] → Command: DEBIT {} from {}",
            saga.getAmount(), saga.getFromAccount());
    }

    // ─── STEP 2: RECEIVE DEBIT REPLY ─────────────────────
    @KafkaListener(topics = "saga.reply.debit.success",
                   groupId = "orchestrator-service")
    public void onDebitSuccess(String payload) throws Exception {
        var reply = objectMapper.readValue(payload, Map.class);
        String sagaId = (String) reply.get("sagaId");
        Number newBalance = (Number) reply.get("newBalance");

        SagaState saga = sagas.get(sagaId);
        if (saga == null) return;

        saga.setStatus(SagaStatus.DEBITED);
        log.info("[ORCHESTRATOR] ← Debit SUCCESS sagaId={} newBalance={}",
            sagaId, newBalance);

        // Step 2: COMMAND — credit destination account
        sendCreditCommand(saga);
    }

    // ─── SEND CREDIT COMMAND ─────────────────────────────
    private void sendCreditCommand(SagaState saga) throws Exception {
        saga.setStatus(SagaStatus.CREDIT_REQUESTED);
        saga.setCurrentStep("CREDIT");

        String command = objectMapper.writeValueAsString(Map.of(
            "sagaId", saga.getSagaId(),
            "accountId", saga.getToAccount(),
            "amount", saga.getAmount(),
            "commandType", "CREDIT"
        ));

        kafkaTemplate.send("saga.cmd.credit.account",
            saga.getSagaId(), command);
        log.info("[ORCHESTRATOR] → Command: CREDIT {} to {}",
            saga.getAmount(), saga.getToAccount());
    }

    // ─── STEP 3: RECEIVE CREDIT REPLY ────────────────────
    @KafkaListener(topics = "saga.reply.credit.success",
                   groupId = "orchestrator-service")
    public void onCreditSuccess(String payload) throws Exception {
        var reply = objectMapper.readValue(payload, Map.class);
        String sagaId = (String) reply.get("sagaId");
        Number newBalance = (Number) reply.get("newBalance");

        SagaState saga = sagas.get(sagaId);
        if (saga == null) return;

        saga.setStatus(SagaStatus.COMPLETED);
        log.info("[ORCHESTRATOR] ← Credit SUCCESS sagaId={} newBalance={}",
            sagaId, newBalance);

        // Step 3: COMMAND — send notification
        sendNotificationCommand(saga, true);
        log.info("[ORCHESTRATOR] ✓ SAGA COMPLETED sagaId={}", sagaId);
    }

    // ─── FAILURE: DEBIT FAILED ───────────────────────────
    @KafkaListener(topics = "saga.reply.debit.failed",
                   groupId = "orchestrator-service")
    public void onDebitFailed(String payload) throws Exception {
        var reply = objectMapper.readValue(payload, Map.class);
        String sagaId = (String) reply.get("sagaId");
        String reason = (String) reply.get("reason");

        SagaState saga = sagas.get(sagaId);
        if (saga == null) return;

        saga.setStatus(SagaStatus.FAILED);
        log.info("[ORCHESTRATOR] ← Debit FAILED sagaId={} reason={}",
            sagaId, reason);

        // Compensation: send failure notification
        sendNotificationCommand(saga, false);
        log.info("[ORCHESTRATOR] ✗ SAGA FAILED sagaId={}", sagaId);
    }

    // ─── SEND NOTIFICATION COMMAND ───────────────────────
    private void sendNotificationCommand(SagaState saga,
                                          boolean success) throws Exception {
        String command = objectMapper.writeValueAsString(Map.of(
            "sagaId", saga.getSagaId(),
            "fromAccount", saga.getFromAccount(),
            "toAccount", saga.getToAccount(),
            "amount", saga.getAmount(),
            "success", success
        ));

        kafkaTemplate.send("saga.cmd.send.notification",
            saga.getSagaId(), command);
        log.info("[ORCHESTRATOR] → Command: NOTIFY success={}",  success);
    }

    public Map<String, SagaState> getAllSagas() { return sagas; }
    public SagaState getSaga(String sagaId) { return sagas.get(sagaId); }
}
