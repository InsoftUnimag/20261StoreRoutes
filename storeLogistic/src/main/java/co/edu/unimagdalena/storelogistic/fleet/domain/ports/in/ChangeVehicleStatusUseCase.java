package co.edu.unimagdalena.storelogistic.fleet.domain.ports.in;

import co.edu.unimagdalena.storelogistic.fleet.domain.models.Vehicle;
import co.edu.unimagdalena.storelogistic.fleet.domain.values.VehicleStatus;

public interface ChangeVehicleStatusUseCase {
    Vehicle change(Long vehicleId, VehicleStatus newStatus);
}