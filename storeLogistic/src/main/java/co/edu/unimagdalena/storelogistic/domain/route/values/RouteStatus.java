package co.edu.unimagdalena.storelogistic.domain.route.values;

public enum RouteStatus {
    AVAILABLE,
    CLOSED,
    COMPLETED,
    PENDING_VEHICLE;

    public boolean isValidTransition(RouteStatus target) {
        if (target == null || target == this) return false;
        return switch (this) {
            case AVAILABLE       -> target == CLOSED;
            case PENDING_VEHICLE -> target == AVAILABLE;
            case CLOSED          -> target == COMPLETED;
            case COMPLETED       -> false;
        };
    }

    public String invalidTransitionMessage(RouteStatus target) {
        return "Route state transition from " + this + " to " + target + " is not allowed";
    }
}