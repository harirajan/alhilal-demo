package com.alhilal.eventsourcing.api;

import com.alhilal.eventsourcing.model.AccountEvent;
import com.alhilal.eventsourcing.model.AccountState;
import com.alhilal.eventsourcing.service.EventStoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/v1/es/accounts")
@RequiredArgsConstructor
public class EventStoreController {

    private final EventStoreService service;

    @PostMapping
    public ResponseEntity<AccountEvent> create(
            @RequestBody CreateRequest req) {
        return ResponseEntity.ok(
            service.createAccount(req.customerId(), req.currency()));
    }

    @PostMapping("/{accountId}/deposit")
    public ResponseEntity<AccountEvent> deposit(
            @PathVariable String accountId,
            @RequestBody TransactionRequest req) {
        return ResponseEntity.ok(
            service.deposit(accountId, req.amount(), req.description()));
    }

    @PostMapping("/{accountId}/withdraw")
    public ResponseEntity<AccountEvent> withdraw(
            @PathVariable String accountId,
            @RequestBody TransactionRequest req) {
        return ResponseEntity.ok(
            service.withdraw(accountId, req.amount(), req.description()));
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<AccountState> getState(
            @PathVariable String accountId) {
        return ResponseEntity.ok(service.getAccountState(accountId));
    }

    // TIME TRAVEL — accepts format: 2026-06-11T15:30:00
    @GetMapping("/{accountId}/balance-at")
    public ResponseEntity<AccountState> getStateAt(
            @PathVariable String accountId,
            @RequestParam String at) {
        LocalDateTime dateTime = LocalDateTime.parse(at,
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
        return ResponseEntity.ok(service.getStateAt(accountId, dateTime));
    }

    @GetMapping("/{accountId}/history")
    public ResponseEntity<List<AccountEvent>> getHistory(
            @PathVariable String accountId) {
        return ResponseEntity.ok(service.getHistory(accountId));
    }

    public record CreateRequest(String customerId, String currency) {}
    public record TransactionRequest(Double amount, String description) {}
}
