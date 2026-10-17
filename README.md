# E-Commerce Microservices Starter v2

Clean starter for learning Spring Boot microservices, API Gateway, REST routing and RabbitMQ.

## Versions
- Java 25
- Spring Boot 4.1.1
- Spring Cloud 2025.1.3
- Spring Cloud Gateway 5.0.3 (managed by the Spring Cloud BOM)
- RabbitMQ 4

## Important
Use JDK 25 for this project. Check with:

```bash
java -version
mvn -version
```

## Build
From this directory:

```bash
mvn clean install
```

## Start RabbitMQ

```bash
docker compose up -d
```

RabbitMQ UI: http://localhost:15672
Username: admin
Password: admin

## Start services
Open separate terminals:

```bash
mvn -pl user-service spring-boot:run
mvn -pl product-service spring-boot:run
mvn -pl order-service spring-boot:run
mvn -pl payment-service spring-boot:run
mvn -pl notification-service spring-boot:run
mvn -pl audit-service spring-boot:run
mvn -pl api-gateway spring-boot:run
```

## Ports
- Gateway: 8080
- User: 8081
- Order: 8082
- Product: 8083
- Payment: 8084
- Notification: 8085
- Audit: 8086

## Gateway routing
Gateway routes are deliberately defined in Java using `RouteLocator` so there is no ambiguity about YAML route-property binding.

- `/api/users/**` -> `http://localhost:8081`
- `/api/products/**` -> `http://localhost:8083`
- `/api/orders/**` -> `http://localhost:8082`

## Gateway tests

```bash
curl http://localhost:8080/api/users/10
curl http://localhost:8080/api/users/health
curl http://localhost:8080/api/products/10
curl http://localhost:8080/api/orders/health
```

Expected first response:

```text
user 10
```

## RabbitMQ test

```bash
curl -X POST http://localhost:8080/api/orders \
  -H 'Content-Type: text/plain' \
  -d '{"productId":101,"quantity":2}'
```

Order Service publishes `order.created` to `order.events`. Payment, Notification and Audit consume the event from their own queues.

## Architecture

Client -> API Gateway -> User/Product/Order services

Order -> RabbitMQ exchange -> Payment/Notification/Audit queues

This starter intentionally does not include JWT, MySQL, Redis, Eureka, DLQ, retry, outbox or Kubernetes yet. Those will be added incrementally after the base routing and messaging flow is verified.

## Phase 3 — Service-to-Service REST Communication

Order Service now calls Product Service synchronously using Spring RestClient.

Flow:

Client -> API Gateway :8080 -> Order Service :8082 -> Product Service :8083

Order Service structure:

- controller/ — HTTP endpoints
- service/ — business logic
- client/ — calls to other microservices
- dto/ — request/response models
- config/ — Spring configuration
- messaging/ — RabbitMQ publishing

Test:

- GET http://localhost:8082/api/orders/product/1
- GET http://localhost:8080/api/orders/product/1
- POST http://localhost:8080/api/orders

Example request:

{"productId":1,"quantity":2}
