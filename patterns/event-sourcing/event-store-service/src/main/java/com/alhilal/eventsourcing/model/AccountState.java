package com.alhilal.eventsourcing.model;

import lombok.Data;
import java.util.List;

@Data
public class AccountState {
    private String accountId;
    private String customerId;
    private Double balance;
    private String currency;
    private String status;
    private int totalEvents;
    private List<AccountEvent> eventHistory;

    // Rebuild state by replaying events
    public static AccountState replay(List<AccountEvent> events) {
        AccountState state = new AccountState();
        state.setEventHistory(events);
        state.setTotalEvents(events.size());

        for (AccountEvent event : events) {
            switch (event.getEventType()) {
                case ACCOUNT_CREATED -> {
                    state.setAccountId(event.getAccountId());
                    state.setCustomerId(event.getCustomerId());
                    state.setBalance(0.0);
                    state.setCurrency(event.getCurrency());
                    state.setStatus("ACTIVE");
                }
                case MONEY_DEPOSITED ->
                    state.setBalance(state.getBalance()
                        + event.getAmount());
                case MONEY_WITHDRAWN ->
                    state.setBalance(state.getBalance()
                        - event.getAmount());
                case ACCOUNT_CLOSED ->
                    state.setStatus("CLOSED");
            }
        }
        return state;
    }
}
