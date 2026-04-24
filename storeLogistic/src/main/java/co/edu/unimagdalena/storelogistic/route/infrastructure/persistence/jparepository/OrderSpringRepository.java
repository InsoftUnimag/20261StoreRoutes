package co.edu.unimagdalena.storelogistic.route.infrastructure.persistence.jparepository;

import co.edu.unimagdalena.storelogistic.route.infrastructure.persistence.jpa.OrderJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderSpringRepository extends JpaRepository<OrderJpaEntity, Long> {
}
