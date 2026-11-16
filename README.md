# E-Commerce Microservices Starter v2

A learning-oriented, event-driven E-Commerce Microservices application built using Spring Boot, Spring Cloud Gateway, RabbitMQ, and Redis.

The project demonstrates microservice communication, API Gateway routing, inventory management, asynchronous event processing, idempotency, retry mechanisms, and Dead Letter Queue handling.

## Technology Stack

- Java 25
- Spring Boot 4.1.1
- Spring Cloud 2025.1.3
- Spring Cloud Gateway 5.0.3
- RabbitMQ 4
- Redis 7
- Maven 3.9.11
- Docker

## Prerequisites

Install the following:

- JDK 25
- Maven 3.9.11 or compatible version
- Docker Desktop
- Git

Verify your environment:

```bash
java -version
mvn -version
docker --version
git --version
```

## Project Architecture

```text
                         CLIENT
                            |
                            v
                     API GATEWAY
                       :8080
                            |
          +-----------------+-----------------+
          |                 |                 |
          v                 v                 v
     USER SERVICE     PRODUCT SERVICE    ORDER SERVICE
        :8081              :8083             :8082
                                                |
                                  +-------------+-------------+
                                  |                           |
                                  v                           v
                            PRODUCT SERVICE              RABBITMQ
                            (REST API)                 order.events
                                                             |
                                           +-----------------+-----------------+
                                           |                 |                 |
                                           v                 v                 v
                                     PAYMENT SERVICE   NOTIFICATION SERVICE  AUDIT SERVICE
                                          | 
                                          v
                                         REDIS
                                      Idempotency

                                     PAYMENT DLQ
```

## Microservices

| Service | Port | Responsibility |
|---|---:|---|
| API Gateway | 8080 | Central entry point and request routing |
| User Service | 8081 | User-related APIs |
| Order Service | 8082 | Order creation and business logic |
| Product Service | 8083 | Product information and stock reservation |
| Payment Service | 8084 | Payment event consumption and simulated processing |
| Notification Service | 8085 | Order event notifications |
| Audit Service | 8086 | Order event auditing |

Payment, Notification, and Audit are consumer-oriented services. Their configured ports do not necessarily indicate active HTTP servers.

## Gateway Routing

Gateway routes are defined using Java `RouteLocator` configuration.

| Gateway Endpoint | Target Service |
|---|---|
| `/api/users/**` | `http://localhost:8081` |
| `/api/products/**` | `http://localhost:8083` |
| `/api/orders/**` | `http://localhost:8082` |

## Phase 1 — API Gateway and Microservices Setup

Implemented:

- Spring Boot multi-module Maven architecture.
- Independent microservices.
- API Gateway using Spring Cloud Gateway.
- Centralized request routing.
- Separate application configurations and service ports.

## Phase 2 — RabbitMQ Event-Driven Communication

Implemented asynchronous communication using RabbitMQ.

**Exchange:**

```text
order.events
```

**Routing key:**

```text
order.created
```

**Queues:**

```text
payment.queue
notification.queue
audit.queue
```

When an order is successfully created, Order Service publishes an `ORDER_CREATED` event.

Payment, Notification, and Audit Services independently consume the event from their respective queues.

Event example:

```json
{
  "eventId": "unique-event-id",
  "eventType": "ORDER_CREATED",
  "productId": 2,
  "productName": "Keyboard",
  "quantity": 1,
  "totalAmount": 2000.0
}
```

This implements event fan-out: one published event is delivered to multiple independent consumers.

## Phase 3 — Service-to-Service REST Communication

Order Service communicates synchronously with Product Service using Spring `RestClient`.

**Communication flow:**

```text
Client
  |
  v
API Gateway :8080
  |
  v
Order Service :8082
  |
  v
Product Service :8083
```

Implemented:

- Product information retrieval.
- Product availability validation.
- Synchronous REST communication.
- Request and response DTOs.
- Separation of controller, service, client, and messaging layers.

**Test endpoints:**

```http
GET http://localhost:8082/api/orders/product/1

GET http://localhost:8080/api/orders/product/1

POST http://localhost:8080/api/orders
```

Example request:

```json
{
  "productId": 1,
  "quantity": 2
}
```

## Phase 4 — Inventory Reservation and Concurrency Handling

Implemented stock reservation in Product Service.

Previously, multiple concurrent orders could read the same available stock and both succeed, resulting in overselling.

To address this, Product Service now provides synchronized stock reservation and release operations.

**New endpoints:**

```http
POST /api/products/{id}/reserve?quantity=2

POST /api/products/{id}/release?quantity=2
```

Implemented:

- Stock availability validation.
- Atomic reservation within a single Product Service instance using synchronized methods.
- Stock decrement after successful reservation.
- HTTP 409 Conflict when sufficient stock is unavailable.
- Stock release when event publishing fails.

**Concurrency scenario:**

Initial stock:

```text
Laptop stock = 10
```

Two customers simultaneously request:

```text
Customer A: 8 units
Customer B: 8 units
```

Expected outcome:

```text
Customer A: SUCCESS
Customer B: HTTP 409 CONFLICT

Remaining stock: 2
```

This demonstrates protection against concurrent overselling within a single running Product Service instance.

## Phase 5 — Redis-Based Payment Idempotency

Integrated Redis to prevent duplicate processing of the same payment event.

Redis stores event-processing state using the event ID.

**Redis key format:**

```text
payment:event:{eventId}
```

Implemented:

- Atomic event-processing claims using Redis `SET NX`.
- Processing state management.
- Duplicate event detection.
- Completion state management.
- Lua scripts for safe state transitions.
- Processing claim expiration.
- Completed event retention.

**Processing flow:**

```text
RabbitMQ Message
       |
       v
Payment Consumer
       |
       v
Check Redis Event ID
       |
       +----------------------+
       |                      |
       v                      v
Already Processed        New Event
       |                      |
       v                      v
Skip Duplicate          Claim Event
                              |
                              v
                       Process Payment
                              |
                              v
                       Mark COMPLETED
```

A repeated event with the same event ID is skipped instead of being processed again.

A different event ID represents a different order event, even when the product ID is identical.

## Phase 6 — Payment Consumer Concurrency and Retry

Configured multiple concurrent RabbitMQ consumers in Payment Service.

Current configuration:

```yaml
spring:
  rabbitmq:
    listener:
      simple:
        concurrency: 3
        max-concurrency: 5
        prefetch: 1
        default-requeue-rejected: false
        retry:
          enabled: true
          initial-interval: 1s
          max-retries: 2
          multiplier: 2.0
          max-interval: 5s
```

Implemented:

- Multiple concurrent payment consumers.
- Configurable consumer concurrency.
- Retry with exponential backoff.
- Controlled message rejection.
- Redis idempotency protection during duplicate deliveries.

## Phase 7 — Dead Letter Queue (DLQ)

Implemented a Dead Letter Queue for failed payment messages.

**Dead Letter Exchange:**

```text
payment.dlx
```

**Dead Letter Queue:**

```text
payment.dlq
```

**Routing key:**

```text
payment.failed
```

Implemented:

- Payment queue dead-letter configuration.
- Failed message routing.
- Dedicated DLQ.
- Payment DLQ replay service.
- Support for retrying failed messages.

**Message flow:**

```text
          PAYMENT QUEUE
                |
                v
         PAYMENT CONSUMER
                |
                v
         PAYMENT PROCESSING
                |
          +-----+-----+
          |           |
          v           v
       SUCCESS      FAILURE
          |           |
          v           v
      COMPLETED    RETRY
                      |
                      v
               MESSAGE REJECTED
                      |
                      v
               payment.dlx
                      |
                      v
                payment.dlq
```

## Phase 8 — Notification and Audit Services

Implemented independent RabbitMQ consumers.

**Notification Service**

- Consumes order-created events.
- Processes order notification messages.
- Logs received order details.

**Audit Service**

- Consumes order-created events.
- Records order event details in application logs.

Both services operate independently of Payment Service.

## Infrastructure Setup

### Start RabbitMQ

```bash
docker compose up -d
```

RabbitMQ Management UI:

```text
http://localhost:15672
```

Credentials (local development configuration):

```text
Username: admin
Password: admin
```

### Start Redis

Redis runs on:

```text
localhost:6379
```

Verify Redis:

```bash
docker exec ecommerce-redis redis-cli ping
```

Expected:

```text
PONG
```

Verify RabbitMQ:

```bash
docker exec ecommerce-rabbitmq rabbitmq-diagnostics -q ping
```

Expected:

```text
Ping succeeded
```

## Running the Application

Open separate terminals from the project root.

```bash
mvn -pl user-service spring-boot:run
```

```bash
mvn -pl product-service spring-boot:run
```

```bash
mvn -pl order-service spring-boot:run
```

```bash
mvn -pl payment-service spring-boot:run
```

```bash
mvn -pl notification-service spring-boot:run
```

```bash
mvn -pl audit-service spring-boot:run
```

```bash
mvn -pl api-gateway spring-boot:run
```

Build the complete project:

```bash
mvn clean install -DskipTests
```

## Testing

### 1. Gateway and REST Communication

```bash
curl http://localhost:8080/api/products/1
```

```bash
curl http://localhost:8080/api/orders/product/1
```

### 2. Create an Order

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{"productId":1,"quantity":2}'
```

### 3. Check Inventory

```bash
curl http://localhost:8083/api/products/1
```

### 4. Verify Redis Idempotency

```bash
docker exec ecommerce-redis redis-cli KEYS "payment:event:*"
```

Check a specific event:

```bash
docker exec ecommerce-redis redis-cli GET payment:event:YOUR_EVENT_ID
```

Expected completed state:

```text
COMPLETED
```

### 5. Verify RabbitMQ Queues

Open:

```text
http://localhost:15672
```

Check:

- `payment.queue`
- `notification.queue`
- `audit.queue`
- `payment.dlq`

Verify message delivery and consumer activity.

## Important Implementation Notes

This project is a learning and demonstration environment, not a production-ready e-commerce platform.

Current limitations:

- Product inventory is maintained in memory.
- Synchronized stock reservation protects only a single Product Service instance.
- Inventory is reset when Product Service restarts.
- Stock release does not yet use durable reservation IDs or transactional reservation records.
- Payment processing is simulated; no external payment gateway is integrated.
- Orders are not persisted in a database.
- Notification and Audit currently demonstrate event consumption through application logs.
- HTTP order-creation idempotency is not implemented.
- RabbitMQ publishing and inventory rollback are not a fully transactional outbox workflow.
- JWT authentication, MySQL persistence, Eureka, Kubernetes, and transactional outbox are not implemented.

Redis-based payment event idempotency and RabbitMQ retry/DLQ handling are implemented.

## Future Enhancements

- MySQL integration for persistent inventory and orders.
- Database-level concurrency control and distributed stock reservation.
- Order API idempotency.
- Transactional Outbox Pattern.
- Spring Security and JWT authentication.
- External payment gateway integration.
- Persistent notification and audit records.
- Service discovery.
- Distributed tracing and centralized logging.
- Kubernetes deployment.

## Learning Outcomes

Through this project, the following backend engineering concepts are demonstrated:

- Microservices architecture.
- API Gateway and request routing.
- Synchronous REST communication.
- Asynchronous event-driven architecture.
- RabbitMQ exchanges, queues, and routing keys.
- Redis-based idempotency.
- Concurrent message consumers.
- Inventory concurrency control.
- Retry and exponential backoff.
- Dead Letter Queue and message replay.
- Inter-service communication and failure handling.

---

**Project:** E-Commerce Microservices Starter v2  
**Repository:** https://github.com/prajjwal77/ecommerce-microservices  
**Branch:** main

## Architecture & Technical Documentation

Explore the engineering design and implementation details:

- [System Architecture](docs/architecture.md)
- [Testing & Validation Results](docs/testing-and-validation.md)

### Engineering Highlights

- Event-driven communication with RabbitMQ.
- Redis-based payment idempotency.
- Concurrent inventory reservation.
- Multiple RabbitMQ consumers.
- Retry, Dead Letter Exchange and Dead Letter Queue.
- Synchronous and asynchronous microservice communication.