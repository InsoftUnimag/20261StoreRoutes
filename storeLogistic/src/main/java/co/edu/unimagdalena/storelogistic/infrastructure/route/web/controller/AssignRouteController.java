package co.edu.unimagdalena.storelogistic.infrastructure.route.web.controller;

import co.edu.unimagdalena.storelogistic.domain.route.ports.in.AssignOrderUseCase;
import co.edu.unimagdalena.storelogistic.domain.route.ports.in.GetRoutesUseCase;
import co.edu.unimagdalena.storelogistic.infrastructure.route.mapper.RouteAssignmentMapper;
import co.edu.unimagdalena.storelogistic.infrastructure.route.web.dto.AssignOrderRequest;
import co.edu.unimagdalena.storelogistic.infrastructure.route.web.dto.AssignOrderResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/logistics/routes")
@RequiredArgsConstructor
@Tag(name = "Route Assignment", description = "Internal API for assigning orders to delivery routes")
public class AssignRouteController {

    private final AssignOrderUseCase assignOrderUseCase;
    private final GetRoutesUseCase getRoutesUseCase;
    private final RouteAssignmentMapper mapper;

    @GetMapping
    public ResponseEntity<List<AssignOrderResponse>> listRoutes() {
        List<AssignOrderResponse> routes = getRoutesUseCase.getAll()
                .stream()
                .map(mapper::toAssignOrderResponse)
                .toList();
        return ResponseEntity.ok(routes);
    }

    @Operation(summary = "Assign an order to a route",
               description = "Assigns the order to an existing available route or creates a new one. Closes route at 95% capacity.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Order assigned to existing route"),
        @ApiResponse(responseCode = "201", description = "Order assigned to newly created route"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "404", description = "Order not found"),
        @ApiResponse(responseCode = "422", description = "Order weight exceeds maximum vehicle capacity")
    })
    @PostMapping("/assignments")
    public ResponseEntity<AssignOrderResponse> assignOrder(@Valid @RequestBody AssignOrderRequest request) {
        AssignOrderUseCase.AssignResult result = assignOrderUseCase.assign(request.getOrderId());
        AssignOrderResponse response = mapper.toAssignOrderResponse(result.route());
        HttpStatus status = result.isNewRoute() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(response);
    }
}
