package com.alhilal.saga.orchestrator.model;

import lombok.Data;

@Data
public class SagaState {
    private String sagaId;
    private String fromAccount;
    private String toAccount;
    private Double amount;
    private String customerId;
    private SagaStatus status;
    private String currentStep;

    public enum SagaStatus {
        STARTED,
        DEBIT_REQUESTED,
        DEBITED,
        CREDIT_REQUESTED,
        COMPLETED,
        FAILED
    }
}
