package co.edu.unimagdalena.storelogistic.infrastructure.fleet;

import co.edu.unimagdalena.storelogistic.domain.fleet.exceptions.InvalidTransporterException;
import co.edu.unimagdalena.storelogistic.infrastructure.fleet.client.MockTransporterServiceClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MockTransporterServiceClientTest {

    private MockTransporterServiceClient client;

    @BeforeEach
    void setUp() {
        client = new MockTransporterServiceClient();
    }

    @Test
    void getAvailable_returnsKnownTransporterId() {
        assertThat(client.getAvailable()).isPositive();
    }

    @Test
    void validateExistence_withValidId_doesNotThrow() {
        client.validateExistence(1L);
    }

    @Test
    void validateExistence_withInvalidId_throwsInvalidTransporterException() {
        assertThatThrownBy(() -> client.validateExistence(999L))
                .isInstanceOf(InvalidTransporterException.class);
    }

    @Test
    void validateExistence_withNull_throwsInvalidTransporterException() {
        assertThatThrownBy(() -> client.validateExistence(null))
                .isInstanceOf(InvalidTransporterException.class);
    }

    @Test
    void getAvailable_thenValidate_successFlow() {
        var id = client.getAvailable();
        client.validateExistence(id);
    }
}