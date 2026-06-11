package com.alhilal.cqrs.query.api;

import com.alhilal.cqrs.query.model.AccountView;
import com.alhilal.cqrs.query.service.AccountQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cqrs/accounts")
@RequiredArgsConstructor
public class AccountQueryController {

    private final AccountQueryService queryService;

    @GetMapping("/{accountId}")
    public ResponseEntity<AccountView> getAccount(
            @PathVariable String accountId) {
        return queryService.getAccount(accountId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<AccountView>> getByCustomer(
            @PathVariable String customerId) {
        return ResponseEntity.ok(
            queryService.getByCustomer(customerId));
    }

    @GetMapping
    public ResponseEntity<List<AccountView>> getAll() {
        return ResponseEntity.ok(queryService.getAllAccounts());
    }
}
