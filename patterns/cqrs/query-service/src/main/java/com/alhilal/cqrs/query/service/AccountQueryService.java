package com.alhilal.cqrs.query.service;

import com.alhilal.cqrs.query.model.AccountView;
import com.alhilal.cqrs.query.model.AccountViewRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountQueryService {

    private final AccountViewRepository repository;
    private final ObjectMapper objectMapper;

    // ─── CONSUME EVENTS → UPDATE READ DB ─────────────────
    @KafkaListener(topics = "cqrs.account.events",
                   groupId = "cqrs-query-service")
    public void onAccountEvent(String payload) throws Exception {
        var event = objectMapper.readValue(payload, Map.class);
        String eventType  = (String) event.get("eventType");
        String accountId  = (String) event.get("accountId");
        Double amount     = ((Number) event.get("amount")).doubleValue();
        Double newBalance = ((Number) event.get("newBalance")).doubleValue();

        log.info("[CQRS-QUERY] ← Event received: {} for {}",
            eventType, accountId);

        switch (eventType) {
            case "CREATED" -> handleCreated(event);
            case "DEPOSITED" -> handleDeposited(accountId, amount, newBalance);
            case "WITHDRAWN" -> handleWithdrawn(accountId, amount, newBalance);
        }
    }

    private void handleCreated(Map<?, ?> event) {
        AccountView view = new AccountView();
        view.setAccountId((String) event.get("accountId"));
        view.setCustomerId((String) event.get("customerId"));
        view.setAccountType((String) event.get("accountType"));
        view.setBalance(((Number) event.get("newBalance")).doubleValue());
        view.setCurrency((String) event.get("currency"));
        view.setStatus((String) event.get("status"));
        view.setLastUpdated(((Number) event.get("timestamp")).longValue());
        view.setTotalTransactions(0);
        view.setTotalDeposited(0.0);
        view.setTotalWithdrawn(0.0);
        repository.save(view);
        log.info("[CQRS-QUERY] ✓ READ DB: account created {}", view.getAccountId());
    }

    private void handleDeposited(String accountId,
                                  Double amount,
                                  Double newBalance) {
        repository.findById(accountId).ifPresent(view -> {
            view.setBalance(newBalance);
            view.setTotalTransactions(view.getTotalTransactions() + 1);
            view.setTotalDeposited(view.getTotalDeposited() + amount);
            view.setLastUpdated(System.currentTimeMillis());
            repository.save(view);
            log.info("[CQRS-QUERY] ✓ READ DB: deposited {} balance={}",
                amount, newBalance);
        });
    }

    private void handleWithdrawn(String accountId,
                                  Double amount,
                                  Double newBalance) {
        repository.findById(accountId).ifPresent(view -> {
            view.setBalance(newBalance);
            view.setTotalTransactions(view.getTotalTransactions() + 1);
            view.setTotalWithdrawn(view.getTotalWithdrawn() + amount);
            view.setLastUpdated(System.currentTimeMillis());
            repository.save(view);
            log.info("[CQRS-QUERY] ✓ READ DB: withdrawn {} balance={}",
                amount, newBalance);
        });
    }

    // ─── QUERY METHODS ───────────────────────────────────
    public Optional<AccountView> getAccount(String accountId) {
        return repository.findById(accountId);
    }

    public List<AccountView> getByCustomer(String customerId) {
        return repository.findByCustomerId(customerId);
    }

    public List<AccountView> getAllAccounts() {
        return repository.findAll();
    }
}
