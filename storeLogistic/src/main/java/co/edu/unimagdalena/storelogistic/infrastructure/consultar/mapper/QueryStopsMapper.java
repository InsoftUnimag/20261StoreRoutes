package co.edu.unimagdalena.storelogistic.infrastructure.consultar.mapper;

import co.edu.unimagdalena.storelogistic.infrastructure.consultar.web.dto.QueryStopsResponse;
import co.edu.unimagdalena.storelogistic.infrastructure.consultar.web.dto.StopDetailDTO;
import co.edu.unimagdalena.storelogistic.infrastructure.consultar.web.dto.StopSummaryDTO;
import co.edu.unimagdalena.storelogistic.domain.paymentmethod.models.OrderPaymentMethod;
import co.edu.unimagdalena.storelogistic.domain.route.models.Stop;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public abstract class QueryStopsMapper {

    public StopSummaryDTO toSummaryDTO(Stop stop) {
        if (stop == null) return null;
        return new StopSummaryDTO(
                stop.stopId(),
                stop.sequence(),
                stop.deliveryAddress(),
                stop.orderId(),
                stop.status() != null ? stop.status().name() : null
        );
    }

    public StopDetailDTO toDetailDTO(Stop stop, OrderPaymentMethod payment) {
        if (stop == null) return null;
        return new StopDetailDTO(
                stop.stopId(),
                stop.sequence(),
                stop.deliveryAddress(),
                stop.orderId(),
                stop.customerContact(),
                payment != null && payment.paymentMethod() != null ? payment.paymentMethod().name() : null,
                payment != null ? payment.totalPedido() : null,
                stop.status() != null ? stop.status().name() : null
        );
    }

    public QueryStopsResponse toResponse(Long routeId, Long carrierId, List<Stop> stops) {
        List<StopSummaryDTO> dtos = stops.stream().map(this::toSummaryDTO).toList();
        return new QueryStopsResponse(routeId, carrierId, dtos.size(), dtos);
    }
}
