package com.alhilal.eventsourcing.service;

import com.alhilal.eventsourcing.model.AccountEvent;
import com.alhilal.eventsourcing.model.AccountEvent.EventType;
import com.alhilal.eventsourcing.model.AccountEventRepository;
import com.alhilal.eventsourcing.model.AccountState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventStoreService {

    private final AccountEventRepository repository;

    // ─── CREATE ACCOUNT ──────────────────────────────────
    @Transactional
    public AccountEvent createAccount(String customerId,
                                       String currency) {
        String accountId = "ES-ACC-" + UUID.randomUUID()
            .toString().substring(0, 8).toUpperCase();

        // Store the CREATION EVENT — not current state!
        AccountEvent event = new AccountEvent();
        event.setAccountId(accountId);
        event.setCustomerId(customerId);
        event.setEventType(EventType.ACCOUNT_CREATED);
        event.setAmount(0.0);
        event.setBalanceAfter(0.0);
        event.setCurrency(currency);
        event.setOccurredAt(LocalDateTime.now());
        event.setDescription("Account created for " + customerId);
        repository.save(event);

        log.info("[EVENT-STORE] ACCOUNT_CREATED accountId={}", accountId);
        return event;
    }

    // ─── DEPOSIT ─────────────────────────────────────────
    @Transactional
    public AccountEvent deposit(String accountId,
                                 Double amount,
                                 String description) {
        // Get current state by replaying events
        AccountState currentState = getAccountState(accountId);
        Double newBalance = currentState.getBalance() + amount;

        // Store DEPOSIT EVENT — never update existing events!
        AccountEvent event = new AccountEvent();
        event.setAccountId(accountId);
        event.setCustomerId(currentState.getCustomerId());
        event.setEventType(EventType.MONEY_DEPOSITED);
        event.setAmount(amount);
        event.setBalanceAfter(newBalance);
        event.setCurrency(currentState.getCurrency());
        event.setOccurredAt(LocalDateTime.now());
        event.setDescription(description != null
            ? description : "Deposit of " + amount);
        repository.save(event);

        log.info("[EVENT-STORE] MONEY_DEPOSITED accountId={} " +
                 "amount={} newBalance={}", accountId, amount, newBalance);
        return event;
    }

    // ─── WITHDRAW ────────────────────────────────────────
    @Transactional
    public AccountEvent withdraw(String accountId,
                                  Double amount,
                                  String description) {
        AccountState currentState = getAccountState(accountId);

        if (currentState.getBalance() < amount) {
            throw new IllegalStateException(
                "Insufficient funds: balance="
                + currentState.getBalance()
                + " required=" + amount);
        }

        Double newBalance = currentState.getBalance() - amount;

        AccountEvent event = new AccountEvent();
        event.setAccountId(accountId);
        event.setCustomerId(currentState.getCustomerId());
        event.setEventType(EventType.MONEY_WITHDRAWN);
        event.setAmount(amount);
        event.setBalanceAfter(newBalance);
        event.setCurrency(currentState.getCurrency());
        event.setOccurredAt(LocalDateTime.now());
        event.setDescription(description != null
            ? description : "Withdrawal of " + amount);
        repository.save(event);

        log.info("[EVENT-STORE] MONEY_WITHDRAWN accountId={} " +
                 "amount={} newBalance={}", accountId, amount, newBalance);
        return event;
    }

    // ─── GET CURRENT STATE (replay all events) ───────────
    public AccountState getAccountState(String accountId) {
        List<AccountEvent> events = repository
            .findByAccountIdOrderByOccurredAtAsc(accountId);

        if (events.isEmpty()) {
            throw new RuntimeException(
                "Account not found: " + accountId);
        }

        log.info("[EVENT-STORE] Replaying {} events for {}",
            events.size(), accountId);
        return AccountState.replay(events);
    }

    // ─── TIME TRAVEL — get state at any point in time ────
    public AccountState getStateAt(String accountId,
                                    LocalDateTime at) {
        List<AccountEvent> events = repository
            .findByAccountIdAndOccurredAtBeforeOrderByOccurredAtAsc(
                accountId, at);

        if (events.isEmpty()) {
            throw new RuntimeException(
                "No events found for " + accountId + " before " + at);
        }

        log.info("[EVENT-STORE] Time travel: replaying {} events " +
                 "for {} up to {}", events.size(), accountId, at);
        return AccountState.replay(events);
    }

    // ─── GET ALL EVENTS (audit trail) ────────────────────
    public List<AccountEvent> getHistory(String accountId) {
        return repository
            .findByAccountIdOrderByOccurredAtAsc(accountId);
    }
}
