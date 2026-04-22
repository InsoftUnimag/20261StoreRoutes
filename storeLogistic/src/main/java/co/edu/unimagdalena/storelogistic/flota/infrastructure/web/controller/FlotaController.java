package co.edu.unimagdalena.storelogistic.flota.infrastructure.web.controller;

import co.edu.unimagdalena.storelogistic.flota.domain.ports.in.ListarVehiculosUseCase;
import co.edu.unimagdalena.storelogistic.flota.domain.ports.in.RegistrarVehiculoUseCase;
import co.edu.unimagdalena.storelogistic.flota.domain.ports.in.CambiarEstadoVehiculoUseCase;
import co.edu.unimagdalena.storelogistic.flota.domain.ports.in.ObtenerVehiculoUseCase;
import co.edu.unimagdalena.storelogistic.flota.domain.values.EstadoVehiculo;
import co.edu.unimagdalena.storelogistic.flota.domain.values.TipoCategoria;
import co.edu.unimagdalena.storelogistic.flota.domain.values.CapacidadCarga;
import co.edu.unimagdalena.storelogistic.flota.infrastructure.web.dto.*;
import co.edu.unimagdalena.storelogistic.flota.infrastructure.exception.ErrorResponse;
import co.edu.unimagdalena.storelogistic.flota.infrastructure.mapper.VehiculoMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Slf4j
@RestController
@RequestMapping("/vehiculos")
@RequiredArgsConstructor
@Tag(name = "Gestión de Flota", description = "API para administración de vehículos de flota")
public class FlotaController {
    private final ListarVehiculosUseCase listarUseCase;
    private final ObtenerVehiculoUseCase obtenerUseCase;
    private final RegistrarVehiculoUseCase registrarUseCase;
    private final CambiarEstadoVehiculoUseCase cambiarEstadoUseCase;
    private final VehiculoMapper mapper;

    @GetMapping
    @Operation(summary = "Listar vehículos", description = "Obtiene la lista de vehículos con filtros opcionales")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Listado de vehículos obtenido correctamente"),
            @ApiResponse(responseCode = "400", description = "Parámetros de filtro inválidos", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ListaVehiculosResponse> listarVehiculos(
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) Double capacidadMin,
            @RequestParam(required = false) Double capacidadMax) {

        var filtro = mapper.toFiltroVehiculo(categoria, estado, capacidadMin, capacidadMax);
        var vehiculos = listarUseCase.listar(filtro);
        var response = mapper.toListaVehiculosResponse(vehiculos);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{idVehiculo}")
    @Operation(summary = "Obtener detalle de vehículo", description = "Obtiene la información detallada de un vehículo específico")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Detalle del vehículo obtenido correctamente"),
            @ApiResponse(responseCode = "404", description = "Vehículo no encontrado", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<VehiculoDetailResponse> obtenerVehiculo(@PathVariable Long idVehiculo) {
        var vehiculo = obtenerUseCase.obtener(idVehiculo);

        var response = mapper.toVehiculoDetailResponse(vehiculo);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @Operation(summary = "Registrar nuevo vehículo", description = "Crea un nuevo vehículo con estado inicial 'en mantenimiento'")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Vehículo registrado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<RegistrarVehiculoResponse> registrarVehiculo(
            @Valid @RequestBody RegistrarVehiculoRequest request) {

        var vehiculo = registrarUseCase.registrar(
                TipoCategoria.valueOf(request.getCategoria()),
                new CapacidadCarga(BigDecimal.valueOf(request.getCapacidadCarga())),
                request.getIdTransportista());

        var response = mapper.toRegistrarVehiculoResponse(vehiculo);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{id}/estado")
    @Operation(summary = "Cambiar estado del vehículo", description = "Actualiza el estado del vehículo validando transiciones permitidas")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Estado actualizado correctamente"),
            @ApiResponse(responseCode = "404", description = "Vehículo no encontrado", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Transición de estado inválida", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<VehiculoDTO> cambiarEstadoVehiculo(
            @PathVariable Long id,
            @Valid @RequestBody CambiarEstadoRequest request) {
        var vehiculo = cambiarEstadoUseCase.cambiar(
                id,
                EstadoVehiculo.valueOf(request.getNuevoEstado()));
        var response = mapper.toVehiculoDTO(vehiculo);
        return ResponseEntity.ok(response);
    }
}
