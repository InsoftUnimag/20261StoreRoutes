package co.edu.unimagdalena.storelogistic.route.application.services;

import co.edu.unimagdalena.storelogistic.route.domain.exceptions.CapacityExceededException;
import co.edu.unimagdalena.storelogistic.route.domain.models.RouteVehicle;
import co.edu.unimagdalena.storelogistic.route.domain.ports.out.RouteVehicleRepository;
import co.edu.unimagdalena.storelogistic.route.domain.values.LogisticWeight;
import co.edu.unimagdalena.storelogistic.route.domain.values.VehicleType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SelectVehicleService {

    private final RouteVehicleRepository vehicleRepository;

    public Optional<RouteVehicle> select(LogisticWeight weight) {
        VehicleType requiredType = VehicleType.forWeight(weight);
        log.info("Selecting vehicle of type {} for weight {} kg", requiredType, weight.valueKg());

        Optional<RouteVehicle> vehicle = vehicleRepository.findAvailableByType(requiredType);
        if (vehicle.isPresent()) {
            log.info("Found available vehicle id={} type={}", vehicle.get().vehicleId(), requiredType);
            return vehicle;
        }

        log.warn("No vehicle of type {} available, trying fallback to max-capacity vehicle", requiredType);
        Optional<RouteVehicle> fallback = vehicleRepository.findMaxCapacity();

        if (fallback.isPresent()) {
            RouteVehicle v = fallback.get();
            if (weight.valueKg().compareTo(v.capacity().valueKg()) > 0) {
                throw new CapacityExceededException(
                    "Weight " + weight.valueKg() + " kg exceeds max available vehicle capacity of " +
                    v.capacity().valueKg() + " kg"
                );
            }
            log.warn("Using fallback vehicle id={} with capacity {} kg", v.vehicleId(), v.capacity().valueKg());
            return fallback;
        }

        log.warn("No vehicles available at all for weight {} kg", weight.valueKg());
        return Optional.empty();
    }
}