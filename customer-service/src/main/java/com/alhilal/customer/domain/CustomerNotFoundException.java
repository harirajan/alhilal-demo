package com.alhilal.customer.domain;

// ----------------------------------------------------------------
// DOMAIN EXCEPTIONS — named after business concepts
// ----------------------------------------------------------------
class CustomerNotFoundException extends RuntimeException {
    public CustomerNotFoundException(String message) {
        super(message);
    }
}
