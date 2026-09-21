package dev.ashfaqbs.jevmcp.tools;

/** Result of a successful {@link JevTools#check} call. Failures are thrown, not returned. */
public record CheckResult(Double probability, String band) {
}
