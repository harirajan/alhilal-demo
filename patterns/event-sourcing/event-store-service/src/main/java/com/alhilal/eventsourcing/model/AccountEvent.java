package com.alhilal.eventsourcing.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "account_events")
public class AccountEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String accountId;
    private String customerId;

    @Enumerated(EnumType.STRING)
    private EventType eventType;

    private Double amount;
    private Double balanceAfter;  // snapshot for convenience
    private String currency;
    private LocalDateTime occurredAt;
    private String description;

    public enum EventType {
        ACCOUNT_CREATED,
        MONEY_DEPOSITED,
        MONEY_WITHDRAWN,
        ACCOUNT_CLOSED
    }
}
