package dev.ashfaqbs.jevmcp.tools;

/** Result of {@link JevTools#check}. Exactly one of the success fields or {@code error} is set. */
public record CheckResult(boolean ok, Double probability, String band, String error) {

    static CheckResult success(Double probability, String band) {
        return new CheckResult(true, probability, band, null);
    }

    static CheckResult failure(String error) {
        return new CheckResult(false, null, null, error);
    }
}
