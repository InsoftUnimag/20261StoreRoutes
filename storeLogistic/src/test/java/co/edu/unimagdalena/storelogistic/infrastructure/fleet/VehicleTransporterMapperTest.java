package co.edu.unimagdalena.storelogistic.infrastructure.fleet;

import co.edu.unimagdalena.storelogistic.domain.fleet.models.Vehicle;
import co.edu.unimagdalena.storelogistic.domain.fleet.values.LoadCapacity;
import co.edu.unimagdalena.storelogistic.domain.fleet.values.VehicleStatus;
import co.edu.unimagdalena.storelogistic.infrastructure.fleet.mapper.VehicleTransporterMapper;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class VehicleTransporterMapperTest {

    private final VehicleTransporterMapper mapper = Mappers.getMapper(VehicleTransporterMapper.class);

    @Test
    void toResponse_mapsVehicleIdAndTransporterId() {
        var vehicle = Vehicle.builder()
                .vehicleId(42L)
                .transporterId(1L)
                .loadCapacity(new LoadCapacity(BigDecimal.valueOf(1500)))
                .status(VehicleStatus.DISPONIBLE)
                .currentWeight(BigDecimal.ZERO)
                .build();

        var response = mapper.toResponse(vehicle);

        assertThat(response.getIdVehiculo()).isEqualTo(42L);
        assertThat(response.getIdTransportista()).isEqualTo(1L);
    }

    @Test
    void toResponse_withDifferentTransporterId_mapsCorrectly() {
        var vehicle = Vehicle.builder()
                .vehicleId(7L)
                .transporterId(99L)
                .loadCapacity(new LoadCapacity(BigDecimal.valueOf(5000)))
                .status(VehicleStatus.EN_MANTENIMIENTO)
                .currentWeight(BigDecimal.ZERO)
                .build();

        var response = mapper.toResponse(vehicle);

        assertThat(response.getIdVehiculo()).isEqualTo(7L);
        assertThat(response.getIdTransportista()).isEqualTo(99L);
    }
}