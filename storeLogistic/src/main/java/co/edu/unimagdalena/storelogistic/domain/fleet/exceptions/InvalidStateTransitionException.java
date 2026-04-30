package co.edu.unimagdalena.storelogistic.domain.fleet.exceptions;

public class InvalidStateTransitionException extends LogisticsException {
    public InvalidStateTransitionException(String message) {
        super(message);
    }
}