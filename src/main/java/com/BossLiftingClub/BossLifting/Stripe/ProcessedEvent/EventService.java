package com.BossLiftingClub.BossLifting.Stripe.ProcessedEvent;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class EventService {
    private final ProcessedEventRepository processedEventRepository;

    public EventService(ProcessedEventRepository processedEventRepository) {
        this.processedEventRepository = processedEventRepository;
    }

    public boolean isEventProcessed(String eventId) {
        return processedEventRepository.existsByEventId(eventId);
    }

    @Transactional
    public void markEventProcessed(String eventId) {
        processedEventRepository.save(new ProcessedEvent(eventId, LocalDateTime.now()));
    }
}