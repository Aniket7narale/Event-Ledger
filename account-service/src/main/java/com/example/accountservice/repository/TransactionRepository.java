package com.example.accountservice.repository;

import com.example.accountservice.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Optional<Transaction> findByEventId(String eventId);
    boolean existsByEventId(String eventId);
    List<Transaction> findByAccountIdOrderByEventTimestampAsc(String accountId);

    @Query("SELECT COALESCE(SUM(CASE WHEN t.type = 'CREDIT' THEN t.amount WHEN t.type = 'DEBIT' THEN -t.amount ELSE 0 END), 0) FROM Transaction t WHERE t.accountId = :accountId")
    Double computeBalance(@Param("accountId") String accountId);
}
