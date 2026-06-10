package com.alhilal.account.domain;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AccountService {

    private final AccountRepository repository;
    private final AccountEventPublisher eventPublisher;

    public Account openAccount(String customerId, Account.AccountType type, String currency) {
        Account account = Account.open(customerId, type, currency);
        Account saved = repository.save(account);
        eventPublisher.publishAccountOpened(saved);
        return saved;
    }

    public Account deposit(String accountId, BigDecimal amount, String description) {
        Account account = findById(accountId);
        account.deposit(amount);
        Account saved = repository.save(account);
        eventPublisher.publishMoneyDeposited(saved, amount, description);
        return saved;
    }

    public Account withdraw(String accountId, BigDecimal amount, String description) {
        Account account = findById(accountId);
        account.withdraw(amount);
        Account saved = repository.save(account);
        eventPublisher.publishMoneyWithdrawn(saved, amount, description);
        return saved;
    }

    @Transactional(readOnly = true)
    public Account findById(String accountId) {
        return repository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountId));
    }

    @Transactional(readOnly = true)
    public List<Account> findByCustomerId(String customerId) {
        return repository.findByCustomerId(customerId);
    }

    @Transactional(readOnly = true)
    public List<Account> findAll() {
        return repository.findAll();
    }
}