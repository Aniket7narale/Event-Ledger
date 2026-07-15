# Event Ledger

Event Ledger is a Java-based event processing system composed of two Spring Boot microservices:

- Event Gateway: the public-facing entry point for client requests
- Account Service: the internal service that persists transactions and computes balances

The system is designed to handle duplicate events, out-of-order arrival, validation failures, trace propagation, and temporary downstream unavailability.

## Architecture Overview

```text
Client / Browser
        |
        v
Event Gateway API (port 8080)
        |
        | REST / HTTP
        v
Account Service (port 8081)
```

### High-Level Design

1. The Gateway receives incoming event submissions via POST /events.
2. It validates the payload, enforces idempotency, and stores the event locally.
3. It forwards accepted events to the Account Service over REST.
4. The Account Service applies the transaction to its own independent database and computes balances.
5. Both services emit structured JSON logs with a trace ID for request correlation.

## Components

### 1. Event Gateway
Responsibilities:
- Accept event submissions from clients
- Validate input fields such as eventId, accountId, type, amount, currency, and eventTimestamp
- Enforce idempotency using eventId
- Store event records in its own embedded H2 database
- Propagate trace IDs to the Account Service using the X-Trace-Id header
- Apply a Resilience4j circuit breaker to protect against account-service failures
- Expose endpoints for event lookup, listing, health, and metrics

### 2. Account Service
Responsibilities:
- Receive transaction application requests from the Gateway
- Persist transaction records in its own embedded H2 database
- Compute balances for each account
- Return account details and transaction history
- Log trace IDs for debugging and observability

## Functional Requirements Implemented

### Core Functionality
- Idempotency: submitting the same eventId more than once does not create a duplicate record or alter balance
- Out-of-order tolerance: events are listed in chronological order by eventTimestamp
- Balance computation: balance = sum of CREDITs - sum of DEBITs
- Validation: invalid or incomplete payloads are rejected with meaningful error messages

### Service Separation
- Each service runs independently
- Each service has its own embedded in-memory H2 database
- No shared database or in-process state is used

### Distributed Tracing
- Each incoming request to the Gateway generates or receives a trace ID
- The trace ID is propagated to the Account Service via the X-Trace-Id header
- Structured logs in both services include the trace ID

### Resilience
- The Gateway uses a circuit breaker around the account-service call
- If the Account Service is unavailable, the Gateway returns a graceful 503 Service Unavailable response

### Observability
- Structured JSON logging
- Health endpoints on both services
- Metrics endpoint on both services

## API Contracts

### Event Gateway
- POST /events
  - Submit a transaction event
- GET /events/{id}
  - Retrieve a single event by its ID
- GET /events?account={accountId}
  - List events for an account ordered by eventTimestamp
- GET /health
  - Health status for the gateway
- GET /metrics
  - Basic metrics information

### Account Service
- POST /accounts/{accountId}/transactions
  - Apply a transaction to an account
- GET /accounts/{accountId}/balance
  - Retrieve the current balance for an account
- GET /accounts/{accountId}
  - Retrieve account details and recent transactions
- GET /health
  - Health status for the account service
- GET /metrics
  - Basic metrics information

## Swagger / OpenAPI

Swagger UI is available when the services are running:

- Gateway: http://localhost:8080/swagger-ui/index.html
- Account Service: http://localhost:8081/swagger-ui/index.html

OpenAPI JSON is available at:

- Gateway: http://localhost:8080/v3/api-docs
- Account Service: http://localhost:8081/v3/api-docs

## API Details

### 1. Submit an event

Endpoint:

```http
POST /events
Content-Type: application/json
```

Example request body:

```json
{
  "eventId": "evt-001",
  "accountId": "acct-123",
  "type": "CREDIT",
  "amount": 150.0,
  "currency": "USD",
  "eventTimestamp": "2026-05-15T14:02:11Z",
  "metadata": {
    "source": "mainframe-batch",
    "batchId": "B-9042"
  }
}
```

Expected responses:
- `201 Created` for a new accepted event
- `200 OK` for a duplicate eventId (idempotent replay)
- `400 Bad Request` for invalid input
- `503 Service Unavailable` when the Account Service is unavailable and the circuit breaker is open

### 2. Retrieve one event

Endpoint:

```http
GET /events/{id}
```

Expected responses:
- `200 OK` with the stored event
- `404 Not Found` if the event does not exist

### 3. List events for an account

Endpoint:

```http
GET /events?account={accountId}
```

Expected responses:
- `200 OK` with a chronologically ordered list of events for the account

### 4. Get balance from the Account Service

Endpoint:

```http
GET /accounts/{accountId}/balance
```

Expected responses:
- `200 OK` with a JSON object containing the accountId and balance
- `503 Service Unavailable` if the Account Service is unreachable

### 5. Apply a transaction to an account

Endpoint:

```http
POST /accounts/{accountId}/transactions
Content-Type: application/json
```

Example request body:

```json
{
  "eventId": "evt-001",
  "type": "CREDIT",
  "amount": 100.0,
  "currency": "USD",
  "eventTimestamp": "2026-05-15T14:02:11Z"
}
```

Expected responses:
- `201 Created` for a newly stored transaction
- `200 OK` for a duplicate eventId
- `400 Bad Request` for invalid input

## Run Locally

### Prerequisites
- Java 17+
- Maven

### Start both services

```bash
mvn test
mvn -pl account-service -am spring-boot:run
mvn -pl event-gateway -am spring-boot:run
```

### Run with Docker Compose

```bash
docker compose up --build
```

## Testing

Run all tests with:

```bash
mvn test
```

## Notes

- The gateway uses Resilience4j circuit breaker protection for the downstream account-service call.
- Each service uses its own embedded H2 database.
- The system is designed for independent deployment and local development without needing an external database.
