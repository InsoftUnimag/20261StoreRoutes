package co.edu.unimagdalena.storelogistic.infrastructure.route.persistence.repository;

import co.edu.unimagdalena.storelogistic.domain.route.models.Stop;
import co.edu.unimagdalena.storelogistic.domain.route.ports.out.StopRepository;
import co.edu.unimagdalena.storelogistic.infrastructure.route.mapper.RouteAssignmentMapper;
import co.edu.unimagdalena.storelogistic.infrastructure.route.persistence.jpa.StopJpaEntity;
import co.edu.unimagdalena.storelogistic.infrastructure.route.persistence.jparepository.StopSpringRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class StopRepositoryAdapter implements StopRepository {

    private final StopSpringRepository springRepository;
    private final RouteAssignmentMapper mapper;

    @Override
    public Stop save(Stop stop) {
        StopJpaEntity entity = mapper.toStopEntity(stop);
        StopJpaEntity saved = springRepository.save(entity);
        return mapper.toStop(saved);
    }

    @Override
    public List<Stop> findByRouteId(Long routeId) {
        return springRepository.findByRouteIdOrderBySequence(routeId)
                .stream()
                .map(mapper::toStop)
                .toList();
    }

    @Override
    public List<Stop> findByRouteIdOrderBySequence(Long routeId) {
        return springRepository.findByRouteIdOrderBySequence(routeId)
                .stream()
                .map(mapper::toStop)
                .toList();
    }

    @Override
    public java.util.Optional<Stop> findByIdAndRouteId(Long stopId, Long routeId) {
        return springRepository.findByStopIdAndRouteId(stopId, routeId)
                .map(mapper::toStop);
    }
}
