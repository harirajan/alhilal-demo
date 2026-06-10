package com.alhilal.customer.domain;

import com.alhilal.customer.events.CustomerEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

/**
 * ================================================================
 * CUSTOMER SERVICE — business logic lives here
 * ================================================================
 * All business rules for the Customer domain are in this class.
 * The controller just calls this. Thin controller, fat service.
 * ================================================================
 */
@Service
@RequiredArgsConstructor
@Transactional
public class CustomerService {

    private final CustomerRepository repository;
    private final CustomerEventPublisher eventPublisher;

    // ----------------------------------------------------------------
    // REGISTER a new customer
    // After saving → publishes CustomerRegisteredEvent to Kafka
    // Other services (notification-service) will listen and react
    // ----------------------------------------------------------------
    public Customer register(String firstName, String lastName,
                              String email, String mobileNumber,
                              String emiratesId) {

        // Business rule: email must be unique
        if (repository.findByEmail(email).isPresent()) {
            throw new CustomerAlreadyExistsException("Email already registered: " + email);
        }

        // Create via factory method (DDD pattern)
        Customer customer = Customer.register(firstName, lastName, email, mobileNumber,
                emiratesId);
        Customer saved = repository.save(customer);

        // Publish event to Kafka — loosely coupled
        // notification-service will consume this and send welcome SMS
        // account-service doesn't care about this event (yet)
        eventPublisher.publishCustomerRegistered(saved);

        return saved;
    }

    // ----------------------------------------------------------------
    // VERIFY KYC — in real Al Hilal: called after UAE PASS verification
    // ----------------------------------------------------------------
    public Customer verifyKyc(String customerId) {
        Customer customer = findById(customerId);
        customer.verifyKyc(); // business rule enforced in domain object
        Customer saved = repository.save(customer);

        // Publish event — account-service listens to this
        // When customer is KYC verified → account-service can open account
        eventPublisher.publishCustomerVerified(saved);

        return saved;
    }

    // ----------------------------------------------------------------
    // QUERIES — read operations
    // ----------------------------------------------------------------
    @Transactional(readOnly = true)
    public Customer findById(String customerId) {
        return repository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found: " + customerId));
    }

    @Transactional(readOnly = true)
    public List<Customer> findAll() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Customer> findPendingKyc() {
        return repository.findByKycStatus(Customer.KycStatus.PENDING);
    }
}

