package co.edu.unimagdalena.storelogistic.route.infrastructure.persistence.repository;

import co.edu.unimagdalena.storelogistic.route.domain.models.Order;
import co.edu.unimagdalena.storelogistic.route.domain.ports.out.OrderRepository;
import co.edu.unimagdalena.storelogistic.route.infrastructure.mapper.RouteAssignmentMapper;
import co.edu.unimagdalena.storelogistic.route.infrastructure.persistence.jpa.OrderJpaEntity;
import co.edu.unimagdalena.storelogistic.route.infrastructure.persistence.jparepository.OrderSpringRepository;
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
