package co.edu.unimagdalena.storelogistic.orderstatus.infrastructure.persistence.repository;

import co.edu.unimagdalena.storelogistic.orderstatus.domain.models.Alert;
import co.edu.unimagdalena.storelogistic.orderstatus.domain.ports.out.AlertRepository;
import co.edu.unimagdalena.storelogistic.orderstatus.infrastructure.mapper.OrderStatusMapper;
import co.edu.unimagdalena.storelogistic.orderstatus.infrastructure.persistence.jpa.OrderAlertJpaEntity;
import co.edu.unimagdalena.storelogistic.orderstatus.infrastructure.persistence.jparepository.OrderAlertSpringRepository;
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
