package com.alhilal.bff.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.List;

@Component
@Slf4j
public class AccountClientFallback implements AccountServiceClient {

    @Override
    public List<AccountResponse> getAccountsByCustomer(String customerId) {
        log.warn("[CIRCUIT BREAKER] account-service is down! Returning empty accounts for: {}", customerId);
        return List.of();
    }

    @Override
    public AccountResponse getAccount(String accountId) {
        log.warn("[CIRCUIT BREAKER] account-service is down! Returning fallback account.");
        return new AccountResponse(accountId, "", "", "UNKNOWN", "UNKNOWN", BigDecimal.ZERO, "AED", "");
    }

    @Override
    public List<AccountResponse> getAllAccounts() {
        return List.of();
    }
}