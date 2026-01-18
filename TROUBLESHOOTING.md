# Troubleshooting & Prevention Notes

This document summarizes the technical challenges encountered during the Spring Boot 3.0.0 downgrade and provides strategies for preventing REST connectivity errors in the future.

## 1. The Spring Boot 3.0.0 Bug
**Issue**: Compilation failure in `KafkaConfig.java` after downgrading.

### Technical Root Cause:
In Spring Boot 3.0.x/3.1.x, the `KafkaProperties.buildProducerProperties(AdminClientConfigCustomizer)` method expects a parameter. In later versions (like 3.2.x), it was overloaded or changed. Specifically, in 3.0.0, the standard way to build properties is:
```java
// Spring Boot 3.0.0
Map<String, Object> configs = properties.buildProducerProperties();
```
Passing `null` was causing a signature mismatch error.

### Solution:
Removed the `null` parameter. This is a common pitfall when moving between minor/major versions of Spring Kafka.

---

## 2. Kafka Connectivity (DNS Resolution)
**Issue**: `org.apache.kafka.common.config.ConfigException: No resolvable bootstrap urls given in bootstrap.servers`.

### Technical Root Cause:
This occurs when the Kafka broker is defined with `ADVERTISED_LISTENERS` that the application cannot reach or resolve. In Docker Compose:
1. The app container tries to connect to `kafka:9092`.
2. If the Kafka container hasn't fully started or the bridge network hasn't registered the `kafka` alias, the DNS lookup fails.
3. Spring Boot's `KafkaAdmin` checks this on startup and crashes the app immediately if it fails.

### Solution:
Standardized the `docker-compose.yml` listeners:
- `KAFKA_LISTENERS`: Defines what ports the broker *listens* on.
- `KAFKA_ADVERTISED_LISTENERS`: Defines what address the broker *tells* clients to use.
Using `PLAINTEXT://kafka:9092` for internal Docker traffic and `PLAINTEXT_HOST://localhost:29092` for host machine traffic is the reliable pattern.

---

## 3. Preventing REST Request Failures
To avoid the "Connection Refused" or "Connection Reset" errors seen during testing, follow these best practices:

### A. Wait for "Started" Logs
Don't send requests as soon as the container is "Running". Docker says "Running" as soon as the process starts, but Java/Tomcat takes 10-20 seconds to bind to the port.
> **Fix**: Always verify the "Started [ApplicationName] in X seconds" log message before testing.

### B. Use JSON Object Wrappers
The `MessageController` expects a `MessageRequest` DTO, not a raw list of strings.
- **Wrong**: `["msg1", "msg2"]` -> Returns 400 Bad Request.
- **Correct**: `{"messages": ["msg1", "msg2"]}`.

### C. PowerShell Syntax Precision
When using `Invoke-RestMethod` in PowerShell:
- Always use `-ContentType "application/json"`.
- Ensure strings in the body are single-quoted to preserve the JSON double quotes.
- Use `127.0.0.1` instead of `localhost` to avoid IPv6 resolution delays on some Windows systems.

### D. Health Checks
In `docker-compose.yml`, use `depends_on` with `condition: service_healthy`. This ensures the App doesn't even try to start until Kafka is actually ready to accept connections.
