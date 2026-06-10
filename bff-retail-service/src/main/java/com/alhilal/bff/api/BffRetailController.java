package com.alhilal.bff.api;

import com.alhilal.bff.client.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * ================================================================
 * BFF RETAIL SERVICE — the orchestrator
 * ================================================================
 * This is bff-retail-services at Al Hilal.
 *
 * What it does:
 *   1. Receives ONE request from mobile app
 *   2. Calls multiple domain services IN PARALLEL
 *   3. Assembles the response tailored for mobile
 *   4. Returns ONE response to mobile
 *
 * What it does NOT do:
 *   - No database access
 *   - No business rules
 *   - No direct Kafka publishing
 *
 * Naming (from Al Hilal Confluence): bff-{domain}-services
 * URL pattern: /bff/v1/... (journey-based, not resource-based)
 * ================================================================
 */
@RestController
@RequestMapping("/bff/v1")
@Tag(name = "BFF Retail", description = "Backend For Frontend — Mobile Banking App")
@RequiredArgsConstructor
@Slf4j
public class BffRetailController {

    // Feign clients — these make HTTP calls to domain services
    private final CustomerServiceClient customerClient;
    private final AccountServiceClient accountClient;

    /**
     * HOME SCREEN endpoint
     * Mobile calls this ONE endpoint to load the entire home screen.
     *
     * WITHOUT BFF: mobile would need 2 calls (customer + accounts)
     *              each over slow mobile network
     *
     * WITH BFF: mobile makes 1 call
     *           BFF makes 2 calls IN PARALLEL on fast internal network
     *           Mobile gets back exactly what the home screen needs
     */
    @GetMapping("/home/{customerId}")
    @Operation(
        summary = "Load mobile home screen",
        description = """
            KEY LEARNING ENDPOINT.
            
            This calls customer-service AND account-service IN PARALLEL.
            Returns a tailored response for the mobile home screen.
            
            Watch the logs — you'll see:
            [BFF] Calling customer-service and account-service in parallel...
            [BFF] customer-service responded in Xms
            [BFF] account-service responded in Xms
            [BFF] Assembled home screen response
            
            Try stopping customer-service → circuit breaker activates,
            BFF still returns partial data (accounts still show).
            """
    )
    public ResponseEntity<HomeScreenResponse> getHomeScreen(
            @PathVariable String customerId) {

        log.info("[BFF] Building home screen for customer: {}", customerId);
        long start = System.currentTimeMillis();

        // PARALLEL CALLS — both happen at the same time
        // Not sequential — this is why BFF is faster than mobile calling each service
        CompletableFuture<CustomerResponse> customerFuture =
            CompletableFuture.supplyAsync(() -> {
                log.info("[BFF] → Calling customer-service...");
                return customerClient.getCustomer(customerId);
            });

        CompletableFuture<List<AccountResponse>> accountsFuture =
            CompletableFuture.supplyAsync(() -> {
                log.info("[BFF] → Calling account-service...");
                return accountClient.getAccountsByCustomer(customerId);
            });

        // Wait for BOTH to finish
        CompletableFuture.allOf(customerFuture, accountsFuture).join();

        CustomerResponse customer = customerFuture.join();
        List<AccountResponse> accounts = accountsFuture.join();

        // ADD THESE LOG LINES
        log.info("[BFF] Customer response: firstName={}, kycStatus={}",
                customer.firstName(), customer.kycStatus());
        log.info("[BFF] Accounts response: count={}", accounts.size());

        long elapsed = System.currentTimeMillis() - start;
        log.info("[BFF] Home screen assembled in {}ms (parallel calls)", elapsed);

        // ASSEMBLE — shape the response for the mobile UI
        // Not raw domain data — exactly what the home screen needs
        HomeScreenResponse response = HomeScreenResponse.builder()
                .greeting("Good morning, " + customer.firstName())
                .customerId(customerId)
                .kycStatus(customer.kycStatus())
                .totalBalance(accounts.stream()
                        .map(AccountResponse::balance)
                        .reduce(BigDecimal.ZERO, BigDecimal::add))
                .currency("AED")
                .accountCount(accounts.size())
                .accounts(accounts.stream().map(a ->
                    new HomeScreenResponse.AccountSummary(
                        a.accountId(),
                        a.accountType(),
                        maskAccountNumber(a.accountNumber()),  // AE•••••1234
                        a.balance(),
                        a.currency(),
                        a.status()
                    )).toList())
                .assembledInMs(elapsed)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * CUSTOMER PROFILE endpoint — mobile profile screen
     * Only needs customer data, not accounts
     */
    @GetMapping("/profile/{customerId}")
    @Operation(summary = "Mobile profile screen — customer details only")
    public ResponseEntity<CustomerResponse> getProfile(@PathVariable String customerId) {
        log.info("[BFF] → Calling customer-service for profile: {}", customerId);
        return ResponseEntity.ok(customerClient.getCustomer(customerId));
    }

    /**
     * ACCOUNTS LIST endpoint — mobile accounts screen
     * Only needs account data, not customer details
     */
    @GetMapping("/accounts/{customerId}")
    @Operation(summary = "Mobile accounts screen — list of accounts")
    public ResponseEntity<List<AccountResponse>> getAccounts(@PathVariable String customerId) {
        log.info("[BFF] → Calling account-service for accounts: {}", customerId);
        return ResponseEntity.ok(accountClient.getAccountsByCustomer(customerId));
    }

    /**
     * HEALTH CHECK — shows which downstream services are up
     * Useful for debugging locally
     */
    @GetMapping("/health/downstream")
    @Operation(summary = "Check downstream service health")
    public ResponseEntity<Map<String, Object>> downstreamHealth() {

        String customerStatus = checkService("http://localhost:8082/actuator/health");
        String accountStatus = checkService("http://localhost:8083/actuator/health");

        return ResponseEntity.ok(Map.of(
                "bff-retail-service", "UP",
                "customer-service", customerStatus,
                "account-service", accountStatus,
                "note", customerStatus.equals("UP") && accountStatus.equals("UP")
                        ? "All services healthy"
                        : "Some services down"
        ));
    }

    private String checkService(String healthUrl) {
        try {
            java.net.URL url = new java.net.URL(healthUrl);
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(2000);
            connection.setReadTimeout(2000);
            int responseCode = connection.getResponseCode();
            return responseCode == 200 ? "UP" : "DOWN";
        } catch (Exception e) {
            return "DOWN";
        }
    }

    // ----------------------------------------------------------------
    // HELPERS
    // ----------------------------------------------------------------
    private String maskAccountNumber(String number) {
        if (number == null || number.length() < 4) return "••••";
        return "AE•••••" + number.substring(number.length() - 4);
    }

    private boolean isServiceUp(Runnable call) {
        try { call.run(); return true; }
        catch (Exception e) { return false; }
    }
}

// ----------------------------------------------------------------
// RESPONSE MODEL — shaped for mobile home screen
// Not the raw domain data — exactly what mobile needs
// ----------------------------------------------------------------
@lombok.Builder
record HomeScreenResponse(
        String greeting,
        String customerId,
        String kycStatus,
        BigDecimal totalBalance,
        String currency,
        int accountCount,
        List<AccountSummary> accounts,
        long assembledInMs
) {
    record AccountSummary(
            String accountId,
            String accountType,
            String maskedAccountNumber,
            BigDecimal balance,
            String currency,
            String status
    ) {}
}
