package co.edu.unimagdalena.storelogistic.consultar.unit.application;

import co.edu.unimagdalena.storelogistic.consultar.application.services.AuthorizationService;
import co.edu.unimagdalena.storelogistic.consultar.application.services.QueryStopsService;
import co.edu.unimagdalena.storelogistic.consultar.domain.exceptions.AccessDeniedException;
import co.edu.unimagdalena.storelogistic.consultar.testdata.StopFixture;
import co.edu.unimagdalena.storelogistic.route.domain.models.Route;
import co.edu.unimagdalena.storelogistic.route.domain.models.Stop;
import co.edu.unimagdalena.storelogistic.route.domain.ports.out.StopRepository;
import co.edu.unimagdalena.storelogistic.route.domain.values.RouteCapacity;
import co.edu.unimagdalena.storelogistic.route.domain.values.RouteStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QueryStopsServiceTest {

    @Mock
    AuthorizationService authorizationService;

    @Mock
    StopRepository stopRepository;

    @InjectMocks
    QueryStopsService queryStopsService;

    private static Route sampleRoute() {
        return Route.reconstitute(1L, 10L,
                RouteCapacity.of(BigDecimal.valueOf(1000)),
                BigDecimal.ZERO,
                RouteStatus.AVAILABLE,
                LocalDate.now(),
                List.of());
    }

    @Test
    @DisplayName("query → happy path: verifies access and returns stops ordered by sequence")
    void query_whenAuthorized_returnsStops() {
        Route route = sampleRoute();
        List<Stop> stops = List.of(StopFixture.withSequence(1), StopFixture.withSequence(2));
        when(authorizationService.verifyCarrierHasAccess(1L, 1L)).thenReturn(route);
        when(stopRepository.findByRouteIdOrderBySequence(1L)).thenReturn(stops);

        List<Stop> result = queryStopsService.query(1L, 1L);

        assertThat(result).hasSize(2);
        verify(authorizationService).verifyCarrierHasAccess(1L, 1L);
        verify(stopRepository).findByRouteIdOrderBySequence(1L);
    }

    @Test
    @DisplayName("query → access denied: StopRepository is never called")
    void query_whenAccessDenied_doesNotCallStopRepository() {
        when(authorizationService.verifyCarrierHasAccess(1L, 99L))
                .thenThrow(new AccessDeniedException());

        assertThatThrownBy(() -> queryStopsService.query(1L, 99L))
                .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(stopRepository);
    }

    @Test
    @DisplayName("query → route with zero stops returns empty list")
    void query_whenRouteHasNoStops_returnsEmptyList() {
        Route route = sampleRoute();
        when(authorizationService.verifyCarrierHasAccess(1L, 1L)).thenReturn(route);
        when(stopRepository.findByRouteIdOrderBySequence(1L)).thenReturn(List.of());

        List<Stop> result = queryStopsService.query(1L, 1L);

        assertThat(result).isEmpty();
        verify(stopRepository).findByRouteIdOrderBySequence(1L);
    }
}
