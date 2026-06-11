package com.alhilal.customer.unit;

import com.alhilal.customer.domain.Customer;
import com.alhilal.customer.events.CustomerEventPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomerEventPublisher Unit Tests")
class CustomerEventPublisherTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private CustomerEventPublisher eventPublisher;

    @Captor
    private ArgumentCaptor<String> topicCaptor;

    @Captor
    private ArgumentCaptor<String> keyCaptor;

    @Captor
    private ArgumentCaptor<String> payloadCaptor;

    private Customer buildTestCustomer() {
        Customer customer = new Customer();
        customer.setCustomerId("CUST-C09DDEE2");
        customer.setFirstName("Abdullah");
        customer.setLastName("Al-Rashid");
        customer.setEmail("abdullah@test.com");
        customer.setMobileNumber("+971501234567");
        return customer;
    }

    @Test
    @DisplayName("should publish CustomerRegisteredEvent to correct topic")
    void shouldPublishRegisteredEventToCorrectTopic() throws Exception {
        // GIVEN
        Customer customer = buildTestCustomer();
        when(objectMapper.writeValueAsString(any()))
            .thenReturn("{\"eventType\":\"CUSTOMER_REGISTERED\"}");

        // WHEN
        eventPublisher.publishCustomerRegistered(customer);

        // THEN
        verify(kafkaTemplate).send(
            topicCaptor.capture(),
            keyCaptor.capture(),
            payloadCaptor.capture()
        );

        assertThat(topicCaptor.getValue())
            .isEqualTo("banking.customers");
        assertThat(keyCaptor.getValue())
            .isEqualTo("CUST-C09DDEE2");
    }

    @Test
    @DisplayName("should publish CustomerVerifiedEvent to correct topic")
    void shouldPublishVerifiedEventToCorrectTopic() throws Exception {
        // GIVEN
        Customer customer = buildTestCustomer();
        when(objectMapper.writeValueAsString(any()))
            .thenReturn("{\"eventType\":\"CUSTOMER_VERIFIED\"}");

        // WHEN
        eventPublisher.publishCustomerVerified(customer);

        // THEN
        verify(kafkaTemplate).send(
            topicCaptor.capture(),
            keyCaptor.capture(),
            anyString()
        );

        assertThat(topicCaptor.getValue())
            .isEqualTo("banking.customers");
        assertThat(keyCaptor.getValue())
            .isEqualTo("CUST-C09DDEE2");
    }

    @Test
    @DisplayName("should not throw when Kafka publish fails")
    void shouldNotThrowWhenKafkaFails() throws Exception {
        // GIVEN
        Customer customer = buildTestCustomer();
        when(objectMapper.writeValueAsString(any()))
            .thenThrow(new RuntimeException("Serialization failed"));

        // WHEN + THEN — should NOT throw, just log error
        assertThatNoException().isThrownBy(() ->
            eventPublisher.publishCustomerRegistered(customer));
    }
}
