package co.edu.unimagdalena.storelogistic.route.application;

import co.edu.unimagdalena.storelogistic.route.application.services.SelectVehicleService;
import co.edu.unimagdalena.storelogistic.route.domain.exceptions.CapacityExceededException;
import co.edu.unimagdalena.storelogistic.route.domain.models.RouteVehicle;
import co.edu.unimagdalena.storelogistic.route.domain.ports.out.RouteVehicleRepository;
import co.edu.unimagdalena.storelogistic.route.domain.values.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SelectVehicleServiceTest {

    @Mock RouteVehicleRepository vehicleRepository;
    @InjectMocks SelectVehicleService service;

    private RouteVehicle vehicle(VehicleType type) {
        return new RouteVehicle(1L, type, type.capacity());
    }

    @Test
    @DisplayName("≤1 500 kg → selects URBAN_VAN")
    void select_lightWeight_returnsUrbanVan() {
        when(vehicleRepository.findAvailableByType(VehicleType.URBAN_VAN))
                .thenReturn(Optional.of(vehicle(VehicleType.URBAN_VAN)));

        Optional<RouteVehicle> result = service.select(LogisticWeight.of(800.0));

        assertThat(result).isPresent();
        assertThat(result.get().type()).isEqualTo(VehicleType.URBAN_VAN);
    }

    @Test
    @DisplayName("≤5 000 kg → selects SINGLE_TRUCK")
    void select_mediumWeight_returnsSingleTruck() {
        when(vehicleRepository.findAvailableByType(VehicleType.SINGLE_TRUCK))
                .thenReturn(Optional.of(vehicle(VehicleType.SINGLE_TRUCK)));

        Optional<RouteVehicle> result = service.select(LogisticWeight.of(3_000.0));

        assertThat(result).isPresent();
        assertThat(result.get().type()).isEqualTo(VehicleType.SINGLE_TRUCK);
    }

    @Test
    @DisplayName("≤25 000 kg → selects REGIONAL_SEMI")
    void select_heavyWeight_returnsRegionalSemi() {
        when(vehicleRepository.findAvailableByType(VehicleType.REGIONAL_SEMI))
                .thenReturn(Optional.of(vehicle(VehicleType.REGIONAL_SEMI)));

        Optional<RouteVehicle> result = service.select(LogisticWeight.of(10_000.0));

        assertThat(result).isPresent();
        assertThat(result.get().type()).isEqualTo(VehicleType.REGIONAL_SEMI);
    }

    @Test
    @DisplayName(">25 000 kg → throws CapacityExceededException (no vehicle can handle it)")
    void select_overMaxCapacity_throwsException() {
        LogisticWeight overweight = LogisticWeight.of(26_000.0);
        assertThatThrownBy(() -> service.select(overweight))
                .isInstanceOf(CapacityExceededException.class);
    }

    @Test
    @DisplayName("preferred type unavailable → uses fallback max-capacity vehicle")
    void select_preferredTypeUnavailable_usesFallback() {
        RouteVehicle fallback = vehicle(VehicleType.REGIONAL_SEMI);
        when(vehicleRepository.findAvailableByType(VehicleType.URBAN_VAN)).thenReturn(Optional.empty());
        when(vehicleRepository.findMaxCapacity()).thenReturn(Optional.of(fallback));

        Optional<RouteVehicle> result = service.select(LogisticWeight.of(500.0));

        assertThat(result).isPresent();
        assertThat(result.get().type()).isEqualTo(VehicleType.REGIONAL_SEMI);
    }

    @Test
    @DisplayName("no vehicles at all → returns empty Optional")
    void select_noVehicles_returnsEmpty() {
        when(vehicleRepository.findAvailableByType(any())).thenReturn(Optional.empty());
        when(vehicleRepository.findMaxCapacity()).thenReturn(Optional.empty());

        Optional<RouteVehicle> result = service.select(LogisticWeight.of(500.0));

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("fallback vehicle too small → throws CapacityExceededException")
    void select_fallbackTooSmall_throwsException() {
        RouteVehicle tooSmall = new RouteVehicle(1L, VehicleType.URBAN_VAN, RouteCapacity.of(1_500.0));
        when(vehicleRepository.findAvailableByType(VehicleType.REGIONAL_SEMI)).thenReturn(Optional.empty());
        when(vehicleRepository.findMaxCapacity()).thenReturn(Optional.of(tooSmall));

        assertThatThrownBy(() -> service.select(LogisticWeight.of(20_000.0)))
                .isInstanceOf(CapacityExceededException.class);
    }
}
