package co.edu.unimagdalena.storelogistic.domain.fleet.ports.in;

import co.edu.unimagdalena.storelogistic.domain.fleet.models.Vehicle;
import co.edu.unimagdalena.storelogistic.domain.fleet.values.VehicleStatus;

public interface ChangeVehicleStatusUseCase {
    Vehicle change(Long vehicleId, VehicleStatus newStatus);
}