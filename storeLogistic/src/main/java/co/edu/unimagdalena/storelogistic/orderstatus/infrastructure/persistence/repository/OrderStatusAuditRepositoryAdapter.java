package co.edu.unimagdalena.storelogistic.orderstatus.infrastructure.persistence.repository;

import co.edu.unimagdalena.storelogistic.orderstatus.domain.models.OrderStatusAudit;
import co.edu.unimagdalena.storelogistic.orderstatus.domain.ports.out.OrderStatusAuditRepository;
import co.edu.unimagdalena.storelogistic.orderstatus.infrastructure.mapper.OrderStatusMapper;
import co.edu.unimagdalena.storelogistic.orderstatus.infrastructure.persistence.jpa.OrderStatusAuditJpaEntity;
import co.edu.unimagdalena.storelogistic.orderstatus.infrastructure.persistence.jparepository.OrderStatusAuditSpringRepository;
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
