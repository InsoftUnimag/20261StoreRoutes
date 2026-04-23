package co.edu.unimagdalena.storelogistic.fleet.domain.ports.in;

import co.edu.unimagdalena.storelogistic.fleet.domain.models.Vehicle;

public interface GetVehicleUseCase {
    Vehicle get(Long vehicleId);
}