package co.edu.unimagdalena.storelogistic.infrastructure.consultar.web.controller;

import co.edu.unimagdalena.storelogistic.domain.consultar.ports.in.GetStopDetailUseCase;
import co.edu.unimagdalena.storelogistic.domain.consultar.ports.in.QueryStopsUseCase;
import co.edu.unimagdalena.storelogistic.domain.paymentmethod.models.OrderPaymentMethod;
import co.edu.unimagdalena.storelogistic.domain.paymentmethod.ports.in.ConsultPaymentMethodUseCase;
import co.edu.unimagdalena.storelogistic.domain.route.models.Stop;
import co.edu.unimagdalena.storelogistic.domain.route.ports.in.UpdateStopStatusUseCase;
import co.edu.unimagdalena.storelogistic.domain.route.values.StopStatus;
import co.edu.unimagdalena.storelogistic.infrastructure.consultar.mapper.QueryStopsMapper;
import co.edu.unimagdalena.storelogistic.infrastructure.consultar.web.dto.QueryStopsResponse;
import co.edu.unimagdalena.storelogistic.infrastructure.consultar.web.dto.StopDetailDTO;
import co.edu.unimagdalena.storelogistic.infrastructure.consultar.web.dto.UpdateStopStatusRequest;
import co.edu.unimagdalena.storelogistic.infrastructure.consultar.web.dto.UpdateStopStatusResponse;
import jakarta.validation.Valid;
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
    private final UpdateStopStatusUseCase updateStopStatusUseCase;
    private final QueryStopsMapper mapper;

    @GetMapping("/{routeId}/stops")
    public ResponseEntity<QueryStopsResponse> listStops(
            @PathVariable @Positive Long routeId) {
        List<Stop> stops = queryStopsUseCase.query(routeId);
        return ResponseEntity.ok(mapper.toResponse(routeId, stops));
    }

    @GetMapping("/{routeId}/stops/{stopId}")
    public ResponseEntity<StopDetailDTO> getStopDetail(
            @PathVariable @Positive Long routeId,
            @PathVariable @Positive Long stopId) {
        Stop stop = getStopDetailUseCase.get(routeId, stopId);
        OrderPaymentMethod payment = consultPaymentMethodUseCase.consult(stop.orderId());
        return ResponseEntity.ok(mapper.toDetailDTO(stop, payment));
    }

    @PatchMapping("/{routeId}/stops/{stopId}")
    public ResponseEntity<UpdateStopStatusResponse> updateStopStatus(
            @PathVariable @Positive Long routeId,
            @PathVariable @Positive Long stopId,
            @Valid @RequestBody UpdateStopStatusRequest request) {

        StopStatus resultado = StopStatus.valueOf(request.getResultado());
        UpdateStopStatusUseCase.Command command = new UpdateStopStatusUseCase.Command(
                routeId, stopId, resultado, request.getFechaEntrega());

        Stop stop = updateStopStatusUseCase.execute(command);
        return ResponseEntity.ok(toUpdateResponse(stop));
    }

    private UpdateStopStatusResponse toUpdateResponse(Stop stop) {
        return UpdateStopStatusResponse.builder()
                .stopId(stop.stopId())
                .orderId(stop.orderId())
                .sequence(stop.sequence())
                .deliveryAddress(stop.deliveryAddress())
                .status(stop.status().name())
                .fechaEntrega(stop.deliveryDate())
                .build();
    }
}
