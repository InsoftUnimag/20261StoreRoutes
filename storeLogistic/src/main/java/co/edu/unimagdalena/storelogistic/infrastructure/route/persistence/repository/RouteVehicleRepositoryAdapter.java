package co.edu.unimagdalena.storelogistic.infrastructure.route.persistence.repository;

import co.edu.unimagdalena.storelogistic.domain.route.models.RouteVehicle;
import co.edu.unimagdalena.storelogistic.domain.route.ports.out.RouteVehicleRepository;
import co.edu.unimagdalena.storelogistic.domain.route.values.VehicleType;
import co.edu.unimagdalena.storelogistic.infrastructure.route.mapper.RouteAssignmentMapper;
import co.edu.unimagdalena.storelogistic.infrastructure.route.persistence.jparepository.RouteVehicleSpringRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RouteVehicleRepositoryAdapter implements RouteVehicleRepository {

    private final RouteVehicleSpringRepository springRepository;
    private final RouteAssignmentMapper mapper;

    @Override
    public Optional<RouteVehicle> findAvailableByType(VehicleType type) {
        return springRepository.findFirstAvailableByCategory(type.categoryName())
                .map(mapper::toRouteVehicle);
    }

    @Override
    public Optional<RouteVehicle> findMaxCapacity() {
        return springRepository.findTopAvailableByMaxCapacity()
                .map(mapper::toRouteVehicle);
    }
}
