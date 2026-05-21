package co.edu.unimagdalena.storelogistic.infrastructure.shared.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    static final String EXCHANGE = "estadopedido";
    static final String QUEUE    = "estadopedido";

    @Bean
    public TopicExchange estadoPedidoExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue estadoPedidoQueue() {
        return new Queue(QUEUE, true);
    }

    @Bean
    public Binding estadoPedidoBinding(Queue estadoPedidoQueue, TopicExchange estadoPedidoExchange) {
        return BindingBuilder.bind(estadoPedidoQueue).to(estadoPedidoExchange).with("#");
    }
}
