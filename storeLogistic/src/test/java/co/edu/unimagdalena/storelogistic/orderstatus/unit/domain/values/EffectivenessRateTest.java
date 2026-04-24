package co.edu.unimagdalena.storelogistic.orderstatus.unit.domain.values;

import co.edu.unimagdalena.storelogistic.orderstatus.domain.values.FinalStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EffectivenessRateTest {

    @Test
    @DisplayName("EffectivenessRate from ENTREGADO_COMPLETO equals another with value 100")
    void equalsAndHashCode_sameValue() {
        var rate1 = FinalStatus.ENTREGADO_COMPLETO.effectivenessRate();
        var rate2 = FinalStatus.ENTREGADO_COMPLETO.effectivenessRate();
        assertThat(rate1).isEqualTo(rate2);
        assertThat(rate1.hashCode()).isEqualTo(rate2.hashCode());
    }

    @Test
    @DisplayName("EffectivenessRate.value() returns int value")
    void value_returnsInt() {
        assertThat(FinalStatus.FALTANTE_INVENTARIO.effectivenessRate().value()).isEqualTo(-100);
        assertThat(FinalStatus.RECHAZO_PARCIAL.effectivenessRate().value()).isEqualTo(80);
    }

    @Test
    @DisplayName("EffectivenessRate.toString() returns string representation of value")
    void toString_returnsStringValue() {
        assertThat(FinalStatus.ENTREGADO_COMPLETO.effectivenessRate().toString()).isEqualTo("100");
    }
}
