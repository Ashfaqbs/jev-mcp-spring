package dev.ashfaqbs.jevmcp.tools;

/** Result of {@link JevTools#health}. Exactly one of the success fields or {@code error} is set. */
public record HealthResult(boolean ok, String model, Long latencyMillis, String error) {

    static HealthResult success(String model, Long latencyMillis) {
        return new HealthResult(true, model, latencyMillis, null);
    }

    static HealthResult failure(String error) {
        return new HealthResult(false, null, null, error);
    }
}
