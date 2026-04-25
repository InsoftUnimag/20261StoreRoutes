package co.edu.unimagdalena.storelogistic.orderstatus.infrastructure.web.controller;

import co.edu.unimagdalena.storelogistic.orderstatus.domain.exceptions.InvalidFinalStatusException;
import co.edu.unimagdalena.storelogistic.orderstatus.domain.models.Order;
import co.edu.unimagdalena.storelogistic.orderstatus.domain.ports.in.UpdateOrderStatusUseCase;
import co.edu.unimagdalena.storelogistic.orderstatus.domain.values.FinalStatus;
import co.edu.unimagdalena.storelogistic.orderstatus.infrastructure.mapper.OrderStatusMapper;
import co.edu.unimagdalena.storelogistic.orderstatus.infrastructure.web.dto.UpdateOrderStatusRequest;
import co.edu.unimagdalena.storelogistic.orderstatus.infrastructure.web.dto.UpdateOrderStatusResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/logistics/orders")
@RequiredArgsConstructor
public class UpdateOrderStatusController {

    private final UpdateOrderStatusUseCase updateOrderStatusUseCase;
    private final OrderStatusMapper mapper;

    @PutMapping("/{idPedido}/status")
    public ResponseEntity<UpdateOrderStatusResponse> updateStatus(
            @PathVariable Long idPedido,
            @Valid @RequestBody UpdateOrderStatusRequest request) {

        FinalStatus status = parseFinalStatus(request.getEstadoFinal());
        Order order = updateOrderStatusUseCase.update(idPedido, request.getIdTransportista(), status);
        return ResponseEntity.ok(mapper.toResponse(order));
    }

    private FinalStatus parseFinalStatus(String value) {
        try {
            return FinalStatus.fromDisplayName(value);
        } catch (IllegalArgumentException ex) {
            throw new InvalidFinalStatusException(value);
        }
    }
}
