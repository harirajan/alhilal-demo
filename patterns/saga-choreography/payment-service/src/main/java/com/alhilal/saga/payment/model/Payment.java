package com.alhilal.saga.payment.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "payments")
public class Payment {

    @Id
    private String sagaId;
    private String fromAccount;
    private String toAccount;
    private Double amount;
    private String currency;
    private String customerId;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    public enum PaymentStatus {
        INITIATED,
        DEBIT_PENDING,
        DEBITED,
        COMPLETED,
        FAILED
    }
}
