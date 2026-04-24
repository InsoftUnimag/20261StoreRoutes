package co.edu.unimagdalena.storelogistic.route.domain.values;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

class LogisticWeightTest {

    @Test
    void of_validWeight_createsInstance() {
        LogisticWeight w = LogisticWeight.of(100.0);
        assertThat(w.valueKg()).isEqualByComparingTo(BigDecimal.valueOf(100));
    }

    @Test
    void of_zeroWeight_throwsIllegalArgument() {
        assertThatThrownBy(() -> LogisticWeight.of(BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void of_negativeWeight_throwsIllegalArgument() {
        assertThatThrownBy(() -> LogisticWeight.of(-1.0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void of_nullWeight_throwsNullPointer() {
        assertThatThrownBy(() -> new LogisticWeight(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void add_twoWeights_returnsSum() {
        LogisticWeight a = LogisticWeight.of(300.0);
        LogisticWeight b = LogisticWeight.of(200.0);
        assertThat(a.add(b).valueKg()).isEqualByComparingTo(BigDecimal.valueOf(500));
    }
}
