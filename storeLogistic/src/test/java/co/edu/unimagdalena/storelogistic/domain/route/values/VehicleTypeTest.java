package co.edu.unimagdalena.storelogistic.domain.route.values;

import co.edu.unimagdalena.storelogistic.domain.route.exceptions.CapacityExceededException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

class VehicleTypeTest {

    @ParameterizedTest(name = "{0} kg → {1}")
    @CsvSource({
        "500,   URBAN_VAN",
        "1500,  URBAN_VAN",
        "1501,  SINGLE_TRUCK",
        "5000,  SINGLE_TRUCK",
        "5001,  REGIONAL_SEMI",
        "25000, REGIONAL_SEMI"
    })
    void forWeight_returnsCorrectType(double weightKg, VehicleType expected) {
        LogisticWeight weight = LogisticWeight.of(weightKg);
        assertThat(VehicleType.forWeight(weight)).isEqualTo(expected);
    }

    @Test
    @DisplayName("Weight > 25 000 kg throws CapacityExceededException")
    void forWeight_exceedsMaxCapacity_throwsException() {
        LogisticWeight heavyWeight = LogisticWeight.of(25_001);
        assertThatThrownBy(() -> VehicleType.forWeight(heavyWeight))
                .isInstanceOf(CapacityExceededException.class)
                .hasMessageContaining("25001");
    }

    @Test
    @DisplayName("Each type maps to a non-null category name")
    void categoryName_isNonNull() {
        for (VehicleType type : VehicleType.values()) {
            assertThat(type.categoryName()).isNotBlank();
        }
    }

    @Test
    @DisplayName("URBAN_VAN maps to CAMIONETA_URBANA category")
    void urbanVan_mapsToCamioneta() {
        assertThat(VehicleType.URBAN_VAN.categoryName()).isEqualTo("CAMIONETA_URBANA");
    }

    @Test
    @DisplayName("SINGLE_TRUCK maps to CAMION_SENCILLO category")
    void singleTruck_mapsToCamionSencillo() {
        assertThat(VehicleType.SINGLE_TRUCK.categoryName()).isEqualTo("CAMION_SENCILLO");
    }

    @Test
    @DisplayName("REGIONAL_SEMI maps to TRACTOCAMION_REGIONAL category")
    void regionalSemi_mapsToTractocamionRegional() {
        assertThat(VehicleType.REGIONAL_SEMI.categoryName()).isEqualTo("TRACTOCAMION_REGIONAL");
    }
}
