package com.alhilal.cqrs.command.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "accounts")
public class Account {

    @Id
    private String accountId;
    private String customerId;
    private String accountType;
    private Double balance;
    private String currency;
    private String status;

    public void deposit(Double amount) {
        this.balance += amount;
    }

    public void withdraw(Double amount) {
        if (this.balance < amount) {
            throw new IllegalStateException(
                "Insufficient funds: balance=" + balance + " required=" + amount);
        }
        this.balance -= amount;
    }
}
