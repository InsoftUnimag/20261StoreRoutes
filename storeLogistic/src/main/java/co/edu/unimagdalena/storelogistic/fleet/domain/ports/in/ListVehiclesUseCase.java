package co.edu.unimagdalena.storelogistic.fleet.domain.ports.in;

import co.edu.unimagdalena.storelogistic.fleet.domain.models.Vehicle;
import co.edu.unimagdalena.storelogistic.fleet.domain.values.VehicleFilter;
import java.util.List;

public interface ListVehiclesUseCase {
    List<Vehicle> list(VehicleFilter filter);
}