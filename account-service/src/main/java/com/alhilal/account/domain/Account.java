package com.alhilal.account.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

// ================================================================
// ACCOUNT ENTITY — Aggregate Root for Account domain
// ================================================================
@Entity
@Table(name = "accounts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account {

    @Id
    private String accountId;

    // Only stores customerId — NOT the full Customer object
    // If we need customer name, we call customer-service API
    // This is "Database per Service" principle
    private String customerId;

    private String accountNumber;

    @Enumerated(EnumType.STRING)
    private AccountType accountType;

    @Enumerated(EnumType.STRING)
    private AccountStatus status;

    private BigDecimal balance;
    private String currency;
    private LocalDateTime openedAt;

    // ----------------------------------------------------------------
    // FACTORY METHOD — only way to open an account
    // ----------------------------------------------------------------
    public static Account open(String customerId, AccountType type, String currency) {
        return Account.builder()
                .accountId("ACC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .customerId(customerId)
                .accountNumber("AE" + System.currentTimeMillis())
                .accountType(type)
                .status(AccountStatus.ACTIVE)
                .balance(BigDecimal.ZERO)
                .currency(currency)
                .openedAt(LocalDateTime.now())
                .build();
    }

    // ----------------------------------------------------------------
    // BUSINESS METHODS — rules enforced here, not in controller
    // ----------------------------------------------------------------
    public void deposit(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Deposit amount must be positive");
        if (status != AccountStatus.ACTIVE)
            throw new AccountNotActiveException("Account is not active");

        this.balance = this.balance.add(amount);
    }

    public void withdraw(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Amount must be positive");
        if (status != AccountStatus.ACTIVE)
            throw new AccountNotActiveException("Account is not active");

        // Business rule: no overdraft (Islamic banking principle)
        if (this.balance.compareTo(amount) < 0)
            throw new InsufficientFundsException(
                "Insufficient funds. Balance: " + balance + ", Requested: " + amount);

        this.balance = this.balance.subtract(amount);
    }

    public enum AccountType {
        SAVINGS, CURRENT,
        MURABAHA,   // Islamic cost-plus financing
        IJARA,      // Islamic lease
        MUDARABA    // Islamic profit-sharing
    }

    public enum AccountStatus { ACTIVE, SUSPENDED, CLOSED }
}

