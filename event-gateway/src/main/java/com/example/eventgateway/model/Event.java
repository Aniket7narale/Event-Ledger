package com.example.eventgateway.model;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "events")
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String eventId;
    private String accountId;
    private String type;
    private double amount;
    private String currency;
    private Instant eventTimestamp;
    @Lob
    private String metadata;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public Instant getEventTimestamp() { return eventTimestamp; }
    public void setEventTimestamp(Instant eventTimestamp) { this.eventTimestamp = eventTimestamp; }
    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
}
