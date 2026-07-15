package com.example.eventgateway;

import com.example.eventgateway.controller.EventController;
import com.example.eventgateway.model.Event;
import com.example.eventgateway.repository.EventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EventController.class)
class EventControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EventRepository repository;

    @MockBean
    private RestTemplate restTemplate;

    @Test
    void acceptsValidEvent() throws Exception {
        Event event = new Event();
        event.setEventId("evt-1");
        event.setAccountId("acct-1");
        event.setType("CREDIT");
        event.setAmount(150.0);
        event.setCurrency("USD");
        event.setEventTimestamp(Instant.parse("2026-05-15T14:02:11Z"));

        when(repository.existsByEventId("evt-1")).thenReturn(false);
        when(repository.save(any(Event.class))).thenReturn(event);
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Object.class))).thenReturn(ResponseEntity.ok().build());

        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventId\":\"evt-1\",\"accountId\":\"acct-1\",\"type\":\"CREDIT\",\"amount\":150.0,\"currency\":\"USD\",\"eventTimestamp\":\"2026-05-15T14:02:11Z\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void rejectsEventWithoutTimestamp() throws Exception {
        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventId\":\"evt-1\",\"accountId\":\"acct-1\",\"type\":\"CREDIT\",\"amount\":150.0,\"currency\":\"USD\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returnsExistingEventForDuplicateEventId() throws Exception {
        Event existing = new Event();
        existing.setEventId("evt-1");
        existing.setAccountId("acct-1");
        existing.setType("CREDIT");
        existing.setAmount(150.0);
        existing.setCurrency("USD");
        existing.setEventTimestamp(Instant.parse("2026-05-15T14:02:11Z"));

        when(repository.existsByEventId("evt-1")).thenReturn(true);
        when(repository.findByEventId("evt-1")).thenReturn(existing);

        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventId\":\"evt-1\",\"accountId\":\"acct-1\",\"type\":\"CREDIT\",\"amount\":150.0,\"currency\":\"USD\",\"eventTimestamp\":\"2026-05-15T14:02:11Z\"}"))
                .andExpect(status().isOk());
        verify(restTemplate, never()).postForEntity(anyString(), any(HttpEntity.class), eq(Object.class));
    }

    @Test
    void returnsServiceUnavailableWhenAccountServiceFails() throws Exception {
        Event event = new Event();
        event.setEventId("evt-2");
        event.setAccountId("acct-1");
        event.setType("CREDIT");
        event.setAmount(150.0);
        event.setCurrency("USD");
        event.setEventTimestamp(Instant.parse("2026-05-15T14:02:11Z"));

        when(repository.existsByEventId("evt-2")).thenReturn(false);
        when(repository.save(any(Event.class))).thenReturn(event);
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Object.class))).thenThrow(new RestClientException("down"));

        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventId\":\"evt-2\",\"accountId\":\"acct-1\",\"type\":\"CREDIT\",\"amount\":150.0,\"currency\":\"USD\",\"eventTimestamp\":\"2026-05-15T14:02:11Z\"}"))
                .andExpect(status().isServiceUnavailable());
    }
}
