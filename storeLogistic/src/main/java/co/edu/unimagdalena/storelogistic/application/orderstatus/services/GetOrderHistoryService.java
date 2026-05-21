package co.edu.unimagdalena.storelogistic.application.orderstatus.services;

import co.edu.unimagdalena.storelogistic.domain.orderstatus.models.Order;
import co.edu.unimagdalena.storelogistic.domain.orderstatus.ports.in.GetOrderHistoryUseCase;
import co.edu.unimagdalena.storelogistic.domain.orderstatus.ports.out.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetOrderHistoryService implements GetOrderHistoryUseCase {

    private final OrderRepository orderRepository;

    @Override
    public List<Order> getByCarrier(Long carrierId) {
        return orderRepository.findCompletedByCarrierId(carrierId);
    }
}
