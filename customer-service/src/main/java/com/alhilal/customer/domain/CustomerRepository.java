package com.alhilal.customer.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * ================================================================ REPOSITORY — data access for
 * Customer domain ================================================================ In a real
 * microservice this would connect to its OWN PostgreSQL. Here we use H2 (in-memory) to keep it
 * simple for local learning.
 *
 * Key principle: ONLY customer-service uses this repository. account-service CANNOT access this DB
 * directly. That would violate "Database per Service" principle. account-service must call
 * customer-service's REST API instead.
 * ================================================================
 */
@Repository
interface CustomerRepository extends JpaRepository<Customer, String> {
    Optional<Customer> findByEmail(String email);

    Optional<Customer> findByMobileNumber(String mobileNumber);

    List<Customer> findByKycStatus(Customer.KycStatus status);
}
