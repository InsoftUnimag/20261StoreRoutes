package co.edu.unimagdalena.storelogistic.application.route.services;

import co.edu.unimagdalena.storelogistic.domain.route.ports.in.AssignVehicleToPendingRoutesUseCase;
import co.edu.unimagdalena.storelogistic.domain.route.ports.out.RouteRepository;
import co.edu.unimagdalena.storelogistic.domain.route.values.RouteCapacity;
import co.edu.unimagdalena.storelogistic.domain.route.values.RouteStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssignVehicleToPendingRoutesService implements AssignVehicleToPendingRoutesUseCase {

    private final RouteRepository routeRepository;

    @Override
    @Transactional
    public void assignPending(Long vehicleId, BigDecimal capacityKg) {
        var pending = routeRepository.findByStatus(RouteStatus.PENDING_VEHICLE);

        for (var route : pending) {
            if (route.accumulatedWeightKg().compareTo(capacityKg) <= 0) {
                var capacity = RouteCapacity.of(capacityKg);

                log.info("Assigning vehicle id={} (capacity={} kg) to pending route id={} (weight={} kg)",
                        vehicleId, capacityKg, route.routeId(), route.accumulatedWeightKg());

                route.assignVehicle(vehicleId, capacity);
                routeRepository.save(route);

                log.info("Route id={} updated from PENDING_VEHICLE to AVAILABLE with vehicle id={}",
                        route.routeId(), vehicleId);
                return;
            }
        }

        log.info("No PENDING_VEHICLE route with accumulated weight <= {} kg found for vehicle id={}",
                capacityKg, vehicleId);
    }
}
