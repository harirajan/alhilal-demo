package com.alhilal.cqrs.command.service;

import com.alhilal.cqrs.command.event.AccountEvent;
import com.alhilal.cqrs.command.model.Account;
import com.alhilal.cqrs.command.model.AccountRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountCommandService {

    private final AccountRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private static final String TOPIC = "cqrs.account.events";

    // ─── CREATE ACCOUNT ──────────────────────────────────
    @Transactional
    public Account createAccount(String customerId,
                                  String accountType,
                                  String currency) throws Exception {
        Account account = new Account();
        account.setAccountId("ACC-" + UUID.randomUUID()
            .toString().substring(0, 8).toUpperCase());
        account.setCustomerId(customerId);
        account.setAccountType(accountType);
        account.setBalance(0.0);
        account.setCurrency(currency);
        account.setStatus("ACTIVE");
        repository.save(account);

        log.info("[CQRS-COMMAND] Account created: {}", account.getAccountId());

        // Publish event → query-service will update READ DB
        publishEvent("CREATED", account, 0.0);
        return account;
    }

    // ─── DEPOSIT ─────────────────────────────────────────
    @Transactional
    public Account deposit(String accountId, Double amount) throws Exception {
        Account account = repository.findById(accountId)
            .orElseThrow(() -> new RuntimeException("Account not found: " + accountId));

        account.deposit(amount);
        repository.save(account);

        log.info("[CQRS-COMMAND] Deposited {} to {} newBalance={}",
            amount, accountId, account.getBalance());

        publishEvent("DEPOSITED", account, amount);
        return account;
    }

    // ─── WITHDRAW ────────────────────────────────────────
    @Transactional
    public Account withdraw(String accountId, Double amount) throws Exception {
        Account account = repository.findById(accountId)
            .orElseThrow(() -> new RuntimeException("Account not found: " + accountId));

        account.withdraw(amount);  // throws if insufficient
        repository.save(account);

        log.info("[CQRS-COMMAND] Withdrew {} from {} newBalance={}",
            amount, accountId, account.getBalance());

        publishEvent("WITHDRAWN", account, amount);
        return account;
    }

    // ─── PUBLISH EVENT ───────────────────────────────────
    private void publishEvent(String eventType,
                               Account account,
                               Double amount) throws Exception {
        AccountEvent event = new AccountEvent(
            eventType,
            account.getAccountId(),
            account.getCustomerId(),
            account.getAccountType(),
            amount,
            account.getBalance(),
            account.getCurrency(),
            account.getStatus(),
            System.currentTimeMillis()
        );

        kafkaTemplate.send(TOPIC, account.getAccountId(),
            objectMapper.writeValueAsString(event));

        log.info("[CQRS-COMMAND] → Published {} event for {}",
            eventType, account.getAccountId());
    }
}
