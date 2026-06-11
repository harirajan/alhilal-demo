package com.alhilal.saga.payment.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentInitiatedEvent {
    private String sagaId;
    private String fromAccount;
    private String toAccount;
    private Double amount;
    private String currency;
    private String customerId;
}
