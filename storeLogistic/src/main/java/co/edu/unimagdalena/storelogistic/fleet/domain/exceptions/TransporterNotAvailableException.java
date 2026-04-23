package co.edu.unimagdalena.storelogistic.fleet.domain.exceptions;

public class TransporterNotAvailableException extends LogisticsException {
    public TransporterNotAvailableException() {
        super("No hay transportistas disponibles para asignar al vehículo");
    }
}