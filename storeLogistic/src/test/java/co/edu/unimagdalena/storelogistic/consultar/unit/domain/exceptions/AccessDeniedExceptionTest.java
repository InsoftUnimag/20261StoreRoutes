package co.edu.unimagdalena.storelogistic.consultar.unit.domain.exceptions;

import co.edu.unimagdalena.storelogistic.consultar.domain.exceptions.AccessDeniedException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AccessDeniedExceptionTest {

    @Test
    @DisplayName("AccessDeniedException → message does not reveal route existence")
    void message_isGenericAndDoesNotRevealRouteExistence() {
        AccessDeniedException ex = new AccessDeniedException();
        assertThat(ex.getMessage()).isEqualTo("Acceso denegado");
        assertThat(ex.getMessage()).doesNotContainIgnoringCase("ruta");
        assertThat(ex.getMessage()).doesNotContainIgnoringCase("existe");
        assertThat(ex.getMessage()).doesNotContainIgnoringCase("not found");
    }
}
