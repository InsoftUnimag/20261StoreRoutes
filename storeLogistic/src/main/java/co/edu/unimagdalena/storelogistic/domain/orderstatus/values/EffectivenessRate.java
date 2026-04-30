package co.edu.unimagdalena.storelogistic.domain.orderstatus.values;

import java.util.Objects;

public final class EffectivenessRate {

    private final int value;

    private EffectivenessRate(int value) {
        if (value < -100 || value > 100) {
            throw new IllegalArgumentException("Effectiveness rate must be between -100 and 100, got: " + value);
        }
        this.value = value;
    }

    static EffectivenessRate of(int value) {
        return new EffectivenessRate(value);
    }

    public int value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EffectivenessRate that)) return false;
        return value == that.value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
