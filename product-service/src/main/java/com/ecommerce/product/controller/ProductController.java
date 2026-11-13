
package com.ecommerce.product.controller;

import java.util.List;

import com.ecommerce.product.model.Product;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final List<Product> products = List.of(
            new Product(1L, "Laptop", 75000, 10),
            new Product(2L, "Keyboard", 2000, 25),
            new Product(3L, "Mouse", 1000, 50)
    );

    @GetMapping("/{id}")
    public synchronized Product getProduct(@PathVariable Long id) {
        return findProduct(id);
    }

    @PostMapping("/{id}/reserve")
    public synchronized Product reserveStock(
            @PathVariable Long id,
            @RequestParam int quantity) {

        if (quantity < 1) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Quantity must be at least 1"
            );
        }

        Product product = findProduct(id);

        if (product.getStock() < quantity) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Insufficient stock. Available: " + product.getStock()
            );
        }

        // Atomically reserve stock within this Product Service instance.
        product.setStock(product.getStock() - quantity);

        return product;
    }

    @PostMapping("/{id}/release")
    public synchronized Product releaseStock(
            @PathVariable Long id,
            @RequestParam int quantity) {

        if (quantity < 1) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Quantity must be at least 1"
            );
        }

        Product product = findProduct(id);

        product.setStock(product.getStock() + quantity);

        return product;
    }

    private Product findProduct(Long id) {
        return products.stream()
                .filter(product -> product.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Product not found"
                ));
    }
}
