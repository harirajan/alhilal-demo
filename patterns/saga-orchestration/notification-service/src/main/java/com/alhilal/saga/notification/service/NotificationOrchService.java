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
public class NotificationOrchService {

    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "saga.cmd.send.notification",
                   groupId = "orch-notification-service")
    public void onNotificationCommand(String payload) throws Exception {
        var cmd = objectMapper.readValue(payload, Map.class);
        String sagaId     = (String) cmd.get("sagaId");
        String fromAccount = (String) cmd.get("fromAccount");
        String toAccount   = (String) cmd.get("toAccount");
        Number amount      = (Number) cmd.get("amount");
        Boolean success    = (Boolean) cmd.get("success");

        if (success) {
            log.info("[ORCH-NOTIFICATION] 📱 SMS → 'Transfer SUCCESS: " +
                     "AED {} from {} to {}' sagaId={}",
                     amount, fromAccount, toAccount, sagaId);
        } else {
            log.info("[ORCH-NOTIFICATION] 📱 SMS → 'Transfer FAILED: " +
                     "AED {} from {} - Insufficient funds' sagaId={}",
                     amount, fromAccount, sagaId);
        }
    }
}
