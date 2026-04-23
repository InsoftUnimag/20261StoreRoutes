package co.edu.unimagdalena.storelogistic.fleet.application.services;

import co.edu.unimagdalena.storelogistic.fleet.domain.models.Vehicle;
import co.edu.unimagdalena.storelogistic.fleet.domain.ports.in.ListVehiclesUseCase;
import co.edu.unimagdalena.storelogistic.fleet.domain.ports.out.VehicleRepository;
import co.edu.unimagdalena.storelogistic.fleet.domain.values.VehicleFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ListVehiclesService implements ListVehiclesUseCase {
    private final VehicleRepository vehicleRepository;

    @Override
    public List<Vehicle> list(VehicleFilter filter) {
        log.info("Listing vehicles with filters: {}", filter);
        return vehicleRepository.findWithFilters(filter);
    }
}