package co.edu.unimagdalena.storelogistic.consultar.infrastructure.web.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QueryStopsResponse {
    private Long routeId;
    private Long carrierId;
    private int totalStops;
    private List<StopDTO> stops;
}
