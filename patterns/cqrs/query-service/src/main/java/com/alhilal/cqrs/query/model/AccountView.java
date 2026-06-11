package com.alhilal.cqrs.query.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "account_views")
public class AccountView {

    @Id
    private String accountId;
    private String customerId;
    private String accountType;
    private Double balance;
    private String currency;
    private String status;
    private long lastUpdated;

    // Denormalized fields — pre-computed for fast reads
    private Integer totalTransactions;
    private Double totalDeposited;
    private Double totalWithdrawn;
}
