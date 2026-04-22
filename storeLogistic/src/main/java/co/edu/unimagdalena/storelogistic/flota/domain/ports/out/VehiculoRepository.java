package co.edu.unimagdalena.storelogistic.flota.domain.ports.out;

import co.edu.unimagdalena.storelogistic.flota.domain.models.Vehiculo;
import co.edu.unimagdalena.storelogistic.flota.domain.values.FiltroVehiculo;
import co.edu.unimagdalena.storelogistic.flota.domain.values.TipoCategoria;
import java.util.List;
import java.util.Optional;

public interface VehiculoRepository {
    Vehiculo save(Vehiculo vehiculo);
    Optional<Vehiculo> findById(Long idVehiculo);
    List<Vehiculo> findWithFilters(FiltroVehiculo filtro);
}
