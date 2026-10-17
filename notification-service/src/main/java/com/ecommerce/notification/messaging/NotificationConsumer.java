package com.ecommerce.notification.messaging;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationConsumer {
    @RabbitListener(queues = "notification.queue")
    public void consume(String message) {
        System.out.println("Notification Service received: " + message);
    }
}
