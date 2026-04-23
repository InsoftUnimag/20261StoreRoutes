package co.edu.unimagdalena.storelogistic.route.infrastructure.persistence.jparepository;

import co.edu.unimagdalena.storelogistic.route.infrastructure.persistence.jpa.StopJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StopSpringRepository extends JpaRepository<StopJpaEntity, Long> {

    List<StopJpaEntity> findByRouteIdOrderBySequence(Long routeId);
}
