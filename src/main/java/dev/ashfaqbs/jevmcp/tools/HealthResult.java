package dev.ashfaqbs.jevmcp.tools;

/** Result of a successful {@link JevTools#health} call. Failures are thrown, not returned. */
public record HealthResult(String model, Long latencyMillis) {
}
