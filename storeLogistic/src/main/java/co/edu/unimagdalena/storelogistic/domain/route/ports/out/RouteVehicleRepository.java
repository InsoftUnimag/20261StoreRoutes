package co.edu.unimagdalena.storelogistic.domain.route.ports.out;

import co.edu.unimagdalena.storelogistic.domain.route.models.RouteVehicle;
import co.edu.unimagdalena.storelogistic.domain.route.values.VehicleType;

import java.util.Optional;

public interface RouteVehicleRepository {

    Optional<RouteVehicle> findAvailableByType(VehicleType type);

    Optional<RouteVehicle> findMaxCapacity();
}