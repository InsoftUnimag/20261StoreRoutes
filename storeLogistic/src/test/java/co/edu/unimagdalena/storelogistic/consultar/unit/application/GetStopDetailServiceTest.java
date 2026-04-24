package co.edu.unimagdalena.storelogistic.consultar.unit.application;

import co.edu.unimagdalena.storelogistic.consultar.application.services.AuthorizationService;
import co.edu.unimagdalena.storelogistic.consultar.application.services.GetStopDetailService;
import co.edu.unimagdalena.storelogistic.consultar.domain.exceptions.AccessDeniedException;
import co.edu.unimagdalena.storelogistic.consultar.testdata.StopFixture;
import co.edu.unimagdalena.storelogistic.route.domain.models.Stop;
import co.edu.unimagdalena.storelogistic.route.domain.ports.out.StopRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetStopDetailServiceTest {

    @Mock
    AuthorizationService authorizationService;

    @Mock
    StopRepository stopRepository;

    @InjectMocks
    GetStopDetailService getStopDetailService;

    @Test
    @DisplayName("get → happy path: verifies access and returns stop detail")
    void get_whenAuthorizedAndStopExists_returnsStop() {
        Stop expected = StopFixture.standard();
        when(stopRepository.findByIdAndRouteId(1L, 1L)).thenReturn(Optional.of(expected));

        Stop result = getStopDetailService.get(1L, 1L, 1L);

        assertThat(result).isEqualTo(expected);
        verify(authorizationService).verifyCarrierHasAccess(1L, 1L);
        verify(stopRepository).findByIdAndRouteId(1L, 1L);
    }

    @Test
    @DisplayName("get → access denied: StopRepository is never called")
    void get_whenAccessDenied_doesNotCallStopRepository() {
        when(authorizationService.verifyCarrierHasAccess(1L, 99L))
                .thenThrow(new AccessDeniedException());

        assertThatThrownBy(() -> getStopDetailService.get(1L, 1L, 99L))
                .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(stopRepository);
    }

    @Test
    @DisplayName("get → stop not in route: throws AccessDeniedException")
    void get_whenStopNotInRoute_throwsAccessDenied() {
        when(stopRepository.findByIdAndRouteId(99L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> getStopDetailService.get(1L, 99L, 1L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Acceso denegado");
    }
}
