package co.edu.unimagdalena.storelogistic.infrastructure.orderstatus.persistence.jparepository;

import co.edu.unimagdalena.storelogistic.infrastructure.orderstatus.persistence.jpa.OrderAlertJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderAlertSpringRepository extends JpaRepository<OrderAlertJpaEntity, Long> {
}
