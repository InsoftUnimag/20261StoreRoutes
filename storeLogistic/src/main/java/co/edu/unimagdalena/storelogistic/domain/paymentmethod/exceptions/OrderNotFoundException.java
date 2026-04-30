package co.edu.unimagdalena.storelogistic.domain.paymentmethod.exceptions;

public class OrderNotFoundException extends LogisticsException {
    public OrderNotFoundException(String message) {
        super(message);
    }
}
