package com.alhilal.cqrs.command.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AccountEvent {
    private String eventType;    // CREATED, DEPOSITED, WITHDRAWN
    private String accountId;
    private String customerId;
    private String accountType;
    private Double amount;
    private Double newBalance;
    private String currency;
    private String status;
    private long timestamp;
}
