package co.edu.unimagdalena.storelogistic.fleet.domain.exceptions;

public class InvalidStateTransitionException extends LogisticsException {
    public InvalidStateTransitionException(String message) {
        super(message);
    }
}