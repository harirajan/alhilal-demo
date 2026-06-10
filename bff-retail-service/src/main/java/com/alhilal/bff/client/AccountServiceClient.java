package com.alhilal.bff.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.List;

@FeignClient(
        name = "account-services",
        url = "${services.account.url}",
        fallback = AccountClientFallback.class
)
public interface AccountServiceClient {

    @GetMapping("/api/v1/accounts")
    List<AccountResponse> getAccountsByCustomer(@RequestParam String customerId);

    @GetMapping("/api/v1/accounts/{accountId}")
    AccountResponse getAccount(@PathVariable String accountId);

    @GetMapping("/api/v1/accounts/all")
    List<AccountResponse> getAllAccounts();
}