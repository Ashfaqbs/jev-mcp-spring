package dev.ashfaqbs.jevmcp.tools;

import java.util.Map;

/** Result of {@link JevTools#score}. Exactly one of the success fields or {@code error} is set. */
public record ScoreResult(boolean ok, Double score, Double confidence,
                           Map<String, Double> probabilities, Map<String, Object> legend, String error) {

    static ScoreResult success(Double score, Double confidence,
                                Map<String, Double> probabilities, Map<String, Object> legend) {
        return new ScoreResult(true, score, confidence, probabilities, legend, null);
    }

    static ScoreResult failure(String error) {
        return new ScoreResult(false, null, null, null, null, error);
    }
}
