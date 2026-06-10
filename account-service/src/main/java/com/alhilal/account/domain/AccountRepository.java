package com.alhilal.account.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, String> {
    List<Account> findByCustomerId(String customerId);
    Optional<Account> findByAccountNumber(String accountNumber);
}