package co.edu.unimagdalena.storelogistic.orderstatus.infrastructure.messaging;

import co.edu.unimagdalena.storelogistic.orderstatus.domain.ports.out.OrderStatusEventPublisher;
import co.edu.unimagdalena.storelogistic.orderstatus.domain.values.FinalStatus;
import co.edu.unimagdalena.storelogistic.orderstatus.infrastructure.messaging.dto.OrderStatusEventDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OrderStatusEventPublisherAdapter implements OrderStatusEventPublisher {

    @Override
    @Async
    public void publish(Long orderId, FinalStatus status, Long carrierId) {
        OrderStatusEventDto event = OrderStatusEventDto.builder()
                .id_pedido(orderId)
                .estado_final(status.displayName())
                .tasa_efectividad(status.effectivenessRate().value())
                .id_transportista(carrierId)
                .build();
        log.info("[ASYNC] Event published to finance module: {}", event);
    }
}
