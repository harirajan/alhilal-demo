package com.alhilal.customer.api;

import java.time.LocalDateTime;
import java.util.List;

// ----------------------------------------------------------------
// AUTH CREDENTIAL ENTITY — stored in customer-service DB for demo
// In production: separate auth-service with its own DB
// ----------------------------------------------------------------
@jakarta.persistence.Entity
@jakarta.persistence.Table(name = "auth_credentials")
@lombok.Getter
@lombok.Setter
@lombok.NoArgsConstructor
public class AuthCredential {

    @jakarta.persistence.Id
    private String customerId;

    @jakarta.persistence.Column(unique = true)
    private String email;

    private String passwordHash;

    @jakarta.persistence.ElementCollection(fetch = jakarta.persistence.FetchType.EAGER)
    private List<String> roles;

    private int failedAttempts = 0;
    private boolean accountLocked = false;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
}
