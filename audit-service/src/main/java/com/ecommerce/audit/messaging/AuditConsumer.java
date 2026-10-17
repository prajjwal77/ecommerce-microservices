package com.ecommerce.audit.messaging;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class AuditConsumer {
    @RabbitListener(queues = "audit.queue")
    public void consume(String message) {
        System.out.println("Audit Service received: " + message);
    }
}
