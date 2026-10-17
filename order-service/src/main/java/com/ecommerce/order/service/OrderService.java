
package com.ecommerce.order.service;

import com.ecommerce.order.client.ProductClient;
import com.ecommerce.order.dto.CreateOrderRequest;
import com.ecommerce.order.dto.OrderResponse;
import com.ecommerce.order.dto.ProductResponse;
import com.ecommerce.order.messaging.OrderCreatedEvent;
import com.ecommerce.order.messaging.OrderEventPublisher;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class OrderService {

    private final ProductClient productClient;
    private final OrderEventPublisher orderEventPublisher;

    public OrderService(
            ProductClient productClient,
            OrderEventPublisher orderEventPublisher) {
        this.productClient = productClient;
        this.orderEventPublisher = orderEventPublisher;
    }

    public OrderResponse createOrder(CreateOrderRequest request) {

        ProductResponse product =
                productClient.getProduct(request.getProductId());

        if (product.getStock() < request.getQuantity()) {
            throw new IllegalArgumentException("Insufficient stock");
        }

        double totalAmount =
                product.getPrice() * request.getQuantity();

        OrderCreatedEvent event = new OrderCreatedEvent(
                UUID.randomUUID().toString(),
                "ORDER_CREATED",
                product.getId(),
                product.getName(),
                request.getQuantity(),
                totalAmount
        );

        orderEventPublisher.publish(event);

        return new OrderResponse(
                product.getId(),
                product.getName(),
                request.getQuantity(),
                product.getPrice(),
                totalAmount,
                "CREATED"
        );
    }
}
