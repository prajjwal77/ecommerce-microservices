package com.ecommerce.order.client;

import com.ecommerce.order.dto.ProductResponse;
import com.ecommerce.order.exception.ProductServiceException;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class ProductClient {

        private final RestClient restClient;

        public ProductClient(RestClient restClient) {
                this.restClient = restClient;
        }

        @CircuitBreaker(name = "productService", fallbackMethod = "productServiceFallback")
        @Retryable(retryFor = ProductServiceException.class, maxAttempts = 3, backoff = @Backoff(delay = 5000, multiplier = 2))
        public ProductResponse getProduct(Long productId) {

                try {

                        return restClient.get()
                                        .uri(
                                                        "http://localhost:8083/api/products/{id}",
                                                        productId)
                                        .retrieve()
                                        .body(ProductResponse.class);

                } catch (Exception e) {

                        throw new ProductServiceException(
                                        "Product Service is unavailable",
                                        e);
                }
        }

        public ProductResponse productServiceFallback(
                        Long productId,
                        Throwable throwable) {

                throw new ProductServiceException(
                                "Product Service is currently unavailable. " +
                                                "Circuit breaker is open.",
                                throwable);
        }

        public ProductResponse reserveProduct(Long productId, int quantity) {

                return restClient.post()
                                .uri(
                                                "http://localhost:8083/api/products/{id}/reserve?quantity={quantity}",
                                                productId,
                                                quantity)
                                .retrieve()
                                .body(ProductResponse.class);
        }

        public void releaseStock(Long productId, int quantity) {

                restClient.post()
                                .uri(
                                                "http://localhost:8083/api/products/{id}/release?quantity={quantity}",
                                                productId,
                                                quantity)
                                .retrieve()
                                .toBodilessEntity();
        }

}