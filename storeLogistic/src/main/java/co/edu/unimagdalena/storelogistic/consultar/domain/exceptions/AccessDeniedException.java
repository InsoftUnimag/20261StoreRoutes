package co.edu.unimagdalena.storelogistic.consultar.domain.exceptions;

public class AccessDeniedException extends RuntimeException {

    public AccessDeniedException() {
        super("Acceso denegado");
    }
}
