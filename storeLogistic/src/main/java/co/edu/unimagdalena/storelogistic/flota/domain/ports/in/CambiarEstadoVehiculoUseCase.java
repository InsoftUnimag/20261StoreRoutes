package co.edu.unimagdalena.storelogistic.flota.domain.ports.in;

import co.edu.unimagdalena.storelogistic.flota.domain.models.Vehiculo;
import co.edu.unimagdalena.storelogistic.flota.domain.values.EstadoVehiculo;

public interface CambiarEstadoVehiculoUseCase {
    Vehiculo cambiar(Long idVehiculo, EstadoVehiculo nuevoEstado);
}
