package com.ecommerce.product.controller;

import java.util.List;

import com.ecommerce.product.model.Product;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final List<Product> products = List.of(
            new Product(1L, "Laptop", 75000, 10),
            new Product(2L, "Keyboard", 2000, 25),
            new Product(3L, "Mouse", 1000, 50)
    );

    @GetMapping("/{id}")
    public Product getProduct(@PathVariable Long id) {

        return products.stream()
                .filter(product -> product.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Product not found"));
    }
}