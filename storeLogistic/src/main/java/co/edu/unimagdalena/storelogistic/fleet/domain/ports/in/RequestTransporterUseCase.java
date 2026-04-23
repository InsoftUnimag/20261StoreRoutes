package co.edu.unimagdalena.storelogistic.fleet.domain.ports.in;

import co.edu.unimagdalena.storelogistic.fleet.domain.models.Vehicle;

public interface RequestTransporterUseCase {
    Vehicle request(Long vehicleId);
}