package com.alhilal.customer.integration;

import com.alhilal.customer.domain.Customer;
import com.alhilal.customer.domain.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(
    replace = AutoConfigureTestDatabase.Replace.NONE)
// ↑ don't replace with H2 — use our PostgreSQL container
@DisplayName("CustomerRepository — Real PostgreSQL via Testcontainers")
class CustomerPostgresTest {

    // ── Start real PostgreSQL Docker container ────────────
    @Container
    static PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass");

    // ── Point Spring to our container ────────────────────
    @DynamicPropertySource
    static void configureProperties(
            DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",
            postgres::getJdbcUrl);
        registry.add("spring.datasource.username",
            postgres::getUsername);
        registry.add("spring.datasource.password",
            postgres::getPassword);
        registry.add("spring.datasource.driver-class-name",
            () -> "org.postgresql.Driver");
        registry.add("spring.jpa.database-platform",
            () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.jpa.hibernate.ddl-auto",
            () -> "create-drop");
    }

    @Autowired
    private CustomerRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    @DisplayName("PostgreSQL is running")
    void postgresIsRunning() {
        assertThat(postgres.isRunning()).isTrue();
        System.out.println("PostgreSQL URL: "
            + postgres.getJdbcUrl());
        System.out.println("PostgreSQL Version: "
            + postgres.getDatabaseName());
    }

    @Test
    @DisplayName("should save and retrieve from real PostgreSQL")
    void shouldSaveAndRetrieveFromPostgres() {
        // GIVEN
        Customer customer = Customer.register(
            "Postgres", "Test",
            "postgres@test.com",
            "+971507777001",
            "784-2000-7777777-7"
        );

        // WHEN — real INSERT to real PostgreSQL
        Customer saved = repository.save(customer);

        // THEN — real SELECT from real PostgreSQL
        Optional<Customer> found =
            repository.findById(saved.getCustomerId());

        assertThat(found).isPresent();
        assertThat(found.get().getFirstName())
            .isEqualTo("Postgres");
        assertThat(found.get().getCustomerId())
            .startsWith("CUST-");
    }

    @Test
    @DisplayName("should find by email in real PostgreSQL")
    void shouldFindByEmailInPostgres() {
        // GIVEN
        Customer customer = Customer.register(
            "Real", "Postgres",
            "real.postgres@test.com",
            "+971507777002",
            "784-2000-8888888-8"
        );
        repository.save(customer);

        // WHEN — real SQL on real PostgreSQL
        Optional<Customer> found =
            repository.findByEmail("real.postgres@test.com");

        // THEN
        assertThat(found).isPresent();
        assertThat(found.get().getFirstName()).isEqualTo("Real");
    }

    @Test
    @DisplayName("should handle KYC status enum in PostgreSQL")
    void shouldHandleKycStatusEnum() {
        // This tests PostgreSQL enum behavior
        // H2 might handle differently
        Customer customer = Customer.register(
            "KycTest", "User",
            "kyc.postgres@test.com",
            "+971507777003",
            "784-2000-9999999-9"
        );
        repository.save(customer);

        // verify KYC
        customer.verifyKyc();
        repository.save(customer);

        Optional<Customer> found =
            repository.findById(customer.getCustomerId());
        assertThat(found.get().getKycStatus())
            .isEqualTo(Customer.KycStatus.VERIFIED);
    }
}
