package co.edu.unimagdalena.storelogistic.domain.fleet.ports.in;

import co.edu.unimagdalena.storelogistic.domain.fleet.models.Vehicle;
import co.edu.unimagdalena.storelogistic.domain.fleet.values.CategoryType;
import co.edu.unimagdalena.storelogistic.domain.fleet.values.LoadCapacity;

public interface RegisterVehicleUseCase {
    Vehicle register(CategoryType category, LoadCapacity loadCapacity, Long transporterId);
}