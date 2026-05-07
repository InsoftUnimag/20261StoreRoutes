package co.edu.unimagdalena.storelogistic.infrastructure.route;

import co.edu.unimagdalena.storelogistic.domain.route.exceptions.CapacityExceededException;
import co.edu.unimagdalena.storelogistic.domain.route.models.Route;
import co.edu.unimagdalena.storelogistic.domain.route.ports.in.ProcessRouteRequestUseCase;
import co.edu.unimagdalena.storelogistic.domain.route.values.*;
import co.edu.unimagdalena.storelogistic.infrastructure.route.messaging.dto.*;
import co.edu.unimagdalena.storelogistic.infrastructure.route.messaging.listener.RouteRequestListener;
import co.edu.unimagdalena.storelogistic.infrastructure.route.messaging.publisher.RouteEventPublisher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RouteRequestListenerTest {

    @Mock ProcessRouteRequestUseCase useCase;
    @Mock RouteEventPublisher publisher;
    @InjectMocks RouteRequestListener listener;

    private Route sampleRoute() {
        return Route.reconstitute(10L, 5L, RouteCapacity.of(1_500.0),
                BigDecimal.valueOf(500), RouteStatus.AVAILABLE, LocalDate.now(), List.of());
    }

    @Test
    @DisplayName("Valid event → publishes RouteAssignedEvent")
    void validEvent_publishesRouteAssigned() {
        RouteRequestEvent event = RouteRequestEvent.builder()
                .orderId(1L).clientId(2L)
                .logisticWeight(BigDecimal.valueOf(500))
                .deliveryAddress("Calle 1")
                .build();

        when(useCase.process(1L, BigDecimal.valueOf(500), "Calle 1")).thenReturn(sampleRoute());

        Consumer<RouteRequestEvent> consumer = listener.procesarSolicitudRuta();
        consumer.accept(event);

        verify(publisher).publishRouteAssigned(argThat(e ->
                e.getOrderId().equals(1L) && e.getRouteId().equals(10L)));
        verify(publisher, never()).publishRouteError(any());
    }

    @Test
    @DisplayName("Business exception → publishes RouteErrorEvent, does not rethrow")
    void businessException_publishesError_doesNotRethrow() {
        RouteRequestEvent event = RouteRequestEvent.builder()
                .orderId(1L).clientId(2L)
                .logisticWeight(BigDecimal.valueOf(500))
                .deliveryAddress("Calle 1")
                .build();

        when(useCase.process(any(), any(), any()))
                .thenThrow(new CapacityExceededException("Too heavy"));

        Consumer<RouteRequestEvent> consumer = listener.procesarSolicitudRuta();
        assertThatCode(() -> consumer.accept(event)).doesNotThrowAnyException();

        verify(publisher).publishRouteError(argThat(e ->
                "ERROR_SOLICITUD_RUTA".equals(e.getCode())));
    }

    @Test
    @DisplayName("Null orderId → publishes RouteErrorEvent without calling use case")
    void nullOrderId_publishesError_noUseCaseCall() {
        RouteRequestEvent event = RouteRequestEvent.builder()
                .orderId(null)
                .logisticWeight(BigDecimal.valueOf(500))
                .deliveryAddress("Calle 1")
                .build();

        Consumer<RouteRequestEvent> consumer = listener.procesarSolicitudRuta();
        consumer.accept(event);

        verify(useCase, never()).process(any(), any(), any());
        verify(publisher).publishRouteError(any());
    }

    @Test
    @DisplayName("Route reaches 95% capacity → publishes RouteAssignedEvent AND RouteDispatchedEvent")
    void routeReachesCapacity_publishesBothEvents() {
        RouteRequestEvent event = RouteRequestEvent.builder()
                .orderId(7L).clientId(2L)
                .logisticWeight(BigDecimal.valueOf(400))
                .deliveryAddress("Calle 7")
                .build();

        Route closedRoute = Route.reconstitute(10L, 5L, RouteCapacity.of(1_000.0),
                BigDecimal.valueOf(970), RouteStatus.CLOSED, LocalDate.now(),
                List.of());

        when(useCase.process(7L, BigDecimal.valueOf(400), "Calle 7")).thenReturn(closedRoute);

        Consumer<RouteRequestEvent> consumer = listener.procesarSolicitudRuta();
        consumer.accept(event);

        verify(publisher).publishRouteAssigned(argThat(e ->
                e.getOrderId().equals(7L) && e.getRouteId().equals(10L)));
        verify(publisher).publishRouteDispatched(argThat(e ->
                e.getRouteId().equals(10L) && e.getDispatchedAt() != null));
        verify(publisher, never()).publishRouteError(any());
    }

    @Test
    @DisplayName("Unexpected exception → rethrows for DLQ handling")
    void unexpectedException_rethrows() {
        RouteRequestEvent event = RouteRequestEvent.builder()
                .orderId(1L).clientId(2L)
                .logisticWeight(BigDecimal.valueOf(500))
                .deliveryAddress("Calle 1")
                .build();

        when(useCase.process(any(), any(), any()))
                .thenThrow(new RuntimeException("DB connection lost"));

        Consumer<RouteRequestEvent> consumer = listener.procesarSolicitudRuta();
        assertThatThrownBy(() -> consumer.accept(event))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("DB connection lost");
    }
}
