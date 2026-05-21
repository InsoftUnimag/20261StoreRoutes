package co.edu.unimagdalena.storelogistic.infrastructure.orderstatus.persistence.jparepository;

import co.edu.unimagdalena.storelogistic.infrastructure.orderstatus.persistence.jpa.OrderStatusJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderStatusSpringRepository extends JpaRepository<OrderStatusJpaEntity, Long> {

    List<OrderStatusJpaEntity> findByCarrierIdAndEstadoFinalIsNotNull(Long carrierId);
}
