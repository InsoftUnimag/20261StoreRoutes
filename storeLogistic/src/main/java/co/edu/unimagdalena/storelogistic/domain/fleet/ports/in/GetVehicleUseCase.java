package co.edu.unimagdalena.storelogistic.domain.fleet.ports.in;

import co.edu.unimagdalena.storelogistic.domain.fleet.models.Vehicle;

public interface GetVehicleUseCase {
    Vehicle get(Long vehicleId);
}