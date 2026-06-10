package com.alhilal.customer.events;

import com.alhilal.customer.domain.Customer;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

/**
 * ================================================================
 * KAFKA EVENT PUBLISHER
 * ================================================================
 * Publishes domain events to Kafka topics.
 *
 * Topics:
 *   banking.customers  → all customer events
 *
 * Who listens?
 *   notification-service → sends welcome SMS on CustomerRegistered
 *   account-service      → checks KYC before opening account
 *
 * KEY PRINCIPLE: customer-service does NOT call notification-service
 * or account-service directly. It just publishes an event.
 * What they do with it is their business. Loose coupling.
 * ================================================================
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CustomerEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public static final String TOPIC_CUSTOMERS = "banking.customers";

    public void publishCustomerRegistered(Customer customer) {
        CustomerRegisteredEvent event = new CustomerRegisteredEvent(
                customer.getCustomerId(),
                customer.getFirstName(),
                customer.getLastName(),
                customer.getEmail(),
                customer.getMobileNumber(),
                LocalDateTime.now()
        );
        publish(customer.getCustomerId(), event);
        log.info("[KAFKA] Published CustomerRegisteredEvent for: {}", customer.getCustomerId());
    }

    public void publishCustomerVerified(Customer customer) {
        CustomerVerifiedEvent event = new CustomerVerifiedEvent(
                customer.getCustomerId(),
                customer.getFullName(),
                LocalDateTime.now()
        );
        publish(customer.getCustomerId(), event);
        log.info("[KAFKA] Published CustomerVerifiedEvent for: {}", customer.getCustomerId());
    }

    private void publish(String key, Object event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            // KEY = customerId → ensures all events for same customer
            // go to the same Kafka partition → ordered processing
            kafkaTemplate.send(TOPIC_CUSTOMERS, key, payload);
        } catch (Exception e) {
            log.error("[KAFKA] Failed to publish event", e);
        }
    }
}

// ----------------------------------------------------------------
// EVENT CLASSES — immutable facts that happened
// Past tense: CustomerREGISTERED, CustomerVERIFIED
// ----------------------------------------------------------------
record CustomerRegisteredEvent(
        String customerId,
        String firstName,
        String lastName,
        String email,
        String mobileNumber,
        LocalDateTime occurredAt
) {
    public String getEventType() { return "CUSTOMER_REGISTERED"; }
}

record CustomerVerifiedEvent(
        String customerId,
        String fullName,
        LocalDateTime occurredAt
) {
    public String getEventType() { return "CUSTOMER_VERIFIED"; }
}
