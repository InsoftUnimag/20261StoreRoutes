package co.edu.unimagdalena.storelogistic.flota.domain.ports.in;

import co.edu.unimagdalena.storelogistic.flota.domain.models.Vehiculo;
import co.edu.unimagdalena.storelogistic.flota.domain.values.CapacidadCarga;
import co.edu.unimagdalena.storelogistic.flota.domain.values.TipoCategoria;

public interface RegistrarVehiculoUseCase {
    Vehiculo registrar(TipoCategoria categoria, CapacidadCarga capacidadCarga, String idTransportista);
}
