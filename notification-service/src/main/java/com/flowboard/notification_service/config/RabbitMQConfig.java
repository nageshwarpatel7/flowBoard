package com.flowboard.notification_service.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String CARD_ASSIGNMENT_QUEUE = "card.assignment.queue";
    public static final String DUE_DATE_QUEUE        = "card.duedate.queue";
    public static final String INVITE_QUEUE          = "workspace.invite.queue";

    public static final String FLOWBOARD_EXCHANGE    = "flowboard.exchange";

    public static final String ASSIGNMENT_KEY        = "card.assigned";
    public static final String DUE_DATE_KEY          = "card.due";
    public static final String INVITE_KEY            = "workspace.invite";

    @Bean
    public TopicExchange flowboardExchange() {
        return new TopicExchange(FLOWBOARD_EXCHANGE, true, false);
    }

    @Bean
    public Queue cardAssignmentQueue() {
        return QueueBuilder.durable(CARD_ASSIGNMENT_QUEUE)
                .withArgument("x-dead-letter-exchange",
                        FLOWBOARD_EXCHANGE + ".dlx")
                .build();
    }

    @Bean
    public Queue dueDateQueue() {
        return QueueBuilder.durable(DUE_DATE_QUEUE).build();
    }

    @Bean
    public Queue inviteQueue() {
        return QueueBuilder.durable(INVITE_QUEUE).build();
    }

    @Bean
    public Binding assignmentBinding() {
        return BindingBuilder
                .bind(cardAssignmentQueue())
                .to(flowboardExchange())
                .with(ASSIGNMENT_KEY);
    }

    @Bean
    public Binding dueDateBinding() {
        return BindingBuilder
                .bind(dueDateQueue())
                .to(flowboardExchange())
                .with(DUE_DATE_KEY);
    }

    @Bean
    public Binding inviteBinding() {
        return BindingBuilder
                .bind(inviteQueue())
                .to(flowboardExchange())
                .with(INVITE_KEY);
    }

    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory factory) {
        RabbitTemplate template = new RabbitTemplate(factory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}
