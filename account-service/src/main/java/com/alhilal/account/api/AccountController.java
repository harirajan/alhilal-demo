package com.alhilal.account.api;

import com.alhilal.account.domain.Account;
import com.alhilal.account.domain.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import com.alhilal.account.domain.Account;
import com.alhilal.account.domain.AccountService;  // ← add this line
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/accounts")
@Tag(name = "Accounts", description = "Account Domain Service API")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    @Operation(
        summary = "Open a new account",
        description = "Creates account for a customer. " +
                      "Publishes AccountOpenedEvent to Kafka."
    )
    public ResponseEntity<AccountResponse> openAccount(
            @Valid @RequestBody OpenAccountRequest request) {

        Account account = accountService.openAccount(
                request.customerId(),
                Account.AccountType.valueOf(request.accountType()),
                request.currency()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(AccountResponse.from(account));
    }

    @GetMapping("/{accountId}")
    @Operation(
        summary = "Get account by ID",
        description = "Called by BFF to get account details"
    )
    public ResponseEntity<AccountResponse> getAccount(@PathVariable String accountId) {
        return ResponseEntity.ok(AccountResponse.from(accountService.findById(accountId)));
    }

    @GetMapping
    @Operation(summary = "Get accounts by customer ID")
    public ResponseEntity<List<AccountResponse>> getAccountsByCustomer(
            @RequestParam String customerId) {
        List<AccountResponse> accounts = accountService.findByCustomerId(customerId)
                .stream().map(AccountResponse::from).toList();
        return ResponseEntity.ok(accounts);
    }

    @GetMapping("/all")
    @Operation(summary = "Get all accounts")
    public ResponseEntity<List<AccountResponse>> getAllAccounts() {
        return ResponseEntity.ok(accountService.findAll().stream()
                .map(AccountResponse::from).toList());
    }

    @PostMapping("/{accountId}/deposit")
    @Operation(
        summary = "Deposit money",
        description = "Increases balance. Publishes MoneyDepositedEvent to Kafka."
    )
    public ResponseEntity<AccountResponse> deposit(
            @PathVariable String accountId,
            @Valid @RequestBody TransactionRequest request) {

        Account account = accountService.deposit(accountId, request.amount(), request.description());
        return ResponseEntity.ok(AccountResponse.from(account));
    }

    @PostMapping("/{accountId}/withdraw")
    @Operation(
        summary = "Withdraw money",
        description = "Decreases balance. Business rule: no overdraft. " +
                      "Returns 422 if insufficient funds."
    )
    public ResponseEntity<?> withdraw(
            @PathVariable String accountId,
            @Valid @RequestBody TransactionRequest request) {
        try {
            Account account = accountService.withdraw(accountId, request.amount(), request.description());
            return ResponseEntity.ok(AccountResponse.from(account));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(Map.of("error", e.getMessage(), "code", "INSUFFICIENT_FUNDS"));
        }
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleException(Exception e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", e.getMessage()));
    }
}

// ----------------------------------------------------------------
// DTOs
// ----------------------------------------------------------------
record OpenAccountRequest(
        @NotBlank String customerId,
        @NotBlank String accountType,  // SAVINGS, CURRENT, MURABAHA, IJARA
        @NotBlank String currency
) {}

record TransactionRequest(
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        String description
) {}

record AccountResponse(
        String accountId,
        String customerId,
        String accountNumber,
        String accountType,
        String status,
        BigDecimal balance,
        String currency,
        String openedAt
) {
    static AccountResponse from(Account a) {
        return new AccountResponse(
                a.getAccountId(), a.getCustomerId(),
                a.getAccountNumber(), a.getAccountType().name(),
                a.getStatus().name(), a.getBalance(),
                a.getCurrency(), a.getOpenedAt().toString()
        );
    }
}
