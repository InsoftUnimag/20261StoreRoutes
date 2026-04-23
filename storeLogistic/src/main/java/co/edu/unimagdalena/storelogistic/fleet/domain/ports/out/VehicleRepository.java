package co.edu.unimagdalena.storelogistic.fleet.domain.ports.out;

import co.edu.unimagdalena.storelogistic.fleet.domain.models.Vehicle;
import co.edu.unimagdalena.storelogistic.fleet.domain.values.VehicleFilter;
import java.util.List;
import java.util.Optional;

public interface VehicleRepository {
    Vehicle save(Vehicle vehicle);
    Optional<Vehicle> findById(Long vehicleId);
    List<Vehicle> findWithFilters(VehicleFilter filter);
}