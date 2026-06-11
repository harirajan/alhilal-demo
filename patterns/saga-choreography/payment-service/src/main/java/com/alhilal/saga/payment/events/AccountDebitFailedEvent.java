package com.alhilal.saga.payment.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AccountDebitFailedEvent {
    private String sagaId;
    private String accountId;
    private String reason;
}
