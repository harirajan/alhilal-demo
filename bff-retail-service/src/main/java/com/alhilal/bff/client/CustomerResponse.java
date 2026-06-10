package com.alhilal.bff.client;

public record CustomerResponse(
        String customerId,
        String firstName,
        String lastName,
        String fullName,
        String email,
        String mobileNumber,
        String kycStatus,
        String createdAt
) {}