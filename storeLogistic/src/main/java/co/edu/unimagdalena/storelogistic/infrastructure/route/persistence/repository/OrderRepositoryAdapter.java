package co.edu.unimagdalena.storelogistic.infrastructure.route.persistence.repository;

import co.edu.unimagdalena.storelogistic.domain.route.models.Order;
import co.edu.unimagdalena.storelogistic.domain.route.ports.out.OrderRepository;
import co.edu.unimagdalena.storelogistic.infrastructure.route.mapper.RouteAssignmentMapper;
import co.edu.unimagdalena.storelogistic.infrastructure.route.persistence.jpa.OrderJpaEntity;
import co.edu.unimagdalena.storelogistic.infrastructure.route.persistence.jparepository.OrderSpringRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class OrderRepositoryAdapter implements OrderRepository {

    private final OrderSpringRepository springRepository;
    private final RouteAssignmentMapper mapper;

    @Override
    public Optional<Order> findById(Long orderId) {
        return springRepository.findById(orderId).map(mapper::toOrder);
    }

    @Override
    public Order save(Order order) {
        OrderJpaEntity entity = mapper.toOrderEntity(order);
        OrderJpaEntity saved = springRepository.save(entity);
        return mapper.toOrder(saved);
    }
}
