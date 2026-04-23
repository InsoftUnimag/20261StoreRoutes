package co.edu.unimagdalena.storelogistic.route.domain.ports.out;

import co.edu.unimagdalena.storelogistic.route.domain.models.RouteVehicle;
import co.edu.unimagdalena.storelogistic.route.domain.values.VehicleType;

import java.util.Optional;

public interface RouteVehicleRepository {

    Optional<RouteVehicle> findAvailableByType(VehicleType type);

    Optional<RouteVehicle> findMaxCapacity();
}