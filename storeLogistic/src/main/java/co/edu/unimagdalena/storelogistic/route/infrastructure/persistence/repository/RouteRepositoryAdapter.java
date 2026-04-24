package co.edu.unimagdalena.storelogistic.route.infrastructure.persistence.repository;

import co.edu.unimagdalena.storelogistic.route.domain.models.Route;
import co.edu.unimagdalena.storelogistic.route.domain.ports.out.RouteRepository;
import co.edu.unimagdalena.storelogistic.route.domain.values.LogisticWeight;
import co.edu.unimagdalena.storelogistic.route.infrastructure.mapper.RouteAssignmentMapper;
import co.edu.unimagdalena.storelogistic.route.infrastructure.persistence.jpa.RouteJpaEntity;
import co.edu.unimagdalena.storelogistic.route.infrastructure.persistence.jparepository.RouteSpringRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RouteRepositoryAdapter implements RouteRepository {

    private final RouteSpringRepository springRepository;
    private final RouteAssignmentMapper mapper;

    @Override
    public Optional<Route> findAvailableWithCapacity(LogisticWeight weight) {
        return springRepository.findBestAvailableWithCapacity(weight.valueKg())
                .map(mapper::toRoute);
    }

    @Override
    public Route save(Route route) {
        RouteJpaEntity entity = mapper.toRouteEntity(route);
        RouteJpaEntity saved = springRepository.save(entity);
        return mapper.toRoute(saved);
    }

    @Override
    public Optional<Route> findById(Long routeId) {
        return springRepository.findById(routeId).map(mapper::toRoute);
    }
}
