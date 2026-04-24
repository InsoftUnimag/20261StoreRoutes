package co.edu.unimagdalena.storelogistic.consultar.unit.infrastructure;

import co.edu.unimagdalena.storelogistic.consultar.infrastructure.mapper.QueryStopsMapperImpl;
import co.edu.unimagdalena.storelogistic.consultar.infrastructure.web.dto.QueryStopsResponse;
import co.edu.unimagdalena.storelogistic.consultar.infrastructure.web.dto.StopDetailDTO;
import co.edu.unimagdalena.storelogistic.consultar.infrastructure.web.dto.StopSummaryDTO;
import co.edu.unimagdalena.storelogistic.consultar.testdata.StopFixture;
import co.edu.unimagdalena.storelogistic.paymentmethod.testdata.OrderPaymentMethodFixture;
import co.edu.unimagdalena.storelogistic.route.domain.models.Stop;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class QueryStopsMapperTest {

    private final QueryStopsMapperImpl mapper = new QueryStopsMapperImpl();

    @Test
    @DisplayName("toSummaryDTO → maps Stop fields correctly")
    void toSummaryDTO_mapsAllFields() {
        Stop stop = StopFixture.standard();

        StopSummaryDTO dto = mapper.toSummaryDTO(stop);

        assertThat(dto.getIdStop()).isEqualTo(stop.stopId());
        assertThat(dto.getSequence()).isEqualTo(stop.sequence());
        assertThat(dto.getDeliveryAddress()).isEqualTo(stop.deliveryAddress());
        assertThat(dto.getOrderId()).isEqualTo(stop.orderId());
        assertThat(dto.getStatus()).isEqualTo("PENDING");
    }

    @Test
    @DisplayName("toSummaryDTO → null stop returns null")
    void toSummaryDTO_nullStop_returnsNull() {
        assertThat(mapper.toSummaryDTO(null)).isNull();
    }

    @Test
    @DisplayName("toDetailDTO → CONTRA_ENTREGA includes totalACobrar and paymentMethod")
    void toDetailDTO_contraEntrega_includesTotal() {
        Stop stop = StopFixture.standard();

        StopDetailDTO dto = mapper.toDetailDTO(stop, OrderPaymentMethodFixture.contraEntrega());

        assertThat(dto.getIdStop()).isEqualTo(stop.stopId());
        assertThat(dto.getCustomerContact()).isEqualTo(stop.customerContact());
        assertThat(dto.getPaymentMethod()).isEqualTo("CONTRA_ENTREGA");
        assertThat(dto.getTotalACobrar()).isEqualByComparingTo("150000.00");
        assertThat(dto.getStatus()).isEqualTo("PENDING");
    }

    @Test
    @DisplayName("toDetailDTO → CARTERA_COMERCIAL yields null totalACobrar")
    void toDetailDTO_carteraComercial_totalACobrarIsNull() {
        Stop stop = StopFixture.standard();

        StopDetailDTO dto = mapper.toDetailDTO(stop, OrderPaymentMethodFixture.carteraComercial());

        assertThat(dto.getPaymentMethod()).isEqualTo("CARTERA_COMERCIAL");
        assertThat(dto.getTotalACobrar()).isNull();
    }

    @Test
    @DisplayName("toDetailDTO → null payment yields null paymentMethod and totalACobrar")
    void toDetailDTO_nullPayment_paymentFieldsAreNull() {
        Stop stop = StopFixture.standard();

        StopDetailDTO dto = mapper.toDetailDTO(stop, null);

        assertThat(dto.getPaymentMethod()).isNull();
        assertThat(dto.getTotalACobrar()).isNull();
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
