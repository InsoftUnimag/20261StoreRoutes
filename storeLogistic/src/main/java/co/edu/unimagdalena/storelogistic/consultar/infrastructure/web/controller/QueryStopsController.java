package co.edu.unimagdalena.storelogistic.consultar.infrastructure.web.controller;

import co.edu.unimagdalena.storelogistic.consultar.domain.ports.in.QueryStopsUseCase;
import co.edu.unimagdalena.storelogistic.consultar.infrastructure.mapper.QueryStopsMapper;
import co.edu.unimagdalena.storelogistic.consultar.infrastructure.web.dto.QueryStopsResponse;
import co.edu.unimagdalena.storelogistic.route.domain.models.Stop;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/logistics/routes")
@RequiredArgsConstructor
@Validated
public class QueryStopsController {

    private final QueryStopsUseCase queryStopsUseCase;
    private final QueryStopsMapper mapper;

    @GetMapping("/{routeId}/stops")
    public ResponseEntity<QueryStopsResponse> getStops(
            @PathVariable @Positive Long routeId,
            @RequestParam @Positive Long carrierId) {
        List<Stop> stops = queryStopsUseCase.query(routeId, carrierId);
        return ResponseEntity.ok(mapper.toResponse(routeId, carrierId, stops));
    }
}
