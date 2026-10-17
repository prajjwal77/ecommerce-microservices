package com.ecommerce.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayRoutesConfig {

    @Bean
    public RouteLocator gatewayRoutes(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("user-service", route -> route
                        .path("/api/users/**")
                        .uri("http://localhost:8081"))
                .route("product-service", route -> route
                        .path("/api/products/**")
                        .uri("http://localhost:8083"))
                .route("order-service", route -> route
                        .path("/api/orders/**")
                        .uri("http://localhost:8082"))
                .build();
    }
}
