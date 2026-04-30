package co.edu.unimagdalena.storelogistic.infrastructure.orderstatus.persistence.jparepository;

import co.edu.unimagdalena.storelogistic.infrastructure.orderstatus.persistence.jpa.OrderStatusAuditJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderStatusAuditSpringRepository extends JpaRepository<OrderStatusAuditJpaEntity, Long> {

    List<OrderStatusAuditJpaEntity> findByOrderIdOrderByTimestampAsc(Long orderId);
}
