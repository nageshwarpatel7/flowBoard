package com.flowboard.notification_service;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class NotificationServiceApplicationTests {

    @MockBean ConnectionFactory connectionFactory;
    @MockBean RabbitTemplate rabbitTemplate;
    @MockBean JavaMailSender javaMailSender;
    @MockBean SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory;

    @Test
    void contextLoads() {
    }
}