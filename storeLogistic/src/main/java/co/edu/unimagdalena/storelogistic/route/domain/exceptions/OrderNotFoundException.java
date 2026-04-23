package co.edu.unimagdalena.storelogistic.route.domain.exceptions;

public class OrderNotFoundException extends RouteException {
    public OrderNotFoundException(Long orderId) {
        super("Order not found with id: " + orderId);
    }
}