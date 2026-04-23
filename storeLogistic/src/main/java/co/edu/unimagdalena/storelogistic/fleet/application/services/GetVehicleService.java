package co.edu.unimagdalena.storelogistic.fleet.application.services;

import co.edu.unimagdalena.storelogistic.fleet.domain.exceptions.VehicleNotFoundException;
import co.edu.unimagdalena.storelogistic.fleet.domain.models.Vehicle;
import co.edu.unimagdalena.storelogistic.fleet.domain.ports.in.GetVehicleUseCase;
import co.edu.unimagdalena.storelogistic.fleet.domain.ports.out.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class GetVehicleService implements GetVehicleUseCase {
    private final VehicleRepository vehicleRepository;

    @Override
    public Vehicle get(Long vehicleId) {
        log.info("Fetching vehicle with ID: {}", vehicleId);
        var vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new VehicleNotFoundException(vehicleId));
        log.info("Vehicle found: ID={}, status={}", vehicle.getVehicleId(), vehicle.getStatus());
        return vehicle;
    }
}