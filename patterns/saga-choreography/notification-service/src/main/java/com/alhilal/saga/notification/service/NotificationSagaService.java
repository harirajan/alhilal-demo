package com.alhilal.saga.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationSagaService {

    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "saga.account.debited",
                   groupId = "notification-service")
    public void onAccountDebited(String payload) throws Exception {
        var event = objectMapper.readValue(payload, Map.class);
        String sagaId = (String) event.get("sagaId");
        String accountId = (String) event.get("accountId");
        Number amount = (Number) event.get("amount");
        Number newBalance = (Number) event.get("newBalance");

        log.info("[NOTIFICATION] ✓ SMS sent: 'AED {} debited from {}. " +
                 "New balance: AED {}' sagaId={}",
                 amount, accountId, newBalance, sagaId);
    }

    @KafkaListener(topics = "saga.account.credited",
                   groupId = "notification-service")
    public void onAccountCredited(String payload) throws Exception {
        var event = objectMapper.readValue(payload, Map.class);
        String sagaId = (String) event.get("sagaId");
        String accountId = (String) event.get("accountId");
        Number amount = (Number) event.get("amount");
        Number newBalance = (Number) event.get("newBalance");

        log.info("[NOTIFICATION] ✓ SMS sent: 'AED {} credited to {}. " +
                 "New balance: AED {}' sagaId={}",
                 amount, accountId, newBalance, sagaId);
    }

    @KafkaListener(topics = "saga.account.debit.failed",
                   groupId = "notification-service")
    public void onDebitFailed(String payload) throws Exception {
        var event = objectMapper.readValue(payload, Map.class);
        String sagaId = (String) event.get("sagaId");
        String reason = (String) event.get("reason");

        log.info("[NOTIFICATION] ✗ SMS sent: 'Payment failed: {}' sagaId={}",
                 reason, sagaId);
    }
}
