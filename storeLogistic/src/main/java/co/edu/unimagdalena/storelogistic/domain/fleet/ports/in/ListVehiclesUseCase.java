package co.edu.unimagdalena.storelogistic.domain.fleet.ports.in;

import co.edu.unimagdalena.storelogistic.domain.fleet.models.Vehicle;
import co.edu.unimagdalena.storelogistic.domain.fleet.values.VehicleFilter;
import java.util.List;

public interface ListVehiclesUseCase {
    List<Vehicle> list(VehicleFilter filter);
}