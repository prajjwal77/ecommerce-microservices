package com.ecommerce.user.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @GetMapping("/health")
    public String health() {
        return "user-service is running";
    }

    @GetMapping("/{id}")
    public String getUser(@PathVariable Long id) {
        return "user " + id;
    }
}
