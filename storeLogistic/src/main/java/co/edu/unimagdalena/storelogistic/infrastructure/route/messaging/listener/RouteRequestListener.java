package co.edu.unimagdalena.storelogistic.infrastructure.route.messaging.listener;

import co.edu.unimagdalena.storelogistic.domain.route.exceptions.RouteException;
import co.edu.unimagdalena.storelogistic.domain.route.models.Route;
import co.edu.unimagdalena.storelogistic.domain.route.ports.in.ProcessRouteRequestUseCase;
import co.edu.unimagdalena.storelogistic.infrastructure.route.messaging.dto.RouteAssignedEvent;
import co.edu.unimagdalena.storelogistic.infrastructure.route.messaging.dto.RouteErrorEvent;
import co.edu.unimagdalena.storelogistic.infrastructure.route.messaging.dto.RouteRequestEvent;
import co.edu.unimagdalena.storelogistic.infrastructure.route.messaging.publisher.RouteEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

@Slf4j
@Component
@RequiredArgsConstructor
public class RouteRequestListener {

    private final ProcessRouteRequestUseCase processRouteRequestUseCase;
    private final RouteEventPublisher publisher;

    @Bean
    public Consumer<RouteRequestEvent> procesarSolicitudRuta() {
        return event -> {
            log.info("Received SolicitudRutaRequerida: orderId={}, weight={}, address={}",
                    event.getOrderId(), event.getLogisticWeight(), event.getDeliveryAddress());

            if (event.getOrderId() == null || event.getLogisticWeight() == null ||
                    event.getDeliveryAddress() == null || event.getDeliveryAddress().isBlank()) {
                publisher.publishRouteError(RouteErrorEvent.builder()
                        .code("ERROR_SOLICITUD_RUTA")
                        .message("Required fields missing: orderId, pesoLogistico, direccionEntrega")
                        .build());
                return;
            }

            try {
                Route route = processRouteRequestUseCase.process(
                        event.getOrderId(),
                        event.getLogisticWeight(),
                        event.getDeliveryAddress()
                );
                publisher.publishRouteAssigned(RouteAssignedEvent.builder()
                        .orderId(event.getOrderId())
                        .routeId(route.routeId())
                        .dispatchDate(route.dispatchDate())
                        .build());
            } catch (RouteException e) {
                log.warn("Business error processing route request for orderId={}: {}",
                        event.getOrderId(), e.getMessage());
                publisher.publishRouteError(RouteErrorEvent.builder()
                        .code("ERROR_SOLICITUD_RUTA")
                        .message(e.getMessage())
                        .build());
            } catch (Exception e) {
                log.error("Unexpected error processing route request for orderId={}", event.getOrderId(), e);
                throw e;
            }
        };
    }
}
