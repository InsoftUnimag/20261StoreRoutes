package co.edu.unimagdalena.storelogistic.domain.orderstatus.exceptions;

public class OrderNotFoundException extends LogisticsException {

    public OrderNotFoundException(Long orderId) {
        super("No existe un pedido con el id proporcionado: " + orderId);
    }
}
