package co.edu.unimagdalena.storelogistic.fleet.domain.exceptions;

public class InvalidTransporterException extends LogisticsException {
    public InvalidTransporterException(Long transporterId) {
        super("El idTransportista proporcionado no existe en el módulo de Transportista: " + transporterId);
    }
}