package co.edu.unimagdalena.storelogistic.orderstatus.domain.exceptions;

public class OrderNotFoundException extends LogisticsException {

    public OrderNotFoundException(Long orderId) {
        super("No existe un pedido con el id proporcionado: " + orderId);
    }
}
