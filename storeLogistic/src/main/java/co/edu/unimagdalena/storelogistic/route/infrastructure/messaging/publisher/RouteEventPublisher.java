package co.edu.unimagdalena.storelogistic.route.infrastructure.messaging.publisher;

import co.edu.unimagdalena.storelogistic.route.infrastructure.messaging.dto.RouteAssignedEvent;
import co.edu.unimagdalena.storelogistic.route.infrastructure.messaging.dto.RouteErrorEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RouteEventPublisher {

    private static final String ROUTE_ASSIGNED_BINDING = "publicarRutaAsignada-out-0";
    private static final String ROUTE_ERROR_BINDING    = "publicarErrorSolicitud-out-0";

    private final StreamBridge streamBridge;

    public void publishRouteAssigned(RouteAssignedEvent event) {
        log.info("Publishing RutaAsignada: orderId={}, routeId={}", event.getOrderId(), event.getRouteId());
        streamBridge.send(ROUTE_ASSIGNED_BINDING, event);
    }

    public void publishRouteError(RouteErrorEvent event) {
        log.warn("Publishing ErrorSolicitudRuta: code={}, message={}", event.getCode(), event.getMessage());
        streamBridge.send(ROUTE_ERROR_BINDING, event);
    }
}
