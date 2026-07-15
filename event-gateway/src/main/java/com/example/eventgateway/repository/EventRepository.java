package com.example.eventgateway.repository;

import com.example.eventgateway.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {
    boolean existsByEventId(String eventId);
    Event findByEventId(String eventId);
    List<Event> findAllByOrderByEventTimestampAsc();
    List<Event> findByAccountIdOrderByEventTimestampAsc(String accountId);
}
