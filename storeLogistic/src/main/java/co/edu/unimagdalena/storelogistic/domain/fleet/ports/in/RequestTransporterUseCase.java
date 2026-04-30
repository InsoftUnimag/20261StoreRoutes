package co.edu.unimagdalena.storelogistic.domain.fleet.ports.in;

import co.edu.unimagdalena.storelogistic.domain.fleet.models.Vehicle;

public interface RequestTransporterUseCase {
    Vehicle request(Long vehicleId);
}