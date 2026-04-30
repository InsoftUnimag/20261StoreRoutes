package co.edu.unimagdalena.storelogistic.domain.route.values;

public enum StopStatus {
    PENDING,
    DELIVERED,
    REJECTED;

    public boolean isValidTransition(StopStatus target) {
        if (target == null || target == this) return false;
        return switch (this) {
            case PENDING   -> target == DELIVERED || target == REJECTED;
            case DELIVERED -> false;
            case REJECTED  -> false;
        };
    }

    public String invalidTransitionMessage(StopStatus target) {
        return "Stop state transition from " + this + " to " + target + " is not allowed";
    }
}