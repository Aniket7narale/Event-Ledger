package com.example.accountservice.controller;

import com.example.accountservice.model.Transaction;
import com.example.accountservice.repository.TransactionRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/accounts")
public class AccountController {
    private final TransactionRepository repository;

    public AccountController(TransactionRepository repository) {
        this.repository = repository;
    }

    @PostMapping("/{accountId}/transactions")
    public ResponseEntity<?> applyTransaction(@PathVariable String accountId,
                                              @RequestHeader(value = "X-Trace-Id", required = false) String traceId,
                                              @RequestBody Transaction transaction) {
        if (transaction == null || transaction.getEventId() == null || transaction.getEventId().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "eventId is required"));
        }
        if (transaction.getType() == null || (!"CREDIT".equals(transaction.getType()) && !"DEBIT".equals(transaction.getType()))) {
            return ResponseEntity.badRequest().body(Map.of("error", "type must be CREDIT or DEBIT"));
        }
        if (transaction.getAmount() <= 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "amount must be greater than zero"));
        }
        if (transaction.getCurrency() == null || transaction.getCurrency().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "currency is required"));
        }
        if (transaction.getEventTimestamp() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "eventTimestamp is required"));
        }
        if (repository.existsByEventId(transaction.getEventId())) {
            Logging.log("info", traceId, "duplicate event ignored: " + transaction.getEventId());
            return ResponseEntity.ok(repository.findByEventId(transaction.getEventId()).orElse(transaction));
        }

        transaction.setAccountId(accountId);
        Transaction saved = repository.save(transaction);
        Logging.log("info", traceId, "applied transaction for account " + accountId);
        return ResponseEntity.created(URI.create("/accounts/" + accountId + "/transactions/" + saved.getEventId())).body(saved);
    }

    @GetMapping("/{accountId}/balance")
    public ResponseEntity<?> balance(@PathVariable String accountId) {
        Double balance = repository.computeBalance(accountId);
        if (balance == null) balance = 0.0;
        return ResponseEntity.ok(Map.of("accountId", accountId, "balance", balance));
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<?> account(@PathVariable String accountId) {
        Double balance = repository.computeBalance(accountId);
        if (balance == null) balance = 0.0;
        List<Transaction> transactions = repository.findByAccountIdOrderByEventTimestampAsc(accountId);
        Map<String, Object> response = new HashMap<>();
        response.put("accountId", accountId);
        response.put("balance", balance);
        response.put("transactions", transactions);
        return ResponseEntity.ok(response);
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
        return ResponseEntity.ok(Map.of("status", dbHealthy ? "UP" : "DOWN", "service", "account-service", "database", dbHealthy ? "UP" : "DOWN"));
    }

    @GetMapping("/metrics")
    public ResponseEntity<?> metrics() {
        return ResponseEntity.ok(Map.of("service", "account-service", "transactions", repository.count()));
    }
}
