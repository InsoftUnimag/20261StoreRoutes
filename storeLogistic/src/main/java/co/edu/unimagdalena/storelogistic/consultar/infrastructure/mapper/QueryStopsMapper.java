package co.edu.unimagdalena.storelogistic.consultar.infrastructure.mapper;

import co.edu.unimagdalena.storelogistic.consultar.infrastructure.web.dto.QueryStopsResponse;
import co.edu.unimagdalena.storelogistic.consultar.infrastructure.web.dto.StopDTO;
import co.edu.unimagdalena.storelogistic.route.domain.models.Stop;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public abstract class QueryStopsMapper {

    public StopDTO toDTO(Stop stop) {
        if (stop == null) return null;
        return new StopDTO(
                stop.stopId(),
                stop.sequence(),
                stop.deliveryAddress(),
                stop.customerContact(),
                stop.status() != null ? stop.status().name() : null
        );
    }

    public QueryStopsResponse toResponse(Long routeId, Long carrierId, List<Stop> stops) {
        List<StopDTO> dtos = stops.stream().map(this::toDTO).toList();
        return new QueryStopsResponse(routeId, carrierId, dtos.size(), dtos);
    }
}
