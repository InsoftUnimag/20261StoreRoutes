package co.edu.unimagdalena.storelogistic.infrastructure.orderstatus.persistence.repository;

import co.edu.unimagdalena.storelogistic.domain.orderstatus.models.Order;
import co.edu.unimagdalena.storelogistic.domain.orderstatus.ports.out.OrderRepository;
import co.edu.unimagdalena.storelogistic.infrastructure.orderstatus.mapper.OrderStatusMapper;
import co.edu.unimagdalena.storelogistic.infrastructure.orderstatus.persistence.jpa.OrderStatusJpaEntity;
import co.edu.unimagdalena.storelogistic.infrastructure.orderstatus.persistence.jparepository.OrderStatusSpringRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class OrderStatusRepositoryAdapter implements OrderRepository {

    private final OrderStatusSpringRepository springRepository;
    private final OrderStatusMapper mapper;

    @Override
    public Optional<Order> findById(Long orderId) {
        return springRepository.findById(orderId).map(mapper::toDomain);
    }

    @Override
    public Order save(Order order) {
        // Fetch existing entity to preserve logistic_weight and delivery_address (NOT NULL columns
        // owned by the route module — this feature only updates the status-related fields).
        OrderStatusJpaEntity entity = springRepository.findById(order.orderId())
                .orElse(mapper.toEntity(order));
        entity.setCarrierId(order.carrierId());
        entity.setClientId(order.clientId());
        entity.setEstadoFinal(order.finalStatus() != null ? order.finalStatus().displayName() : null);
        entity.setTasaEfectividad(order.effectivenessRate() != null ? order.effectivenessRate().value() : null);
        entity.setUpdatedAt(order.updatedAt());
        OrderStatusJpaEntity saved = springRepository.save(entity);
        return mapper.toDomain(saved);
    }
}
