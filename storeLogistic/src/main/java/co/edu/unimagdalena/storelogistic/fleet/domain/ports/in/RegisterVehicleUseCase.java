package co.edu.unimagdalena.storelogistic.fleet.domain.ports.in;

import co.edu.unimagdalena.storelogistic.fleet.domain.models.Vehicle;
import co.edu.unimagdalena.storelogistic.fleet.domain.values.CategoryType;
import co.edu.unimagdalena.storelogistic.fleet.domain.values.LoadCapacity;

public interface RegisterVehicleUseCase {
    Vehicle register(CategoryType category, LoadCapacity loadCapacity, Long transporterId);
}