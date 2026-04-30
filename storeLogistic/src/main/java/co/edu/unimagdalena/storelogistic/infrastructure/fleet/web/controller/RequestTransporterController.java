package co.edu.unimagdalena.storelogistic.infrastructure.fleet.web.controller;

import co.edu.unimagdalena.storelogistic.domain.fleet.ports.in.RequestTransporterUseCase;
import co.edu.unimagdalena.storelogistic.infrastructure.fleet.exception.ErrorResponse;
import co.edu.unimagdalena.storelogistic.infrastructure.fleet.mapper.VehicleTransporterMapper;
import co.edu.unimagdalena.storelogistic.infrastructure.fleet.web.dto.RequestTransporterResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/logistics/vehicles")
@RequiredArgsConstructor
@Tag(name = "Transporter Assignment", description = "API for requesting and assigning transporters to vehicles")
public class RequestTransporterController {

    private final RequestTransporterUseCase requestTransporterUseCase;
    private final VehicleTransporterMapper mapper;

    @PostMapping("/{vehicleId}/transporter")
    @Operation(
            summary = "Request and assign a transporter to a vehicle",
            description = "Requests an available transporter from the external module and assigns it to the specified vehicle."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transporter assigned successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid transporter ID",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Vehicle not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "No transporter available",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<RequestTransporterResponse> requestTransporter(@PathVariable Long vehicleId) {
        log.info("POST /logistics/vehicles/{}/transporter", vehicleId);
        var vehicle = requestTransporterUseCase.request(vehicleId);
        return ResponseEntity.ok(mapper.toResponse(vehicle));
    }
}