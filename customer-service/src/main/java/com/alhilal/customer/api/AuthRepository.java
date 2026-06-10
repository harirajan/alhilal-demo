package com.alhilal.customer.api;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

// ----------------------------------------------------------------
// REPOSITORY
// ----------------------------------------------------------------
@Repository
public interface AuthRepository extends JpaRepository<AuthCredential, String> {
    Optional<AuthCredential> findByEmail(String email);
}
