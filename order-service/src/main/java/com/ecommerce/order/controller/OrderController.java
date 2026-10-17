package com.ecommerce.order.controller;

import com.ecommerce.order.client.ProductClient;
import com.ecommerce.order.dto.CreateOrderRequest;
import com.ecommerce.order.dto.OrderResponse;
import com.ecommerce.order.dto.ProductResponse;
import com.ecommerce.order.service.OrderService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    public ProductResponse getProduct(
            @PathVariable Long productId) {

        return productClient.getProduct(productId);
    }

    @PostMapping
    public OrderResponse createOrder(
            @RequestBody CreateOrderRequest request) {

        return orderService.createOrder(request);
    }
}