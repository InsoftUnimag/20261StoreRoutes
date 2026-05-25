package co.edu.unimagdalena.storelogistic.infrastructure.fleet.client;

import co.edu.unimagdalena.storelogistic.domain.fleet.exceptions.InvalidTransporterException;
import co.edu.unimagdalena.storelogistic.domain.fleet.exceptions.TransporterNotAvailableException;
import co.edu.unimagdalena.storelogistic.domain.fleet.ports.out.TransporterServicePort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Mock for tests — replaced by {@link TransporterServiceClient} in production.
 */
@Slf4j
public class MockTransporterServiceClient implements TransporterServicePort {

    private static final Long AVAILABLE_TRANSPORTER_ID = 1L;
    private static final Set<Long> VALID_IDS = Set.of(1L, 2L, 3L);

    @Override
    public Long getAvailable() {
        log.info("[MOCK] Returning available transporter: {}", AVAILABLE_TRANSPORTER_ID);
        return AVAILABLE_TRANSPORTER_ID;
    }

    @Override
    public void validateExistence(Long transporterId) {
        log.info("[MOCK] Validating transporter: {}", transporterId);
        if (transporterId == null || !VALID_IDS.contains(transporterId)) {
            throw new InvalidTransporterException(transporterId);
        }
    }
}