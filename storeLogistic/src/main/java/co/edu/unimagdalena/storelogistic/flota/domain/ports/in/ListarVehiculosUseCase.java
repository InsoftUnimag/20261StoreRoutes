package co.edu.unimagdalena.storelogistic.flota.domain.ports.in;

import co.edu.unimagdalena.storelogistic.flota.domain.models.Vehiculo;
import co.edu.unimagdalena.storelogistic.flota.domain.values.FiltroVehiculo;
import java.util.List;

public interface ListarVehiculosUseCase {
    List<Vehiculo> listar(FiltroVehiculo filtro);
}
