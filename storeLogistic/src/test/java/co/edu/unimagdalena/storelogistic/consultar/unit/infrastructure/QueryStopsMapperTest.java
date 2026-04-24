package co.edu.unimagdalena.storelogistic.consultar.unit.infrastructure;

import co.edu.unimagdalena.storelogistic.consultar.infrastructure.mapper.QueryStopsMapperImpl;
import co.edu.unimagdalena.storelogistic.consultar.infrastructure.web.dto.QueryStopsResponse;
import co.edu.unimagdalena.storelogistic.consultar.infrastructure.web.dto.StopDTO;
import co.edu.unimagdalena.storelogistic.consultar.testdata.StopFixture;
import co.edu.unimagdalena.storelogistic.route.domain.models.Stop;
import co.edu.unimagdalena.storelogistic.route.domain.values.StopStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class QueryStopsMapperTest {

    private final QueryStopsMapperImpl mapper = new QueryStopsMapperImpl();

    @Test
    @DisplayName("toDTO → maps Stop fields to StopDTO correctly")
    void toDTO_mapsAllFields() {
        Stop stop = StopFixture.standard();

        StopDTO dto = mapper.toDTO(stop);

        assertThat(dto.getIdStop()).isEqualTo(stop.stopId());
        assertThat(dto.getSequence()).isEqualTo(stop.sequence());
        assertThat(dto.getDeliveryAddress()).isEqualTo(stop.deliveryAddress());
        assertThat(dto.getCustomerContact()).isEqualTo(stop.customerContact());
        assertThat(dto.getStatus()).isEqualTo(StopStatus.PENDING.name());
    }

    @Test
    @DisplayName("toDTO → customerContact is null when not set")
    void toDTO_withNullContact_mapsToNull() {
        Stop stop = StopFixture.withRoute(1L, 1);

        StopDTO dto = mapper.toDTO(stop);

        assertThat(dto.getCustomerContact()).isNull();
    }

    @Test
    @DisplayName("toResponse → totalStops matches stops list size")
    void toResponse_totalStopsMatchesSize() {
        List<Stop> stops = List.of(StopFixture.withSequence(1), StopFixture.withSequence(2));

        QueryStopsResponse response = mapper.toResponse(5L, 2L, stops);

        assertThat(response.getRouteId()).isEqualTo(5L);
        assertThat(response.getCarrierId()).isEqualTo(2L);
        assertThat(response.getTotalStops()).isEqualTo(2);
        assertThat(response.getStops()).hasSize(2);
    }

    @Test
    @DisplayName("toResponse → empty stops list yields totalStops=0")
    void toResponse_emptyStops_yieldsZero() {
        QueryStopsResponse response = mapper.toResponse(1L, 1L, List.of());

        assertThat(response.getTotalStops()).isZero();
        assertThat(response.getStops()).isEmpty();
    }
}
