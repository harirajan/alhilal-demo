package com.alhilal.bff.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
@Slf4j
public class CustomerClientFallback implements CustomerServiceClient {

    @Override
    public CustomerResponse getCustomer(String customerId) {
        log.warn("[FALLBACK] getCustomer called for: {}", customerId);
        log.warn("[FALLBACK] This means circuit breaker is OPEN or customer-service returned error");
        return new CustomerResponse(customerId, "Unavailable", "", "Service unavailable", "", "", "UNKNOWN", "");
    }

    @Override
    public List<CustomerResponse> getAllCustomers() {
        log.warn("[FALLBACK] getAllCustomers called");
        return List.of();
    }
}