package com.alhilal.bff.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.List;

@FeignClient(
        name = "customer-services",
        url = "${services.customer.url}",
        fallback = CustomerClientFallback.class
)
public interface CustomerServiceClient {

    @GetMapping("/api/v1/customers/{customerId}")
    CustomerResponse getCustomer(@PathVariable String customerId);

    @GetMapping("/api/v1/customers")
    List<CustomerResponse> getAllCustomers();
}