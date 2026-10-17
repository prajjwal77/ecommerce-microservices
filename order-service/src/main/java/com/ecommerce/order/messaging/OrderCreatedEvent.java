package com.ecommerce.order.messaging;

public record OrderCreatedEvent(
        String eventId,
        String eventType,
        Long productId,
        String productName,
        int quantity,
        double totalAmount
) {}
