package co.edu.unimagdalena.storelogistic.domain.fleet.exceptions;

public class LogisticsException extends RuntimeException {
    public LogisticsException(String message) {
        super(message);
    }

    public LogisticsException(String message, Throwable cause) {
        super(message, cause);
    }
}