package co.edu.unimagdalena.storelogistic.orderstatus.infrastructure.persistence.jparepository;

import co.edu.unimagdalena.storelogistic.orderstatus.infrastructure.persistence.jpa.OrderStatusJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderStatusSpringRepository extends JpaRepository<OrderStatusJpaEntity, Long> {
}
