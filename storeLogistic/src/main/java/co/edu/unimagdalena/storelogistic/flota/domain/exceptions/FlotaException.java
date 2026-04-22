package co.edu.unimagdalena.storelogistic.flota.domain.exceptions;

public class FlotaException extends RuntimeException {
    public FlotaException(String message) {
        super(message);
    }

    public FlotaException(String message, Throwable cause) {
        super(message, cause);
    }
}

