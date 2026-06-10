package com.alhilal.bff.client;

import java.math.BigDecimal;

public record AccountResponse(
        String accountId,
        String customerId,
        String accountNumber,
        String accountType,
        String status,
        BigDecimal balance,
        String currency,
        String openedAt
) {}