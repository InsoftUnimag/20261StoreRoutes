package co.edu.unimagdalena.storelogistic.flota.domain.models;

import co.edu.unimagdalena.storelogistic.flota.domain.exceptions.TransicionEstadoInvalidaException;
import co.edu.unimagdalena.storelogistic.flota.domain.values.EstadoVehiculo;
import co.edu.unimagdalena.storelogistic.flota.domain.values.CapacidadCarga;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Optional;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vehiculo {
    private Long idVehiculo;
    private Long idCategoria;
    private CapacidadCarga capacidadCarga;
    private EstadoVehiculo estado;
    private String idTransportista;
    private BigDecimal pesoActual;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public BigDecimal porcentajeOcupacion() {
        return Optional.ofNullable(capacidadCarga)
                .filter(c -> c.getPesoKg().compareTo(BigDecimal.ZERO) != 0)
                .map(c -> Optional.ofNullable(pesoActual).orElse(BigDecimal.ZERO)
                        .divide(c.getPesoKg(), 2, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)))
                .orElse(BigDecimal.ZERO);
    }

    public static Vehiculo registrarNuevo(Categoria categoria, CapacidadCarga capacidadCarga, String idTransportista) {
        return Vehiculo.builder()
                .idCategoria(categoria.getIdCategoria())
                .capacidadCarga(capacidadCarga)
                .estado(EstadoVehiculo.EN_MANTENIMIENTO)
                .idTransportista(idTransportista)
                .pesoActual(BigDecimal.ZERO)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public void cambiarEstado(EstadoVehiculo nuevoEstado) {
        Optional.ofNullable(nuevoEstado)
                .filter(e -> estado.esTransicionValida(e))
                .orElseThrow(() -> new TransicionEstadoInvalidaException(
                        estado.obtenerMensajeTransicionInvalida(nuevoEstado)
                ));
        this.estado = nuevoEstado;
        this.updatedAt = LocalDateTime.now();
    }
}

