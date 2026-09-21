package dev.ashfaqbs.jevmcp.tools;

import java.util.Map;

/** Result of a successful {@link JevTools#score} call. Failures are thrown, not returned. */
public record ScoreResult(Double score, Double confidence, Map<String, Double> probabilities,
                           Map<String, Object> legend) {
}
