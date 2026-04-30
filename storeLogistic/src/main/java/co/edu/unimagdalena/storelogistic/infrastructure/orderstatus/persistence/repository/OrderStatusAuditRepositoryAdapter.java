package co.edu.unimagdalena.storelogistic.infrastructure.orderstatus.persistence.repository;

import co.edu.unimagdalena.storelogistic.domain.orderstatus.models.OrderStatusAudit;
import co.edu.unimagdalena.storelogistic.domain.orderstatus.ports.out.OrderStatusAuditRepository;
import co.edu.unimagdalena.storelogistic.infrastructure.orderstatus.mapper.OrderStatusMapper;
import co.edu.unimagdalena.storelogistic.infrastructure.orderstatus.persistence.jpa.OrderStatusAuditJpaEntity;
import co.edu.unimagdalena.storelogistic.infrastructure.orderstatus.persistence.jparepository.OrderStatusAuditSpringRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderStatusAuditRepositoryAdapter implements OrderStatusAuditRepository {

    private final OrderStatusAuditSpringRepository springRepository;
    private final OrderStatusMapper mapper;

    @Override
    public OrderStatusAudit save(OrderStatusAudit audit) {
        OrderStatusAuditJpaEntity entity = mapper.toAuditEntity(audit);
        OrderStatusAuditJpaEntity saved = springRepository.save(entity);
        return mapper.toAuditDomain(saved);
    }
}
