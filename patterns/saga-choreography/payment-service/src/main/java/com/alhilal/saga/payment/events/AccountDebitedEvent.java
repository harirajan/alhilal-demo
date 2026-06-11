package com.alhilal.saga.payment.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AccountDebitedEvent {
    private String sagaId;
    private String accountId;
    private Double amount;
    private Double newBalance;
}
