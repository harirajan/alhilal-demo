package com.alhilal.customer.api;

import com.alhilal.customer.domain.Customer;
import com.alhilal.customer.domain.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

/**
 * ================================================================
 * CUSTOMER REST API
 * ================================================================
 * This is what other services call (via Feign client).
 * This is what the BFF calls.
 * This is what Kong routes to (in production).
 *
 * URL convention (from Al Hilal standards): REST resource model
 * /api/v1/customers              ← collection
 * /api/v1/customers/{id}         ← single resource
 * /api/v1/customers/{id}/verify-kyc  ← action on resource
 * ================================================================
 */
@RestController
@RequestMapping("/api/v1/customers")
@Tag(name = "Customers", description = "Customer Domain Service API")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    // ----------------------------------------------------------------
    // POST /api/v1/customers — Register new customer
    // Returns 201 Created
    // ----------------------------------------------------------------
    @PostMapping
    @Operation(
        summary = "Register a new customer",
        description = "Creates customer with KYC status = PENDING. " +
                      "Publishes CustomerRegisteredEvent to Kafka."
    )
    public ResponseEntity<CustomerResponse> register(
            @Valid @RequestBody RegisterCustomerRequest request) {

        Customer customer = customerService.register(
                request.firstName(),
                request.lastName(),
                request.email(),
                request.mobileNumber(),
                request.emiratesId()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(CustomerResponse.from(customer));
    }

    // ----------------------------------------------------------------
    // GET /api/v1/customers/{customerId} — Get single customer
    // Called by bff-retail-service via Feign
    // ----------------------------------------------------------------
    @GetMapping("/{customerId}")
    @Operation(
        summary = "Get customer by ID",
        description = "Called by BFF to get customer details for home screen"
    )
    public ResponseEntity<CustomerResponse> getCustomer(
            @PathVariable String customerId) {

        Customer customer = customerService.findById(customerId);
        return ResponseEntity.ok(CustomerResponse.from(customer));
    }

    // ----------------------------------------------------------------
    // GET /api/v1/customers — Get all customers
    // ----------------------------------------------------------------
    @GetMapping
    @Operation(summary = "Get all customers")
    public ResponseEntity<List<CustomerResponse>> getAllCustomers() {
        List<CustomerResponse> customers = customerService.findAll()
                .stream()
                .map(CustomerResponse::from)
                .toList();
        return ResponseEntity.ok(customers);
    }

    // ----------------------------------------------------------------
    // PATCH /api/v1/customers/{id}/verify-kyc — Verify KYC
    // In real Al Hilal: called after UAE PASS verification succeeds
    // Publishes CustomerVerifiedEvent to Kafka
    // ----------------------------------------------------------------
    @PatchMapping("/{customerId}/verify-kyc")
    @Operation(
        summary = "Verify customer KYC",
        description = "In real Al Hilal: triggered after UAE PASS verification. " +
                      "Changes status PENDING → VERIFIED. " +
                      "Publishes CustomerVerifiedEvent to Kafka."
    )
    public ResponseEntity<CustomerResponse> verifyKyc(
            @PathVariable String customerId) {

        Customer customer = customerService.verifyKyc(customerId);
        return ResponseEntity.ok(CustomerResponse.from(customer));
    }

    // ----------------------------------------------------------------
    // GET /api/v1/customers/pending-kyc — Useful for ops team
    // ----------------------------------------------------------------
    @GetMapping("/pending-kyc")
    @Operation(summary = "Get all customers with pending KYC")
    public ResponseEntity<List<CustomerResponse>> getPendingKyc() {
        List<CustomerResponse> customers = customerService.findPendingKyc()
                .stream()
                .map(CustomerResponse::from)
                .toList();
        return ResponseEntity.ok(customers);
    }

    // ----------------------------------------------------------------
    // EXCEPTION HANDLER — consistent error format
    // ----------------------------------------------------------------
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleException(Exception e) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", e.getMessage()));
    }
}

// ----------------------------------------------------------------
// REQUEST / RESPONSE DTOs
// Records = immutable, concise (Java 16+)
// ----------------------------------------------------------------

record RegisterCustomerRequest(
        @NotBlank(message = "First name required") String firstName,
        @NotBlank(message = "Last name required") String lastName,
        @Email(message = "Valid email required") String email,
        @NotBlank(message = "Mobile number required") String mobileNumber,
        String emiratesId
) {}

// Response DTO — notice we don't expose emiratesId (sensitive data)
record CustomerResponse(
        String customerId,
        String firstName,
        String lastName,
        String fullName,
        String email,
        String mobileNumber,
        String kycStatus,
        String createdAt
) {
    // Maps from domain object to response DTO
    static CustomerResponse from(Customer c) {
        return new CustomerResponse(
                c.getCustomerId(),
                c.getFirstName(),
                c.getLastName(),
                c.getFullName(),
                c.getEmail(),
                c.getMobileNumber(),
                c.getKycStatus().name(),
                c.getCreatedAt().toString()
        );
    }
}
