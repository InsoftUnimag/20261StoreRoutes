package co.edu.unimagdalena.storelogistic.fleet.application.services;

import co.edu.unimagdalena.storelogistic.fleet.domain.exceptions.VehicleNotFoundException;
import co.edu.unimagdalena.storelogistic.fleet.domain.models.Vehicle;
import co.edu.unimagdalena.storelogistic.fleet.domain.ports.in.RequestTransporterUseCase;
import co.edu.unimagdalena.storelogistic.fleet.domain.ports.out.TransporterServicePort;
import co.edu.unimagdalena.storelogistic.fleet.domain.ports.out.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RequestTransporterService implements RequestTransporterUseCase {

    private final VehicleRepository vehicleRepository;
    private final TransporterServicePort transporterServicePort;

    @Override
    @Transactional
    public Vehicle request(Long vehicleId) {
        log.info("Requesting transporter assignment for vehicleId={}", vehicleId);

        var vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new VehicleNotFoundException(vehicleId));

        var transporterId = transporterServicePort.getAvailable();
        log.info("Available transporter obtained: {}", transporterId);

        transporterServicePort.validateExistence(transporterId);

        vehicle.assignTransporter(transporterId);
        var saved = vehicleRepository.save(vehicle);

        log.info("Transporter {} assigned successfully to vehicle {}", transporterId, vehicleId);
        return saved;
    }
}