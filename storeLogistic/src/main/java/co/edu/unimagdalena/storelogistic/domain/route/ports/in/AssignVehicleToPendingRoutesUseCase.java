package co.edu.unimagdalena.storelogistic.domain.route.ports.in;

import java.math.BigDecimal;

public interface AssignVehicleToPendingRoutesUseCase {
    void assignPending(Long vehicleId, BigDecimal capacityKg);
}
