package com.BossLiftingClub.BossLifting.Stripe.ProcessedEvent;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, String> {
    boolean existsByEventId(String eventId);
}