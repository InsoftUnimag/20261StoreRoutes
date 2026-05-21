package co.edu.unimagdalena.storelogistic.infrastructure.orderstatus.web.controller;

import co.edu.unimagdalena.storelogistic.domain.orderstatus.exceptions.InvalidFinalStatusException;
import co.edu.unimagdalena.storelogistic.domain.orderstatus.models.Order;
import co.edu.unimagdalena.storelogistic.domain.orderstatus.ports.in.GetOrderHistoryUseCase;
import co.edu.unimagdalena.storelogistic.domain.orderstatus.ports.in.UpdateOrderStatusUseCase;
import co.edu.unimagdalena.storelogistic.domain.orderstatus.values.FinalStatus;
import co.edu.unimagdalena.storelogistic.infrastructure.orderstatus.mapper.OrderStatusMapper;
import co.edu.unimagdalena.storelogistic.infrastructure.orderstatus.web.dto.OrderHistoryItemResponse;
import co.edu.unimagdalena.storelogistic.infrastructure.orderstatus.web.dto.UpdateOrderStatusRequest;
import co.edu.unimagdalena.storelogistic.infrastructure.orderstatus.web.dto.UpdateOrderStatusResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/logistics/orders")
@RequiredArgsConstructor
public class UpdateOrderStatusController {

    private final UpdateOrderStatusUseCase updateOrderStatusUseCase;
    private final GetOrderHistoryUseCase getOrderHistoryUseCase;
    private final OrderStatusMapper mapper;

    @GetMapping("/carrier/{carrierId}/history")
    public ResponseEntity<List<OrderHistoryItemResponse>> getHistory(@PathVariable Long carrierId) {
        List<OrderHistoryItemResponse> history = getOrderHistoryUseCase.getByCarrier(carrierId)
                .stream()
                .map(mapper::toHistoryItemResponse)
                .toList();
        return ResponseEntity.ok(history);
    }

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
