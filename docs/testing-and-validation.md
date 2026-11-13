# Testing and Validation — E-Commerce Microservices v2

This document records the integration experiments performed during development.

## 1. RabbitMQ Event Fan-Out

**Status: Passed**

Published an order-created event to the `order.events` topic exchange.

Verified that the same event ID was received independently by:

- Payment Service
- Notification Service
- Audit Service

## 2. Redis-Based Payment Idempotency

**Status: Passed**

Published the same event 10 times to `payment.queue`.

| Metric | Result |
|---|---:|
| Total deliveries | 10 |
| Processing executions | 1 |
| Duplicate deliveries skipped | 9 |

Verified the Redis event state as `COMPLETED`.

## 3. Concurrent Payment Consumers

**Status: Passed**

Configured:

- Initial concurrency: 3
- Maximum concurrency: 5
- Prefetch: 1

Verified three active consumers on `payment.queue`.

## 4. Inventory Concurrency Experiment

**Status: Passed**

Initial inventory: 10 laptops.

Two concurrent customers requested 8 units each.

| Customer | Requested | Result |
|---|---:|---|
| Customer A | 8 | SUCCESS |
| Customer B | 8 | HTTP 409 Conflict |

This verified that the single Product Service instance did not accept both reservations.

## 5. Infrastructure Health

**Status: Passed**

Redis:

`docker exec ecommerce-redis redis-cli ping`

Expected result: `PONG`

RabbitMQ:

`docker exec ecommerce-rabbitmq rabbitmq-diagnostics -q ping`

Expected result: `Ping succeeded`

## 6. RabbitMQ Topology

Verified:

- `order.events`
- `payment.queue`
- `notification.queue`
- `audit.queue`
- `payment.dlx`
- `payment.dlq`

## 7. Implemented but Not Yet Fully Validated

- Full retry-to-DLQ-to-replay lifecycle.
- End-to-end failure recovery.
- Distributed inventory concurrency across multiple service instances.

## 8. Known Limitations

- Inventory is stored in memory.
- Stock reservation synchronization is single-instance only.
- Payment processing is simulated.
- Orders are not database-persisted.
- Transactional Outbox Pattern is not implemented.

These experiments demonstrate the current implementation, not a claim of production readiness.