
package com.ecommerce.order.controller;

import com.ecommerce.order.client.ProductClient;
import com.ecommerce.order.dto.CreateOrderRequest;
import com.ecommerce.order.dto.OrderResponse;
import com.ecommerce.order.dto.ProductResponse;
import com.ecommerce.order.service.OrderService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final ProductClient productClient;
    private final OrderService orderService;

    public OrderController(
            ProductClient productClient,
            OrderService orderService) {

        this.productClient = productClient;
        this.orderService = orderService;
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<?> getProduct(
            @PathVariable Long productId) {

        try {
            ProductResponse product =
                    productClient.getProduct(productId);

            return ResponseEntity.ok(product);

        } catch (HttpClientErrorException ex) {

            return ResponseEntity
                    .status(ex.getStatusCode())
                    .body(ex.getResponseBodyAsString());
        }
    }

    @PostMapping
    public OrderResponse createOrder(
            @RequestBody CreateOrderRequest request) {

        return orderService.createOrder(request);
    }
}
