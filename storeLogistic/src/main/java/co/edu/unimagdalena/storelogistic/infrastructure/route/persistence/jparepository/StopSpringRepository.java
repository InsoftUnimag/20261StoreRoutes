package co.edu.unimagdalena.storelogistic.infrastructure.route.persistence.jparepository;

import co.edu.unimagdalena.storelogistic.infrastructure.route.persistence.jpa.StopJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StopSpringRepository extends JpaRepository<StopJpaEntity, Long> {

    List<StopJpaEntity> findByRouteIdOrderBySequence(Long routeId);

    java.util.Optional<StopJpaEntity> findByStopIdAndRouteId(Long stopId, Long routeId);

    java.util.Optional<StopJpaEntity> findByOrderId(Long orderId);
    boolean existsByOrderId(Long orderId);
}
