package com.alhilal.customer.unit;

import com.alhilal.customer.domain.Customer;
import com.alhilal.customer.domain.CustomerAlreadyExistsException;
import com.alhilal.customer.domain.CustomerNotFoundException;
import com.alhilal.customer.domain.CustomerRepository;
import com.alhilal.customer.domain.CustomerService;
import com.alhilal.customer.events.CustomerEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomerService Unit Tests")
class CustomerServiceTest {

    @Mock
    private CustomerRepository repository;

    @Mock
    private CustomerEventPublisher eventPublisher;

    @InjectMocks
    private CustomerService customerService;

    @Captor
    private ArgumentCaptor<Customer> customerCaptor;

    private Customer testCustomer;

    @BeforeEach
    void setUp() {
        testCustomer = new Customer();
        testCustomer.setCustomerId("CUST-C09DDEE2");
        testCustomer.setFirstName("Abdullah");
        testCustomer.setLastName("Al-Rashid");
        testCustomer.setEmail("abdullah@test.com");
        testCustomer.setMobileNumber("+971501234567");
        testCustomer.setKycStatus(Customer.KycStatus.PENDING);
    }

    // ─── register() tests ────────────────────────────────
    @Nested
    @DisplayName("register()")
    class RegisterTests {

        @Test
        @DisplayName("should register customer and publish event")
        void shouldRegisterCustomerAndPublishEvent() {
            // GIVEN
            when(repository.findByEmail("abdullah@test.com"))
                .thenReturn(Optional.empty());
            when(repository.save(any(Customer.class)))
                .thenReturn(testCustomer);

            // WHEN
            Customer result = customerService.register(
                "Abdullah", "Al-Rashid",
                "abdullah@test.com",
                "+971501234567",
                "784-1990-1234567-1"
            );

            // THEN
            assertThat(result).isNotNull();
            assertThat(result.getFirstName()).isEqualTo("Abdullah");
            assertThat(result.getEmail()).isEqualTo("abdullah@test.com");

            // verify repository.save was called
            verify(repository).save(any(Customer.class));

            // verify Kafka event was published
            verify(eventPublisher).publishCustomerRegistered(testCustomer);
        }

        @Test
        @DisplayName("should throw exception when email already exists")
        void shouldThrowWhenEmailAlreadyExists() {
            // GIVEN — email already registered
            when(repository.findByEmail("abdullah@test.com"))
                .thenReturn(Optional.of(testCustomer));

            // WHEN + THEN
            assertThatThrownBy(() ->
                customerService.register(
                    "Abdullah", "Al-Rashid",
                    "abdullah@test.com",
                    "+971501234567",
                    "784-1990-1234567-1"
                ))
                .isInstanceOf(CustomerAlreadyExistsException.class)
                .hasMessageContaining("abdullah@test.com");

            // verify save was NEVER called
            verify(repository, never()).save(any());

            // verify event was NEVER published
            verify(eventPublisher, never())
                .publishCustomerRegistered(any());
        }

        @Test
        @DisplayName("should save customer with correct data")
        void shouldSaveCustomerWithCorrectData() {
            // GIVEN
            when(repository.findByEmail(anyString()))
                .thenReturn(Optional.empty());
            when(repository.save(any(Customer.class)))
                .thenAnswer(inv -> inv.getArgument(0));

            // WHEN
            customerService.register(
                "Fatima", "Al-Hassan",
                "fatima@test.com",
                "+971509876543",
                "784-1995-7654321-2"
            );

            // THEN — capture what was saved
            verify(repository).save(customerCaptor.capture());
            Customer saved = customerCaptor.getValue();

            assertThat(saved.getFirstName()).isEqualTo("Fatima");
            assertThat(saved.getLastName()).isEqualTo("Al-Hassan");
            assertThat(saved.getEmail()).isEqualTo("fatima@test.com");
            assertThat(saved.getKycStatus())
                .isEqualTo(Customer.KycStatus.PENDING);
        }
    }

    // ─── verifyKyc() tests ───────────────────────────────
    @Nested
    @DisplayName("verifyKyc()")
    class VerifyKycTests {

        @Test
        @DisplayName("should verify KYC and publish event")
        void shouldVerifyKycAndPublishEvent() {
            // GIVEN
            when(repository.findById("CUST-C09DDEE2"))
                .thenReturn(Optional.of(testCustomer));
            when(repository.save(any(Customer.class)))
                .thenReturn(testCustomer);

            // WHEN
            Customer result = customerService.verifyKyc("CUST-C09DDEE2");

            // THEN
            assertThat(result).isNotNull();

            // verify KYC status updated
            verify(repository).save(customerCaptor.capture());
            Customer saved = customerCaptor.getValue();
            assertThat(saved.getKycStatus())
                .isEqualTo(Customer.KycStatus.VERIFIED);

            // verify event published
            verify(eventPublisher).publishCustomerVerified(any());
        }

        @Test
        @DisplayName("should throw when customer not found")
        void shouldThrowWhenCustomerNotFound() {
            // GIVEN
            when(repository.findById("CUST-UNKNOWN"))
                .thenReturn(Optional.empty());

            // WHEN + THEN
            assertThatThrownBy(() ->
                customerService.verifyKyc("CUST-UNKNOWN"))
                .isInstanceOf(CustomerNotFoundException.class)
                .hasMessageContaining("CUST-UNKNOWN");

            verify(repository, never()).save(any());
            verify(eventPublisher, never())
                .publishCustomerVerified(any());
        }
    }

    // ─── findById() tests ────────────────────────────────
    @Nested
    @DisplayName("findById()")
    class FindByIdTests {

        @Test
        @DisplayName("should return customer when found")
        void shouldReturnCustomerWhenFound() {
            // GIVEN
            when(repository.findById("CUST-C09DDEE2"))
                .thenReturn(Optional.of(testCustomer));

            // WHEN
            Customer result = customerService.findById("CUST-C09DDEE2");

            // THEN
            assertThat(result).isNotNull();
            assertThat(result.getCustomerId())
                .isEqualTo("CUST-C09DDEE2");
            assertThat(result.getFirstName())
                .isEqualTo("Abdullah");

            verify(repository, times(1)).findById("CUST-C09DDEE2");
        }

        @Test
        @DisplayName("should throw CustomerNotFoundException when not found")
        void shouldThrowWhenNotFound() {
            // GIVEN
            when(repository.findById("CUST-999"))
                .thenReturn(Optional.empty());

            // WHEN + THEN
            assertThatThrownBy(() ->
                customerService.findById("CUST-999"))
                .isInstanceOf(CustomerNotFoundException.class)
                .hasMessageContaining("CUST-999");

            verify(repository).findById("CUST-999");
        }
    }

    // ─── findPendingKyc() tests ──────────────────────────
    @Nested
    @DisplayName("findPendingKyc()")
    class FindPendingKycTests {

        @Test
        @DisplayName("should return list of pending customers")
        void shouldReturnPendingCustomers() {
            // GIVEN
            Customer pending1 = new Customer();
            pending1.setCustomerId("CUST-001");
            pending1.setKycStatus(Customer.KycStatus.PENDING);

            Customer pending2 = new Customer();
            pending2.setCustomerId("CUST-002");
            pending2.setKycStatus(Customer.KycStatus.PENDING);

            when(repository.findByKycStatus(Customer.KycStatus.PENDING))
                .thenReturn(List.of(pending1, pending2));

            // WHEN
            List<Customer> result = customerService.findPendingKyc();

            // THEN
            assertThat(result).hasSize(2);
            assertThat(result)
                .extracting(Customer::getKycStatus)
                .containsOnly(Customer.KycStatus.PENDING);

            verify(repository)
                .findByKycStatus(Customer.KycStatus.PENDING);
        }

        @Test
        @DisplayName("should return empty list when no pending customers")
        void shouldReturnEmptyListWhenNoPending() {
            // GIVEN
            when(repository.findByKycStatus(Customer.KycStatus.PENDING))
                .thenReturn(List.of());

            // WHEN
            List<Customer> result = customerService.findPendingKyc();

            // THEN
            assertThat(result).isEmpty();
        }
    }
}
