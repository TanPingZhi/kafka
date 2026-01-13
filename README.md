# Kafka Transactional Messaging Application

A Spring Boot application demonstrating transactional Kafka messaging with Spring Cloud Stream.

## Features

- **Swagger UI** for API interaction
- **Transactional publishing** to staging queues (atomicity guaranteed)
- **Guaranteed delivery** from staging to public queues
- **Rollback on failure** when "FAIL" message is encountered
- **Fully Dockerized** with Kafka, Zookeeper, and Kafka UI

## Architecture

```
┌─────────────┐     ┌──────────────────────────────────────┐
│  Swagger UI │────▶│          REST Controller             │
└─────────────┘     └──────────────┬───────────────────────┘
                                   │
                    ┌──────────────▼───────────────────────┐
                    │    MessagePublisherService           │
                    │    (@Transactional)                  │
                    └──────────────┬───────────────────────┘
                                   │
              ┌────────────────────┼────────────────────┐
              │                    │                    │
              ▼                    ▼                    │
        ┌──────────┐         ┌──────────┐              │
        │ stagingA │         │ stagingB │              │
        └────┬─────┘         └────┬─────┘              │
             │                    │                    │
             ▼                    ▼                    │
    ┌────────────────┐   ┌────────────────┐           │
    │ stagingATo     │   │ stagingBTo     │           │
    │ PublicA        │   │ PublicB        │           │
    │ (Transactional)│   │ (Transactional)│           │
    └────────┬───────┘   └────────┬───────┘           │
             │                    │                    │
             ▼                    ▼                    │
        ┌──────────┐         ┌──────────┐              │
        │ publicA  │         │ publicB  │              │
        └──────────┘         └──────────┘              │
```

## Quick Start

### Prerequisites

- Docker and Docker Compose installed

### Running the Application

1. **Start all services:**
   ```bash
   docker-compose up --build
   ```

2. **Access the applications:**
   - **Swagger UI**: http://localhost:8080/swagger-ui.html
   - **Kafka UI**: http://localhost:8081

3. **Test the API:**
   
   **Happy path:**
   ```bash
   curl -X POST http://localhost:8080/api/messages \
     -H "Content-Type: application/json" \
     -d '{"messages": ["message1", "message2", "message3"]}'
   ```

   **Rollback scenario:**
   ```bash
   curl -X POST http://localhost:8080/api/messages \
     -H "Content-Type: application/json" \
     -d '{"messages": ["message1", "FAIL", "message3"]}'
   ```
   This will trigger a transaction rollback - no messages will be published.

### Stopping the Application

```bash
docker-compose down
```

To also remove volumes:
```bash
docker-compose down -v
```

## Topics

| Topic     | Description                          |
|-----------|--------------------------------------|
| stagingA  | Staging queue for channel A          |
| stagingB  | Staging queue for channel B          |
| publicA   | Public queue for channel A           |
| publicB   | Public queue for channel B           |

## Configuration

Key configuration in `application.yml`:

- **Kafka Transactions**: Enabled via `transaction-id-prefix`
- **Consumer Isolation**: `read_committed` ensures only committed messages are read
- **Content Type**: `application/json` for all messages

## Development

### Building locally (requires Java 17 and Maven)

```bash
mvn clean package
```

### Running without Docker

1. Start Kafka locally on port 9092
2. Update `application.yml` bootstrap servers to `localhost:9092`
3. Run:
   ```bash
   mvn spring-boot:run
   ```
