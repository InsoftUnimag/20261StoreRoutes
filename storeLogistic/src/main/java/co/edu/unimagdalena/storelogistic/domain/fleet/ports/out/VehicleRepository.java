package co.edu.unimagdalena.storelogistic.domain.fleet.ports.out;

import co.edu.unimagdalena.storelogistic.domain.fleet.models.Vehicle;
import co.edu.unimagdalena.storelogistic.domain.fleet.values.VehicleFilter;
import java.util.List;
import java.util.Optional;

public interface VehicleRepository {
    Vehicle save(Vehicle vehicle);
    Optional<Vehicle> findById(Long vehicleId);
    List<Vehicle> findWithFilters(VehicleFilter filter);
}