package com.alhilal.eventsourcing.model;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface AccountEventRepository
        extends JpaRepository<AccountEvent, Long> {

    // Get ALL events for an account (for replay)
    List<AccountEvent> findByAccountIdOrderByOccurredAtAsc(
        String accountId);

    // Get events up to a point in time (for time travel!)
    List<AccountEvent> findByAccountIdAndOccurredAtBeforeOrderByOccurredAtAsc(
        String accountId, LocalDateTime before);
}
