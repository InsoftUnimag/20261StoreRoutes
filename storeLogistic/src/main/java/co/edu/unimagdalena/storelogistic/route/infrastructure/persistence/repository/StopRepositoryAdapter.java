package co.edu.unimagdalena.storelogistic.route.infrastructure.persistence.repository;

import co.edu.unimagdalena.storelogistic.route.domain.models.Stop;
import co.edu.unimagdalena.storelogistic.route.domain.ports.out.StopRepository;
import co.edu.unimagdalena.storelogistic.route.infrastructure.mapper.RouteAssignmentMapper;
import co.edu.unimagdalena.storelogistic.route.infrastructure.persistence.jpa.StopJpaEntity;
import co.edu.unimagdalena.storelogistic.route.infrastructure.persistence.jparepository.StopSpringRepository;
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
}
