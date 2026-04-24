package co.edu.unimagdalena.storelogistic.route.domain.values;

import java.math.BigDecimal;
import java.util.Objects;

public record RouteCapacity(BigDecimal valueKg) {

    public RouteCapacity {
        Objects.requireNonNull(valueKg, "Route capacity must not be null");
        if (valueKg.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Route capacity must be greater than zero, got: " + valueKg);
    }

    public static RouteCapacity of(BigDecimal kg) {
        return new RouteCapacity(kg);
    }

    public static RouteCapacity of(double kg) {
        return new RouteCapacity(BigDecimal.valueOf(kg));
    }
}