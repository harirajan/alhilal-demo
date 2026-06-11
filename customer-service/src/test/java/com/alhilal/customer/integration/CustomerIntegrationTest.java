package com.alhilal.customer.integration;

import com.alhilal.customer.domain.Customer;
import com.alhilal.customer.domain.CustomerAlreadyExistsException;
import com.alhilal.customer.domain.CustomerNotFoundException;
import com.alhilal.customer.domain.CustomerRepository;
import com.alhilal.customer.domain.CustomerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@DisplayName("Customer Integration Tests — Full Stack H2")
class CustomerIntegrationTest {

    @Autowired
    private CustomerService customerService;

    @Autowired
    private CustomerRepository customerRepository;

    @MockBean
    private KafkaTemplate<String, String> kafkaTemplate;

    @BeforeEach
    void setUp() {
        // Clean DB before EACH test
        customerRepository.deleteAll();

        // Reset mock call counts before each test
        reset(kafkaTemplate);
    }

    // Helper methods
    private String uniqueEmail() {
        return "test." + UUID.randomUUID()
            .toString().substring(0, 8) + "@test.com";
    }

    private String uniqueMobile() {
        return "+9715" + UUID.randomUUID()
            .toString().replace("-", "").substring(0, 8);
    }

    // ─── register() tests ────────────────────────────────
    @Nested
    @DisplayName("register()")
    class RegisterTests {

        @Test
        @DisplayName("full flow: register → save to DB → publish event")
        void shouldRegisterFullFlow() {
            String email = uniqueEmail();

            // WHEN
            Customer result = customerService.register(
                "Ahmed", "Al-Maktoum",
                email, uniqueMobile(),
                "784-2000-1111111-1"
            );

            // THEN — saved to DB
            assertThat(result.getCustomerId()).isNotNull();
            assertThat(result.getKycStatus())
                .isEqualTo(Customer.KycStatus.PENDING);

            // retrievable from DB
            Customer fromDb = customerService
                .findById(result.getCustomerId());
            assertThat(fromDb.getEmail()).isEqualTo(email);

            // Kafka called once
            verify(kafkaTemplate, times(1))
                .send(eq("banking.customers"),
                    anyString(), anyString());
        }

        @Test
        @DisplayName("duplicate email: throws, only 1 customer in DB")
        void shouldThrowOnDuplicateEmail() {
            String email = uniqueEmail();

            // GIVEN — first registration
            customerService.register(
                "First", "User", email,
                uniqueMobile(), "784-2000-2222222-2"
            );

            // WHEN + THEN
            assertThatThrownBy(() ->
                customerService.register(
                    "Second", "User", email,
                    uniqueMobile(), "784-2000-3333333-3"
                ))
                .isInstanceOf(CustomerAlreadyExistsException.class);

            // only ONE customer in DB
            assertThat(customerRepository.findAll()).hasSize(1);
        }
    }

    // ─── verifyKyc() tests ───────────────────────────────
    @Nested
    @DisplayName("verifyKyc()")
    class VerifyKycTests {

        @Test
        @DisplayName("full flow: verify KYC → DB updated → event published")
        void shouldVerifyKycFullFlow() {
            // GIVEN
            Customer registered = customerService.register(
                "Khalid", "Al-Rashid",
                uniqueEmail(), uniqueMobile(),
                "784-2000-4444444-4"
            );

            // reset mock after register call
            reset(kafkaTemplate);

            // WHEN
            Customer verified = customerService
                .verifyKyc(registered.getCustomerId());

            // THEN
            assertThat(verified.getKycStatus())
                .isEqualTo(Customer.KycStatus.VERIFIED);

            // verify from DB
            Customer fromDb = customerService
                .findById(registered.getCustomerId());
            assertThat(fromDb.getKycStatus())
                .isEqualTo(Customer.KycStatus.VERIFIED);

            // Kafka called once for verifyKyc
            verify(kafkaTemplate, times(1))
                .send(eq("banking.customers"),
                    anyString(), anyString());
        }

        @Test
        @DisplayName("should throw for unknown customer")
        void shouldThrowForUnknownCustomer() {
            assertThatThrownBy(() ->
                customerService.verifyKyc("CUST-UNKNOWN-999"))
                .isInstanceOf(CustomerNotFoundException.class)
                .hasMessageContaining("CUST-UNKNOWN-999");
        }
    }

    // ─── findPendingKyc() tests ──────────────────────────
    @Nested
    @DisplayName("findPendingKyc()")
    class FindPendingTests {

        @Test
        @DisplayName("should return only PENDING customers from DB")
        void shouldReturnPendingFromDb() {
            // GIVEN — register two customers
            Customer c1 = customerService.register(
                "Pending1", "User",
                uniqueEmail(), uniqueMobile(),
                "784-2000-5555555-5"
            );
            Customer c2 = customerService.register(
                "Pending2", "User",
                uniqueEmail(), uniqueMobile(),
                "784-2000-6666666-6"
            );

            // verify c1 only
            customerService.verifyKyc(c1.getCustomerId());

            // WHEN
            List<Customer> pending =
                customerService.findPendingKyc();

            // THEN — only c2 is pending
            assertThat(pending).hasSize(1);
            assertThat(pending.get(0).getCustomerId())
                .isEqualTo(c2.getCustomerId());
        }

        @Test
        @DisplayName("should return empty when all verified")
        void shouldReturnEmptyWhenAllVerified() {
            // GIVEN
            Customer c1 = customerService.register(
                "Verify1", "User",
                uniqueEmail(), uniqueMobile(),
                "784-2000-7777777-7"
            );
            customerService.verifyKyc(c1.getCustomerId());

            // WHEN
            List<Customer> pending =
                customerService.findPendingKyc();

            // THEN
            assertThat(pending).isEmpty();
        }
    }
}
