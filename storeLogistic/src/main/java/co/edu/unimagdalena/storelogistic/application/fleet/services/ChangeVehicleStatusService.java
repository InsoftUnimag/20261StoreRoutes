package co.edu.unimagdalena.storelogistic.application.fleet.services;

import co.edu.unimagdalena.storelogistic.domain.fleet.exceptions.VehicleNotFoundException;
import co.edu.unimagdalena.storelogistic.domain.fleet.models.Vehicle;
import co.edu.unimagdalena.storelogistic.domain.fleet.ports.in.ChangeVehicleStatusUseCase;
import co.edu.unimagdalena.storelogistic.domain.fleet.ports.out.VehicleRepository;
import co.edu.unimagdalena.storelogistic.domain.fleet.values.VehicleStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChangeVehicleStatusService implements ChangeVehicleStatusUseCase {
    private final VehicleRepository vehicleRepository;

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Vehicle change(Long vehicleId, VehicleStatus newStatus) {
        log.info("Changing status of vehicle ID: {} to: {}", vehicleId, newStatus);

        var vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new VehicleNotFoundException(vehicleId));

        var previousStatus = vehicle.getStatus();
        vehicle.changeStatus(newStatus);
        var updated = vehicleRepository.save(vehicle);

        log.info("Vehicle {} status changed successfully from {} to {}",
                vehicleId, previousStatus, newStatus);
        return updated;
    }
}