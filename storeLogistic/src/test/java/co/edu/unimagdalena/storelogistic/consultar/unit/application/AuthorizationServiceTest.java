package co.edu.unimagdalena.storelogistic.consultar.unit.application;

import co.edu.unimagdalena.storelogistic.consultar.application.services.AuthorizationService;
import co.edu.unimagdalena.storelogistic.consultar.domain.exceptions.AccessDeniedException;
import co.edu.unimagdalena.storelogistic.route.domain.models.Route;
import co.edu.unimagdalena.storelogistic.route.domain.ports.out.RouteRepository;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthorizationServiceTest {

    @Mock
    RouteRepository routeRepository;

    @InjectMocks
    AuthorizationService authorizationService;

    private static Route sampleRoute() {
        return Route.reconstitute(1L, 10L,
                RouteCapacity.of(BigDecimal.valueOf(1000)),
                BigDecimal.ZERO,
                RouteStatus.AVAILABLE,
                LocalDate.now(),
                List.of());
    }

    @Test
    @DisplayName("verifyCarrierHasAccess → returns Route when route is assigned to carrier")
    void verifyAccess_routeAssignedToCarrier_returnsRoute() {
        Route route = sampleRoute();
        when(routeRepository.findByIdAndCarrierId(1L, 1L)).thenReturn(Optional.of(route));

        Route result = authorizationService.verifyCarrierHasAccess(1L, 1L);

        assertThat(result).isEqualTo(route);
        verify(routeRepository).findByIdAndCarrierId(1L, 1L);
    }

    @Test
    @DisplayName("verifyCarrierHasAccess → throws AccessDeniedException when route belongs to another carrier")
    void verifyAccess_routeOfOtherCarrier_throwsAccessDenied() {
        when(routeRepository.findByIdAndCarrierId(1L, 99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authorizationService.verifyCarrierHasAccess(1L, 99L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Acceso denegado");
    }

    @Test
    @DisplayName("verifyCarrierHasAccess → throws AccessDeniedException when route does not exist (same response as unauthorized)")
    void verifyAccess_routeNotExists_throwsAccessDenied() {
        when(routeRepository.findByIdAndCarrierId(999L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authorizationService.verifyCarrierHasAccess(999L, 1L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Acceso denegado");
    }
}
