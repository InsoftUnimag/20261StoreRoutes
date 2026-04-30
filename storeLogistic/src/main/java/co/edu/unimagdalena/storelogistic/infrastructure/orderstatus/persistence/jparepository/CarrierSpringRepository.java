package co.edu.unimagdalena.storelogistic.infrastructure.orderstatus.persistence.jparepository;

import co.edu.unimagdalena.storelogistic.infrastructure.orderstatus.persistence.jpa.CarrierJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CarrierSpringRepository extends JpaRepository<CarrierJpaEntity, Long> {

    boolean existsByTransporterId(Long transporterId);
}
