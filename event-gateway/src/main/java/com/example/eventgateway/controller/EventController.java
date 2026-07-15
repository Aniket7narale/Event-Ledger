package com.example.eventgateway.controller;

import com.example.eventgateway.model.Event;
import com.example.eventgateway.repository.EventRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
public class EventController {
    private final EventRepository repository;
    private final RestTemplate restTemplate;

    public EventController(EventRepository repository, RestTemplate restTemplate) {
        this.repository = repository;
        this.restTemplate = restTemplate;
    }

    @PostMapping("/events")
    @CircuitBreaker(name = "accountService", fallbackMethod = "accountFallback")
    public ResponseEntity<?> submitEvent(@RequestHeader(value = "X-Trace-Id", required = false) String incomingTraceId,
                                         @RequestBody Event event) {
        String traceId = incomingTraceId != null && !incomingTraceId.isBlank() ? incomingTraceId : UUID.randomUUID().toString();
        Logging.log("info", traceId, "submitEvent invoked");

        if (event == null || event.getEventId() == null || event.getEventId().isBlank()) {
            Logging.log("warn", traceId, "validation failed: missing eventId");
            return ResponseEntity.badRequest().body(Map.of("error", "eventId is required"));
        }
        if (event.getAccountId() == null || event.getAccountId().isBlank()) {
            Logging.log("warn", traceId, "validation failed: missing accountId");
            return ResponseEntity.badRequest().body(Map.of("error", "accountId is required"));
        }
        if (event.getType() == null || (!"CREDIT".equals(event.getType()) && !"DEBIT".equals(event.getType()))) {
            Logging.log("warn", traceId, "validation failed: invalid event type");
            return ResponseEntity.badRequest().body(Map.of("error", "type must be CREDIT or DEBIT"));
        }
        if (event.getAmount() <= 0) {
            Logging.log("warn", traceId, "validation failed: amount must be > 0");
            return ResponseEntity.badRequest().body(Map.of("error", "amount must be greater than zero"));
        }
        if (event.getCurrency() == null || event.getCurrency().isBlank()) {
            Logging.log("warn", traceId, "validation failed: missing currency");
            return ResponseEntity.badRequest().body(Map.of("error", "currency is required"));
        }
        if (event.getEventTimestamp() == null) {
            Logging.log("warn", traceId, "validation failed: missing eventTimestamp");
            return ResponseEntity.badRequest().body(Map.of("error", "eventTimestamp is required"));
        }
        if (repository.existsByEventId(event.getEventId())) {
            Event existing = repository.findByEventId(event.getEventId());
            Logging.log("info", traceId, "idempotent duplicate event returned");
            return ResponseEntity.ok(existing);
        }

        Event saved = repository.save(event);

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Trace-Id", traceId);
        Map<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("eventId", saved.getEventId());
        payload.put("accountId", saved.getAccountId());
        payload.put("type", saved.getType());
        payload.put("amount", saved.getAmount());
        payload.put("currency", saved.getCurrency());
        payload.put("eventTimestamp", saved.getEventTimestamp() != null ? saved.getEventTimestamp().toString() : null);
        payload.put("metadata", saved.getMetadata());
        try {
            restTemplate.postForEntity("http://localhost:8081/accounts/" + saved.getAccountId() + "/transactions",
                    new HttpEntity<>(payload, headers), Object.class);
            Logging.log("info", traceId, "forwarded event to account service");
        } catch (Exception ex) {
            Logging.log("error", traceId, "account service call failed: " + ex.getMessage());
            return accountFallback(traceId, saved, ex);
        }

        return ResponseEntity.created(URI.create("/events/" + saved.getEventId())).body(saved);
    }

    public ResponseEntity<?> accountFallback(String traceId, Event event, Throwable throwable) {
        Logging.log("warn", traceId, "circuit breaker fallback triggered");
        return ResponseEntity.status(503).body(Map.of("status", "SERVICE_UNAVAILABLE", "error", "Account Service unavailable", "traceId", traceId));
    }

    @GetMapping("/events/{id}")
    public ResponseEntity<?> getEvent(@PathVariable String id) {
        Event event = repository.findByEventId(id);
        if (event == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(event);
    }

    @GetMapping("/events")
    public ResponseEntity<?> listEvents(@RequestParam(name = "account", required = false) String account) {
        List<Event> events = account == null || account.isBlank()
                ? repository.findAllByOrderByEventTimestampAsc()
                : repository.findByAccountIdOrderByEventTimestampAsc(account);
        return ResponseEntity.ok(events);
    }

    @GetMapping("/accounts/{accountId}/balance")
    public ResponseEntity<?> balance(@PathVariable String accountId) {
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity("http://localhost:8081/accounts/" + accountId + "/balance", Map.class);
            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (Exception ex) {
            return ResponseEntity.status(503).body(Map.of("status", "SERVICE_UNAVAILABLE", "error", "Account Service unavailable"));
        }
    }

    @GetMapping("/health")
    public ResponseEntity<?> health() {
        boolean dbHealthy;
        try {
            repository.count();
            dbHealthy = true;
        } catch (Exception ex) {
            dbHealthy = false;
        }
        return ResponseEntity.ok(Map.of("status", dbHealthy ? "UP" : "DOWN", "service", "event-gateway", "database", dbHealthy ? "UP" : "DOWN"));
    }

    @GetMapping("/metrics")
    public ResponseEntity<?> metrics() {
        return ResponseEntity.ok(Map.of("service", "event-gateway", "events", repository.count()));
    }
}
