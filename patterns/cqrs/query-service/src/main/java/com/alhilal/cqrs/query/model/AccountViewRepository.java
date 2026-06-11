package com.alhilal.cqrs.query.model;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AccountViewRepository
    extends JpaRepository<AccountView, String> {
    List<AccountView> findByCustomerId(String customerId);
}
