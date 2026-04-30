package co.edu.unimagdalena.storelogistic.application.fleet.services;

import co.edu.unimagdalena.storelogistic.domain.fleet.models.Vehicle;
import co.edu.unimagdalena.storelogistic.domain.fleet.ports.in.RegisterVehicleUseCase;
import co.edu.unimagdalena.storelogistic.domain.fleet.ports.out.CategoryRepository;
import co.edu.unimagdalena.storelogistic.domain.fleet.ports.out.VehicleRepository;
import co.edu.unimagdalena.storelogistic.domain.fleet.values.CategoryType;
import co.edu.unimagdalena.storelogistic.domain.fleet.values.LoadCapacity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RegisterVehicleService implements RegisterVehicleUseCase {
    private final VehicleRepository vehicleRepository;
    private final CategoryRepository categoryRepository;

    @Override
    public Vehicle register(CategoryType category, LoadCapacity loadCapacity, Long transporterId) {
        log.info("Registering new vehicle: category={}, capacity={}, transporterId={}",
                category, loadCapacity, transporterId);

        var cat = categoryRepository.findByType(category)
                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + category));

        var vehicle = Vehicle.createNew(cat, loadCapacity, transporterId);
        var saved = vehicleRepository.save(vehicle);

        log.info("Vehicle registered successfully with ID: {}", saved.getVehicleId());
        return saved;
    }
}