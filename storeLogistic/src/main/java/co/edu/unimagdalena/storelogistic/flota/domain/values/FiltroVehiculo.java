package co.edu.unimagdalena.storelogistic.flota.domain.values;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.Optional;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class FiltroVehiculo {
    private TipoCategoria categoria;
    private CapacidadCarga capacidadMin;
    private CapacidadCarga capacidadMax;
    private EstadoVehiculo estado;


    public boolean esValido() {
        return Optional.ofNullable(capacidadMin)
                .flatMap(min -> Optional.ofNullable(capacidadMax)
                        .map(max -> min.getPesoKg().compareTo(max.getPesoKg()) <= 0))
                .orElse(true);
    }
}
