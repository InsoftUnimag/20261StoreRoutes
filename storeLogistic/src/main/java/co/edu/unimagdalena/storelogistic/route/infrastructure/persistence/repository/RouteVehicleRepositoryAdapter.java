package co.edu.unimagdalena.storelogistic.route.infrastructure.persistence.repository;

import co.edu.unimagdalena.storelogistic.route.domain.models.RouteVehicle;
import co.edu.unimagdalena.storelogistic.route.domain.ports.out.RouteVehicleRepository;
import co.edu.unimagdalena.storelogistic.route.domain.values.VehicleType;
import co.edu.unimagdalena.storelogistic.route.infrastructure.mapper.RouteAssignmentMapper;
import co.edu.unimagdalena.storelogistic.route.infrastructure.persistence.jparepository.RouteVehicleSpringRepository;
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
