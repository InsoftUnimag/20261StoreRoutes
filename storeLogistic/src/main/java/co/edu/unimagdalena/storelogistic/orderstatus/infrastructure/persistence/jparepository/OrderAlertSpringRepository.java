package co.edu.unimagdalena.storelogistic.orderstatus.infrastructure.persistence.jparepository;

import co.edu.unimagdalena.storelogistic.orderstatus.infrastructure.persistence.jpa.OrderAlertJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderAlertSpringRepository extends JpaRepository<OrderAlertJpaEntity, Long> {
}
