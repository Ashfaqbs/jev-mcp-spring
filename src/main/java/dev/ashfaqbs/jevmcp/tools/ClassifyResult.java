package dev.ashfaqbs.jevmcp.tools;

import java.util.Map;

/** Result of a successful {@link JevTools#classify} call. Failures are thrown, not returned. */
public record ClassifyResult(String label, Double confidence, Map<String, Double> probabilities) {
}
