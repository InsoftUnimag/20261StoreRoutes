package co.edu.unimagdalena.storelogistic.route.domain.values;

import co.edu.unimagdalena.storelogistic.route.domain.exceptions.CapacityExceededException;

import java.math.BigDecimal;

public enum VehicleType {
    URBAN_VAN   (BigDecimal.valueOf(1_500),  "CAMIONETA"),
    SINGLE_TRUCK(BigDecimal.valueOf(5_000),  "CAMION_SENCILLO"),
    REGIONAL_SEMI(BigDecimal.valueOf(25_000), "TRACTOCAMION_REGIONAL");

    private final BigDecimal maxCapacityKg;
    private final String categoryName;

    VehicleType(BigDecimal maxCapacityKg, String categoryName) {
        this.maxCapacityKg = maxCapacityKg;
        this.categoryName  = categoryName;
    }

    public BigDecimal maxCapacityKg() { return maxCapacityKg; }
    public String categoryName()      { return categoryName; }

    public RouteCapacity capacity() {
        return RouteCapacity.of(maxCapacityKg);
    }

    public static VehicleType forWeight(LogisticWeight weight) {
        for (VehicleType type : values()) {
            if (weight.valueKg().compareTo(type.maxCapacityKg) <= 0) return type;
        }
        throw new CapacityExceededException(
            "Weight " + weight.valueKg() + " kg exceeds maximum vehicle capacity of " +
            REGIONAL_SEMI.maxCapacityKg + " kg"
        );
    }
}