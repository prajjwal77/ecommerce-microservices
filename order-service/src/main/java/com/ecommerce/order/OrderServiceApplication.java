package com.ecommerce.order;

import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class OrderServiceApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context =
                SpringApplication.run(OrderServiceApplication.class, args);

        System.out.println("RabbitMQConfig loaded: "
                + context.containsBean("rabbitMQConfig"));

        System.out.println("paymentQueue bean exists: "
                + context.containsBean("paymentQueue"));

        System.out.println("notificationQueue bean exists: "
                + context.containsBean("notificationQueue"));

        System.out.println("auditQueue bean exists: "
                + context.containsBean("auditQueue"));

        System.out.println("AmqpAdmin bean exists: "
                + context.getBeansOfType(AmqpAdmin.class).size());
    }
}
