package co.edu.unimagdalena.storelogistic.infrastructure.route.persistence.jparepository;

import co.edu.unimagdalena.storelogistic.infrastructure.route.persistence.jpa.OrderJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderSpringRepository extends JpaRepository<OrderJpaEntity, Long> {
}
