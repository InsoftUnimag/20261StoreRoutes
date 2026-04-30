package co.edu.unimagdalena.storelogistic.infrastructure.orderstatus.persistence.repository;

import co.edu.unimagdalena.storelogistic.domain.orderstatus.models.Alert;
import co.edu.unimagdalena.storelogistic.domain.orderstatus.ports.out.AlertRepository;
import co.edu.unimagdalena.storelogistic.infrastructure.orderstatus.mapper.OrderStatusMapper;
import co.edu.unimagdalena.storelogistic.infrastructure.orderstatus.persistence.jpa.OrderAlertJpaEntity;
import co.edu.unimagdalena.storelogistic.infrastructure.orderstatus.persistence.jparepository.OrderAlertSpringRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AlertRepositoryAdapter implements AlertRepository {

    private final OrderAlertSpringRepository springRepository;
    private final OrderStatusMapper mapper;

    @Override
    public Alert save(Alert alert) {
        OrderAlertJpaEntity entity = mapper.toAlertEntity(alert);
        OrderAlertJpaEntity saved = springRepository.save(entity);
        return mapper.toAlertDomain(saved);
    }
}
