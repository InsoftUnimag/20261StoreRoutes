package co.edu.unimagdalena.storelogistic.fleet.infrastructure.web.controller;

import co.edu.unimagdalena.storelogistic.fleet.domain.ports.in.ChangeVehicleStatusUseCase;
import co.edu.unimagdalena.storelogistic.fleet.domain.ports.in.GetVehicleUseCase;
import co.edu.unimagdalena.storelogistic.fleet.domain.ports.in.ListVehiclesUseCase;
import co.edu.unimagdalena.storelogistic.fleet.domain.ports.in.RegisterVehicleUseCase;
import co.edu.unimagdalena.storelogistic.fleet.domain.values.CategoryType;
import co.edu.unimagdalena.storelogistic.fleet.domain.values.LoadCapacity;
import co.edu.unimagdalena.storelogistic.fleet.domain.values.VehicleStatus;
import co.edu.unimagdalena.storelogistic.fleet.infrastructure.exception.ErrorResponse;
import co.edu.unimagdalena.storelogistic.fleet.infrastructure.mapper.VehicleMapper;
import co.edu.unimagdalena.storelogistic.fleet.infrastructure.web.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@Slf4j
@RestController
@RequestMapping("/vehicles")
@RequiredArgsConstructor
@Tag(name = "Fleet Management", description = "API for fleet vehicle administration")
public class FleetController {
    private final ListVehiclesUseCase listUseCase;
    private final GetVehicleUseCase getUseCase;
    private final RegisterVehicleUseCase registerUseCase;
    private final ChangeVehicleStatusUseCase changeStatusUseCase;
    private final VehicleMapper mapper;

    @GetMapping
    @Operation(summary = "List vehicles", description = "Returns the list of vehicles with optional filters")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Vehicle list retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid filter parameters",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ListVehiclesResponse> listVehicles(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Double minCapacity,
            @RequestParam(required = false) Double maxCapacity) {

        var filter = mapper.toVehicleFilter(category, status, minCapacity, maxCapacity);
        var vehicles = listUseCase.list(filter);
        return ResponseEntity.ok(mapper.toListVehiclesResponse(vehicles));
    }

    @GetMapping("/{vehicleId}")
    @Operation(summary = "Get vehicle detail", description = "Returns the detailed information of a specific vehicle")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Vehicle detail retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Vehicle not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<VehicleDetailResponse> getVehicle(@PathVariable Long vehicleId) {
        var vehicle = getUseCase.get(vehicleId);
        return ResponseEntity.ok(mapper.toVehicleDetailResponse(vehicle));
    }

    @PostMapping
    @Operation(summary = "Register new vehicle", description = "Creates a new vehicle with initial status 'in maintenance'")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Vehicle registered successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<RegisterVehicleResponse> registerVehicle(
            @Valid @RequestBody RegisterVehicleRequest request) {

        var vehicle = registerUseCase.register(
                CategoryType.valueOf(request.getCategory()),
                new LoadCapacity(BigDecimal.valueOf(request.getLoadCapacity())),
                request.getTransporterId());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mapper.toRegisterVehicleResponse(vehicle));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Change vehicle status", description = "Updates the vehicle status validating allowed transitions")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Status updated successfully"),
            @ApiResponse(responseCode = "404", description = "Vehicle not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Invalid state transition",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<VehicleDTO> changeVehicleStatus(
            @PathVariable Long id,
            @Valid @RequestBody ChangeStatusRequest request) {
        var vehicle = changeStatusUseCase.change(id, VehicleStatus.valueOf(request.getNewStatus()));
        return ResponseEntity.ok(mapper.toVehicleDTO(vehicle));
    }
}