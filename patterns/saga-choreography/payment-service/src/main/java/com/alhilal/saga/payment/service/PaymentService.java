package com.alhilal.saga.payment.service;

import com.alhilal.saga.payment.events.PaymentInitiatedEvent;
import com.alhilal.saga.payment.model.Payment;
import com.alhilal.saga.payment.model.Payment.PaymentStatus;
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
public class PaymentService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final Map<String, Payment> payments = new ConcurrentHashMap<>();

    // ─── STEP 1: INITIATE SAGA ───────────────────────────
    public Payment initiatePayment(String fromAccount, String toAccount,
                                   Double amount, String customerId) throws Exception {
        String sagaId = "SAGA-" + UUID.randomUUID()
            .toString().substring(0, 8).toUpperCase();

        Payment payment = new Payment();
        payment.setSagaId(sagaId);
        payment.setFromAccount(fromAccount);
        payment.setToAccount(toAccount);
        payment.setAmount(amount);
        payment.setCurrency("AED");
        payment.setCustomerId(customerId);
        payment.setStatus(PaymentStatus.INITIATED);
        payments.put(sagaId, payment);

        log.info("[SAGA-PAYMENT] ▶ Started sagaId={} from={} to={} amount={}",
            sagaId, fromAccount, toAccount, amount);

        PaymentInitiatedEvent event = new PaymentInitiatedEvent(
            sagaId, fromAccount, toAccount, amount, "AED", customerId);

        kafkaTemplate.send("saga.payment.initiated", sagaId,
            objectMapper.writeValueAsString(event));

        log.info("[SAGA-PAYMENT] → Published PaymentInitiatedEvent sagaId={}", sagaId);
        payment.setStatus(PaymentStatus.DEBIT_PENDING);
        return payment;
    }

    // ─── STEP 2: DEBIT CONFIRMED ─────────────────────────
    @KafkaListener(topics = "saga.account.debited",
                   groupId = "payment-service")
    public void onAccountDebited(String payload) throws Exception {
        var event = objectMapper.readValue(payload, Map.class);
        String sagaId = (String) event.get("sagaId");
        Number newBalance = (Number) event.get("newBalance");

        Payment payment = payments.get(sagaId);
        if (payment == null) return;

        payment.setStatus(PaymentStatus.DEBITED);
        log.info("[SAGA-PAYMENT] ← Debit confirmed sagaId={} sourceNewBalance={}",
            sagaId, newBalance);
        // Now waiting for credit confirmation...
    }

    // ─── STEP 3: CREDIT CONFIRMED → SAGA COMPLETE ────────
    @KafkaListener(topics = "saga.account.credited",
                   groupId = "payment-service")
    public void onAccountCredited(String payload) throws Exception {
        var event = objectMapper.readValue(payload, Map.class);
        String sagaId = (String) event.get("sagaId");
        String accountId = (String) event.get("accountId");
        Number newBalance = (Number) event.get("newBalance");

        Payment payment = payments.get(sagaId);
        if (payment == null) return;

        payment.setStatus(PaymentStatus.COMPLETED);
        log.info("[SAGA-PAYMENT] ✓ SAGA COMPLETED sagaId={} " +
                 "credited={} newBalance={}",
                 sagaId, accountId, newBalance);
    }

    // ─── FAILURE: DEBIT FAILED → SAGA FAILED ─────────────
    @KafkaListener(topics = "saga.account.debit.failed",
                   groupId = "payment-service")
    public void onDebitFailed(String payload) throws Exception {
        var event = objectMapper.readValue(payload, Map.class);
        String sagaId = (String) event.get("sagaId");
        String reason = (String) event.get("reason");

        Payment payment = payments.get(sagaId);
        if (payment == null) return;

        payment.setStatus(PaymentStatus.FAILED);
        log.info("[SAGA-PAYMENT] ✗ SAGA FAILED sagaId={} reason={}",
            sagaId, reason);
    }

    public Map<String, Payment> getAllPayments() { return payments; }
    public Payment getPayment(String sagaId) { return payments.get(sagaId); }
}
