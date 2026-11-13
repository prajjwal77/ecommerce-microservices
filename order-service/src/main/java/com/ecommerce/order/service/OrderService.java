
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

                ProductResponse product;

                // Step 1: Reserve stock before creating the order.
                try {
                        product = productClient.reserveProduct(
                                        request.getProductId(),
                                        request.getQuantity());
                } catch (org.springframework.web.client.HttpClientErrorException.Conflict ex) {
                        throw new org.springframework.web.server.ResponseStatusException(
                                        org.springframework.http.HttpStatus.CONFLICT,
                                        "Insufficient stock",
                                        ex);
                }

                // Step 2: Calculate order amount.
                double totalAmount = product.getPrice() * request.getQuantity();

                // Step 3: Generate a unique event ID.
                OrderCreatedEvent event = new OrderCreatedEvent(
                                UUID.randomUUID().toString(),
                                "ORDER_CREATED",
                                product.getId(),
                                product.getName(),
                                request.getQuantity(),
                                totalAmount);

                // Step 4: Publish event.
                try {

                        orderEventPublisher.publish(event);

                        return new OrderResponse(
                                        product.getId(),
                                        product.getName(),
                                        request.getQuantity(),
                                        product.getPrice(),
                                        totalAmount,
                                        "CREATED");

                } catch (RuntimeException ex) {

                        // Step 5: Compensate if event publishing fails.
                        try {
                                productClient.releaseStock(
                                                product.getId(),
                                                request.getQuantity());
                        } catch (RuntimeException releaseException) {
                                ex.addSuppressed(releaseException);
                        }

                        throw ex;
                }
        }

}
