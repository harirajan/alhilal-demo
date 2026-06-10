package com.alhilal.account.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ================================================================
 * ACCOUNT EVENT PUBLISHER
 * ================================================================
 * Publishes account domain events to Kafka.
 * Topic: banking.accounts
 * ================================================================
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AccountEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public static final String TOPIC_ACCOUNTS = "banking.accounts";

    public void publishAccountOpened(Account account) {
        publish(account.getAccountId(), new AccountOpenedEvent(
                account.getAccountId(),
                account.getCustomerId(),
                account.getAccountNumber(),
                account.getAccountType().name(),
                account.getCurrency(),
                LocalDateTime.now()
        ));
        log.info("[KAFKA] Published AccountOpenedEvent: {}", account.getAccountId());
    }

    public void publishMoneyDeposited(Account account, BigDecimal amount, String description) {
        publish(account.getAccountId(), new MoneyDepositedEvent(
                account.getAccountId(),
                account.getCustomerId(),
                amount,
                account.getBalance(),
                description,
                LocalDateTime.now()
        ));
        log.info("[KAFKA] Published MoneyDepositedEvent: {} amount: {}", account.getAccountId(), amount);
    }

    public void publishMoneyWithdrawn(Account account, BigDecimal amount, String description) {
        publish(account.getAccountId(), new MoneyWithdrawnEvent(
                account.getAccountId(),
                account.getCustomerId(),
                amount,
                account.getBalance(),
                description,
                LocalDateTime.now()
        ));
        log.info("[KAFKA] Published MoneyWithdrawnEvent: {} amount: {}", account.getAccountId(), amount);
    }

    private void publish(String key, Object event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(TOPIC_ACCOUNTS, key, payload);
        } catch (Exception e) {
            log.error("[KAFKA] Failed to publish event", e);
        }
    }
}

/**
 * ================================================================
 * ACCOUNT EVENT CONSUMER
 * ================================================================
 * Listens to events from OTHER services.
 *
 * Listens to: banking.customers topic
 * Why? When a customer is KYC verified, we log it.
 * In a fuller system: you might auto-open a default account.
 *
 * THIS IS THE KEY PATTERN:
 * customer-service publishes CustomerVerifiedEvent
 * account-service LISTENS and reacts
 * They never called each other directly
 * = Loose coupling via Kafka
 * ================================================================
 */
@Component
@Slf4j
class AccountEventConsumer {

    @KafkaListener(
        topics = "banking.customers",
        groupId = "account-service",
            autoStartup = "${kafka.consumer.auto-startup:true}"// unique consumer group
    )
    public void onCustomerEvent(String payload) {
        log.info("[KAFKA CONSUMER] account-service received customer event: {}",
                payload.substring(0, Math.min(100, payload.length())));

        // In a real system: parse the event type and react
        // If CustomerVerifiedEvent → you could auto-create a savings account
        // For this demo: just logging to show the event flow works
        if (payload.contains("CUSTOMER_VERIFIED")) {
            log.info("[KAFKA CONSUMER] Customer verified! account-service could auto-open account here.");
        }
    }
}

// ----------------------------------------------------------------
// EVENT RECORDS
// ----------------------------------------------------------------
record AccountOpenedEvent(
        String accountId, String customerId,
        String accountNumber, String accountType,
        String currency, LocalDateTime occurredAt) {}

record MoneyDepositedEvent(
        String accountId, String customerId,
        BigDecimal amount, BigDecimal balanceAfter,
        String description, LocalDateTime occurredAt) {}

record MoneyWithdrawnEvent(
        String accountId, String customerId,
        BigDecimal amount, BigDecimal balanceAfter,
        String description, LocalDateTime occurredAt) {}
