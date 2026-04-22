package co.edu.unimagdalena.storelogistic.flota.domain.ports.in;

import co.edu.unimagdalena.storelogistic.flota.domain.models.Vehiculo;

public interface ObtenerVehiculoUseCase {
    Vehiculo obtener(Long idVehiculo);
}

