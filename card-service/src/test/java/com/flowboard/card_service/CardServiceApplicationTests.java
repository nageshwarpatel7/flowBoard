package com.flowboard.card_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

@SpringBootTest
@ActiveProfiles("test")
class CardServiceApplicationTests {

	@MockBean ConnectionFactory connectionFactory;
	@MockBean RabbitTemplate rabbitTemplate;

	@Test
	void contextLoads() {
	}

}
