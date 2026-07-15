package com.example.accountservice;

import com.example.accountservice.controller.AccountController;
import com.example.accountservice.model.Transaction;
import com.example.accountservice.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountController.class)
class AccountControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TransactionRepository repository;

    @Test
    void appliesTransactionAndReturnsCreated() throws Exception {
        Transaction tx = new Transaction();
        tx.setEventId("evt-1");
        tx.setType("CREDIT");
        tx.setAmount(100.0);
        tx.setCurrency("USD");
        tx.setEventTimestamp(Instant.parse("2026-05-15T14:02:11Z"));

        when(repository.existsByEventId("evt-1")).thenReturn(false);
        when(repository.save(any(Transaction.class))).thenReturn(tx);
        when(repository.findByEventId("evt-1")).thenReturn(Optional.of(tx));

        mockMvc.perform(post("/accounts/acct-1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventId\":\"evt-1\",\"type\":\"CREDIT\",\"amount\":100.0,\"currency\":\"USD\",\"eventTimestamp\":\"2026-05-15T14:02:11Z\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void rejectsTransactionWithoutTimestamp() throws Exception {
        mockMvc.perform(post("/accounts/acct-1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventId\":\"evt-1\",\"type\":\"CREDIT\",\"amount\":100.0,\"currency\":\"USD\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void ignoresDuplicateTransaction() throws Exception {
        Transaction existing = new Transaction();
        existing.setEventId("evt-2");
        existing.setType("DEBIT");
        existing.setAmount(40.0);
        existing.setCurrency("USD");
        existing.setEventTimestamp(Instant.parse("2026-05-15T14:02:11Z"));

        when(repository.existsByEventId("evt-2")).thenReturn(true);
        when(repository.findByEventId("evt-2")).thenReturn(Optional.of(existing));

        mockMvc.perform(post("/accounts/acct-1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventId\":\"evt-2\",\"type\":\"DEBIT\",\"amount\":40.0,\"currency\":\"USD\",\"eventTimestamp\":\"2026-05-15T14:02:11Z\"}"))
                .andExpect(status().isOk());
    }
}
