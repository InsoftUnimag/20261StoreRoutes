package co.edu.unimagdalena.storelogistic.route.application;

import co.edu.unimagdalena.storelogistic.route.application.services.AssignOrderService;
import co.edu.unimagdalena.storelogistic.route.application.services.SelectVehicleService;
import co.edu.unimagdalena.storelogistic.route.domain.exceptions.OrderNotFoundException;
import co.edu.unimagdalena.storelogistic.route.domain.models.*;
import co.edu.unimagdalena.storelogistic.route.domain.ports.in.AssignOrderUseCase.AssignResult;
import co.edu.unimagdalena.storelogistic.route.domain.ports.out.*;
import co.edu.unimagdalena.storelogistic.route.domain.values.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssignOrderServiceTest {

    @Mock OrderRepository orderRepository;
    @Mock RouteRepository routeRepository;
    @Mock StopRepository stopRepository;
    @Mock SelectVehicleService selectVehicleService;

    @InjectMocks AssignOrderService service;

    private Order sampleOrder;
    private Route availableRoute;
    private Stop savedStop;

    @BeforeEach
    void setUp() {
        sampleOrder = new Order(1L, LogisticWeight.of(500.0), "Calle Test");

        availableRoute = Route.reconstitute(10L, 5L,
                RouteCapacity.of(1_500.0), BigDecimal.valueOf(700),
                RouteStatus.AVAILABLE, LocalDate.now(), List.of());

        savedStop = Stop.reconstitute(99L, 10L, 1L, 1, "Calle Test", StopStatus.PENDING, null);
    }

    // ── SC1: Assign to existing route ──────────────────────────────────────────

    @Test
    @DisplayName("SC1 – assigns order to existing route, returns isNewRoute=false")
    void assign_existingRoute_returnsWithIsNewRouteFalse() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(sampleOrder));
        when(routeRepository.findAvailableWithCapacity(any())).thenReturn(Optional.of(availableRoute));
        when(routeRepository.save(any())).thenReturn(availableRoute);
        when(stopRepository.save(any())).thenReturn(savedStop);

        AssignResult result = service.assign(1L);

        assertThat(result.isNewRoute()).isFalse();
        assertThat(result.route().routeId()).isEqualTo(10L);
        verify(routeRepository, atLeastOnce()).save(any());
        verify(stopRepository).save(any());
    }

    @Test
    @DisplayName("SC1 – route not closed when below 95%")
    void assign_existingRoute_notClosedWhenBelow95() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(sampleOrder));
        when(routeRepository.findAvailableWithCapacity(any())).thenReturn(Optional.of(availableRoute));
        when(routeRepository.save(any())).thenReturn(availableRoute);
        when(stopRepository.save(any())).thenReturn(savedStop);

        service.assign(1L);

        // accumulatedWeight = 700 + 500 = 1200 out of 1500 = 80% → not closed
        assertThat(availableRoute.status()).isEqualTo(RouteStatus.AVAILABLE);
    }

    @Test
    @DisplayName("SC3 – route is closed when assignment reaches >= 95%")
    void assign_existingRoute_closedAt95Percent() {
        // 1425 + 75 = 1500 = 100% → should close
        Route nearFullRoute = Route.reconstitute(10L, 5L,
                RouteCapacity.of(1_500.0), BigDecimal.valueOf(1_425),
                RouteStatus.AVAILABLE, LocalDate.now(), List.of());

        Order smallOrder = new Order(2L, LogisticWeight.of(75.0), "Addr");

        when(orderRepository.findById(2L)).thenReturn(Optional.of(smallOrder));
        when(routeRepository.findAvailableWithCapacity(any())).thenReturn(Optional.of(nearFullRoute));
        when(routeRepository.save(any())).thenReturn(nearFullRoute);
        when(stopRepository.save(any())).thenReturn(savedStop);

        service.assign(2L);

        assertThat(nearFullRoute.status()).isEqualTo(RouteStatus.CLOSED);
        verify(routeRepository, times(2)).save(any());
    }

    // ── SC2: Create new route ─────────────────────────────────────────────────

    @Test
    @DisplayName("SC2 – creates new route with vehicle when no route available")
    void assign_noExistingRoute_createsNewRoute() {
        RouteVehicle vehicle = new RouteVehicle(5L, VehicleType.SINGLE_TRUCK,
                RouteCapacity.of(5_000.0));
        Route newRoute = Route.createNew(5L, RouteCapacity.of(5_000.0), LocalDate.now());
        newRoute.setRouteId(20L);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(sampleOrder));
        when(routeRepository.findAvailableWithCapacity(any())).thenReturn(Optional.empty());
        when(selectVehicleService.select(any())).thenReturn(Optional.of(vehicle));
        when(routeRepository.save(any())).thenReturn(newRoute);
        when(stopRepository.save(any())).thenReturn(savedStop);

        AssignResult result = service.assign(1L);

        assertThat(result.isNewRoute()).isTrue();
        verify(selectVehicleService).select(any());
        verify(routeRepository, atLeastOnce()).save(any());
        verify(stopRepository).save(any());
    }

    @Test
    @DisplayName("SC2 – creates PENDING_VEHICLE route when no vehicle available")
    void assign_noVehicleAvailable_createsPendingRoute() {
        Route pendingRoute = Route.createNew(null, RouteCapacity.of(1_000.0), LocalDate.now());
        pendingRoute.setRouteId(30L);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(sampleOrder));
        when(routeRepository.findAvailableWithCapacity(any())).thenReturn(Optional.empty());
        when(selectVehicleService.select(any())).thenReturn(Optional.empty());
        when(routeRepository.save(any())).thenReturn(pendingRoute);

        AssignResult result = service.assign(1L);

        assertThat(result.isNewRoute()).isTrue();
        assertThat(result.route().status()).isEqualTo(RouteStatus.PENDING_VEHICLE);
        verify(stopRepository, never()).save(any());
    }

    // ── OrderNotFoundException ─────────────────────────────────────────────────

    @Test
    @DisplayName("throws OrderNotFoundException when order does not exist")
    void assign_orderNotFound_throwsException() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.assign(99L))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessageContaining("99");
    }
}
