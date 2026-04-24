package co.edu.unimagdalena.storelogistic.paymentmethod.domain.exceptions;

public class OrderNotFoundException extends LogisticsException {
    public OrderNotFoundException(String message) {
        super(message);
    }
}
