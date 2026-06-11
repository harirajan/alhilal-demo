package com.alhilal.cqrs.command.api;

import com.alhilal.cqrs.command.model.Account;
import com.alhilal.cqrs.command.service.AccountCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cqrs/accounts")
@RequiredArgsConstructor
public class AccountCommandController {

    private final AccountCommandService commandService;

    @PostMapping
    public ResponseEntity<Account> create(
            @RequestBody CreateRequest req) throws Exception {
        return ResponseEntity.ok(
            commandService.createAccount(
                req.customerId(), req.accountType(), req.currency()));
    }

    @PostMapping("/{accountId}/deposit")
    public ResponseEntity<Account> deposit(
            @PathVariable String accountId,
            @RequestBody AmountRequest req) throws Exception {
        return ResponseEntity.ok(
            commandService.deposit(accountId, req.amount()));
    }

    @PostMapping("/{accountId}/withdraw")
    public ResponseEntity<Account> withdraw(
            @PathVariable String accountId,
            @RequestBody AmountRequest req) throws Exception {
        return ResponseEntity.ok(
            commandService.withdraw(accountId, req.amount()));
    }

    public record CreateRequest(
        String customerId, String accountType, String currency) {}
    public record AmountRequest(Double amount) {}
}
