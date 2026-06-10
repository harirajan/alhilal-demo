package com.alhilal.customer.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * ================================================================
 * DOMAIN: Customer
 * ================================================================
 * This is the Aggregate Root for the Customer bounded context.
 *
 * What this service OWNS:
 *   - Customer name, contact details
 *   - KYC status (verified via UAE PASS in real Al Hilal)
 *   - Emirates ID
 *
 * What this service does NOT own:
 *   - Account balances → that's account-service's job
 *   - Transactions → that's transaction-service's job
 *   - Loans → that's loan-service's job
 *
 * This service stores only customerId in other services,
 * not the full Customer object. They call this API if they need
 * customer details.
 * ================================================================
 */
@Entity
@Table(name = "customers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer {

    @Id
    private String customerId;

    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;

    @Email
    @Column(unique = true)
    private String email;

    @Column(unique = true)
    private String mobileNumber;

    // Emirates ID — in real system this is encrypted at field level
    private String emiratesId;

    @Enumerated(EnumType.STRING)
    private KycStatus kycStatus;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Add this field inside the Customer class
    @Column(nullable = true)  // nullable because auth-service sets it via Kafka in production
    private String passwordHash;

    // ----------------------------------------------------------------
    // FACTORY METHOD — only way to create a Customer
    // Enforces: customerId is always a UUID, kycStatus starts as PENDING
    // ----------------------------------------------------------------
    public static Customer register(String firstName, String lastName,
                                    String email, String mobileNumber,
                                    String emiratesId){
        return Customer.builder()
                .customerId("CUST-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .firstName(firstName)
                .lastName(lastName)
                .email(email)
                .mobileNumber(mobileNumber)
                .emiratesId(emiratesId)
                .passwordHash(null)
                .kycStatus(KycStatus.PENDING) // always starts PENDING
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // ----------------------------------------------------------------
    // BUSINESS METHOD — verify KYC
    // In real Al Hilal: called after UAE PASS verification succeeds
    // ----------------------------------------------------------------
    public void verifyKyc() {
        if (this.kycStatus == KycStatus.VERIFIED) {
            throw new IllegalStateException("Customer already verified");
        }
        this.kycStatus = KycStatus.VERIFIED;
        this.updatedAt = LocalDateTime.now();
    }

    // Convenience method for BFF — full name
    public String getFullName() {
        return firstName + " " + lastName;
    }

    public enum KycStatus {
        PENDING,    // just registered, not yet verified
        VERIFIED,   // UAE PASS verified (in real system)
        REJECTED    // failed verification
    }
}
