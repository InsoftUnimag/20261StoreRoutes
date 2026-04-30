package co.edu.unimagdalena.storelogistic.infrastructure.consultar.web.controller;

import co.edu.unimagdalena.storelogistic.domain.consultar.ports.in.GetStopDetailUseCase;
import co.edu.unimagdalena.storelogistic.domain.consultar.ports.in.QueryStopsUseCase;
import co.edu.unimagdalena.storelogistic.infrastructure.consultar.mapper.QueryStopsMapper;
import co.edu.unimagdalena.storelogistic.infrastructure.consultar.web.dto.QueryStopsResponse;
import co.edu.unimagdalena.storelogistic.infrastructure.consultar.web.dto.StopDetailDTO;
import co.edu.unimagdalena.storelogistic.domain.paymentmethod.models.OrderPaymentMethod;
import co.edu.unimagdalena.storelogistic.domain.paymentmethod.ports.in.ConsultPaymentMethodUseCase;
import co.edu.unimagdalena.storelogistic.domain.route.models.Stop;
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
    private final GetStopDetailUseCase getStopDetailUseCase;
    private final ConsultPaymentMethodUseCase consultPaymentMethodUseCase;
    private final QueryStopsMapper mapper;

    @GetMapping("/{routeId}/stops")
    public ResponseEntity<QueryStopsResponse> listStops(
            @PathVariable @Positive Long routeId,
            @RequestParam @Positive Long carrierId) {
        List<Stop> stops = queryStopsUseCase.query(routeId, carrierId);
        return ResponseEntity.ok(mapper.toResponse(routeId, carrierId, stops));
    }

    @GetMapping("/{routeId}/stops/{stopId}")
    public ResponseEntity<StopDetailDTO> getStopDetail(
            @PathVariable @Positive Long routeId,
            @PathVariable @Positive Long stopId,
            @RequestParam @Positive Long carrierId) {
        Stop stop = getStopDetailUseCase.get(routeId, stopId, carrierId);
        OrderPaymentMethod payment = consultPaymentMethodUseCase.consult(stop.orderId());
        return ResponseEntity.ok(mapper.toDetailDTO(stop, payment));
    }
}
