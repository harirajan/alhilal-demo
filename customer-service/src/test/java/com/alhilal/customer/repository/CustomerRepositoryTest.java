package com.alhilal.customer.repository;

import com.alhilal.customer.domain.Customer;
import com.alhilal.customer.domain.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
// ↑ real H2 DB — actual SQL queries run
// ↑ NO controller, NO service, NO Kafka
// ↑ just: does the SQL query work?
@DisplayName("CustomerRepository Tests — Real H2 DB")
class CustomerRepositoryTest {

    @Autowired
    private CustomerRepository repository;

    private Customer abdullah;
    private Customer fatima;

    @BeforeEach
    void setUp() {
        repository.deleteAll();

        abdullah = Customer.register(
            "Abdullah", "Al-Rashid",
            "abdullah@test.com",
            "+971501234567",
            "784-1990-1234567-1"
        );
        repository.save(abdullah);

        fatima = Customer.register(
            "Fatima", "Al-Hassan",
            "fatima@test.com",
            "+971509876543",
            "784-1995-7654321-2"
        );
        repository.save(fatima);
    }

    @Nested
    @DisplayName("findByEmail()")
    class FindByEmailTests {

        @Test
        @DisplayName("should find customer by email — real SQL")
        void shouldFindByEmail() {
            // WHEN — real SQL: SELECT * FROM customers WHERE email=?
            Optional<Customer> found =
                repository.findByEmail("abdullah@test.com");

            assertThat(found).isPresent();
            assertThat(found.get().getFirstName())
                .isEqualTo("Abdullah");
        }

        @Test
        @DisplayName("should return empty for unknown email")
        void shouldReturnEmptyForUnknownEmail() {
            Optional<Customer> found =
                repository.findByEmail("unknown@test.com");
            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByKycStatus()")
    class FindByKycStatusTests {

        @Test
        @DisplayName("should return all PENDING customers")
        void shouldReturnPendingCustomers() {
            List<Customer> pending =
                repository.findByKycStatus(Customer.KycStatus.PENDING);

            assertThat(pending).hasSize(2);
            assertThat(pending)
                .extracting(Customer::getKycStatus)
                .containsOnly(Customer.KycStatus.PENDING);
        }

        @Test
        @DisplayName("should return VERIFIED after KYC update")
        void shouldReturnVerifiedAfterKycUpdate() {
            // GIVEN — verify Abdullah
            abdullah.verifyKyc();
            repository.save(abdullah);

            // WHEN
            List<Customer> verified =
                repository.findByKycStatus(Customer.KycStatus.VERIFIED);
            List<Customer> pending =
                repository.findByKycStatus(Customer.KycStatus.PENDING);

            // THEN
            assertThat(verified).hasSize(1);
            assertThat(verified.get(0).getFirstName())
                .isEqualTo("Abdullah");
            assertThat(pending).hasSize(1);
            assertThat(pending.get(0).getFirstName())
                .isEqualTo("Fatima");
        }
    }

    @Nested
    @DisplayName("findByMobileNumber()")
    class FindByMobileTests {

        @Test
        @DisplayName("should find by mobile number")
        void shouldFindByMobile() {
            Optional<Customer> found =
                repository.findByMobileNumber("+971501234567");

            assertThat(found).isPresent();
            assertThat(found.get().getFirstName())
                .isEqualTo("Abdullah");
        }
    }

    @Nested
    @DisplayName("save() and findById()")
    class SaveAndFindTests {

        @Test
        @DisplayName("should persist and retrieve customer")
        void shouldPersistAndRetrieve() {
            Customer newCustomer = Customer.register(
                "Ahmed", "Al-Maktoum",
                "ahmed@test.com",
                "+971507777777",
                "784-2000-9999999-9"
            );

            Customer saved = repository.save(newCustomer);
            Optional<Customer> found =
                repository.findById(saved.getCustomerId());

            assertThat(found).isPresent();
            assertThat(found.get().getFirstName())
                .isEqualTo("Ahmed");
        }

        @Test
        @DisplayName("should update KYC on save")
        void shouldUpdateKycOnSave() {
            abdullah.verifyKyc();
            repository.save(abdullah);

            Customer updated =
                repository.findById(
                    abdullah.getCustomerId()).get();
            assertThat(updated.getKycStatus())
                .isEqualTo(Customer.KycStatus.VERIFIED);
        }
    }
}
