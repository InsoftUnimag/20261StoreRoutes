package co.edu.unimagdalena.storelogistic.infrastructure.orderstatus.messaging;

import co.edu.unimagdalena.storelogistic.domain.orderstatus.ports.out.OrderStatusEventPublisher;
import co.edu.unimagdalena.storelogistic.domain.orderstatus.values.FinalStatus;
import co.edu.unimagdalena.storelogistic.infrastructure.orderstatus.messaging.dto.OrderStatusEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderStatusEventPublisherAdapter implements OrderStatusEventPublisher {

    private static final String BINDING = "publicarEstadoPedido-out-0";

    private final StreamBridge streamBridge;

    @Override
    public void publish(Long orderId, FinalStatus status, Long carrierId) {
        OrderStatusEventDto event = OrderStatusEventDto.builder()
                .id_pedido(orderId)
                .tasa_efectividad(status.effectivenessRate().value())
                .id_transportista(carrierId)
                .build();
        log.info("Publishing order status event to finance module: {}", event);
        streamBridge.send(BINDING, event);
    }
}
