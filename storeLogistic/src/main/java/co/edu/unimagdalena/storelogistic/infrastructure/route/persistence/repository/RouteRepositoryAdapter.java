package co.edu.unimagdalena.storelogistic.infrastructure.route.persistence.repository;

import co.edu.unimagdalena.storelogistic.domain.route.models.Route;
import co.edu.unimagdalena.storelogistic.domain.route.ports.out.RouteRepository;
import co.edu.unimagdalena.storelogistic.domain.route.values.LogisticWeight;
import co.edu.unimagdalena.storelogistic.domain.route.values.RouteStatus;
import co.edu.unimagdalena.storelogistic.infrastructure.route.mapper.RouteAssignmentMapper;
import co.edu.unimagdalena.storelogistic.infrastructure.route.persistence.jpa.RouteJpaEntity;
import co.edu.unimagdalena.storelogistic.infrastructure.route.persistence.jparepository.RouteSpringRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
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
    public List<Route> findByStatus(RouteStatus status) {
        return springRepository.findByStatus(status.name())
                .stream().map(mapper::toRoute).toList();
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

    @Override
    public List<Route> findAll() {
        return springRepository.findAll().stream().map(mapper::toRoute).toList();
    }
}
