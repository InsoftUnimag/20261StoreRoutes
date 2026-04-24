package co.edu.unimagdalena.storelogistic.orderstatus.unit.domain.values;

import co.edu.unimagdalena.storelogistic.orderstatus.domain.values.FinalStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FinalStatusTest {

    @ParameterizedTest
    @CsvSource({
            "ENTREGADO_COMPLETO, 100",
            "RECHAZO_PARCIAL,    80",
            "NO_ENTREGADO,       0",
            "DEVOLUCION_ERROR_EMPRESA, 0",
            "FALTANTE_INVENTARIO, -100"
    })
    @DisplayName("effectivenessRate → correct value for each status")
    void effectivenessRate_returnsCorrectValuePerStatus(String statusName, int expectedRate) {
        FinalStatus status = FinalStatus.valueOf(statusName.trim());
        assertThat(status.effectivenessRate().value()).isEqualTo(expectedRate);
    }

    @ParameterizedTest
    @CsvSource({
            "ENTREGADO_COMPLETO, 'Entregado Completo'",
            "RECHAZO_PARCIAL, 'Rechazo Parcial'",
            "NO_ENTREGADO, 'No Entregado'",
            "DEVOLUCION_ERROR_EMPRESA, 'Devolución (Error Empresa)'",
            "FALTANTE_INVENTARIO, 'Faltante de Inventario'"
    })
    @DisplayName("displayName → returns human-readable string")
    void displayName_returnsHumanReadableString(String statusName, String expectedDisplay) {
        FinalStatus status = FinalStatus.valueOf(statusName.trim());
        assertThat(status.displayName()).isEqualTo(expectedDisplay.replace("'", ""));
    }

    @Test
    @DisplayName("fromDisplayName → parses 'Entregado Completo' correctly")
    void fromDisplayName_parsesEntregadoCompleto() {
        assertThat(FinalStatus.fromDisplayName("Entregado Completo"))
                .isEqualTo(FinalStatus.ENTREGADO_COMPLETO);
    }

    @Test
    @DisplayName("fromDisplayName → parses 'Devolución (Error Empresa)' correctly")
    void fromDisplayName_parsesDevolucion() {
        assertThat(FinalStatus.fromDisplayName("Devolución (Error Empresa)"))
                .isEqualTo(FinalStatus.DEVOLUCION_ERROR_EMPRESA);
    }

    @Test
    @DisplayName("fromDisplayName → unknown value throws IllegalArgumentException")
    void fromDisplayName_unknownValue_throws() {
        assertThatThrownBy(() -> FinalStatus.fromDisplayName("EstadoInventado"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
