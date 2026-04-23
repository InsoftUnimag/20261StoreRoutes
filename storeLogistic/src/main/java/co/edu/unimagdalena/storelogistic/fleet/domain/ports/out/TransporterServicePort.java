package co.edu.unimagdalena.storelogistic.fleet.domain.ports.out;

public interface TransporterServicePort {
    Long getAvailable();
    void validateExistence(Long transporterId);
}