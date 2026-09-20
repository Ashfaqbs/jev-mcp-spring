package dev.ashfaqbs.jevmcp.tools;

import java.util.Map;

/** Result of {@link JevTools#classify}. Exactly one of the success fields or {@code error} is set. */
public record ClassifyResult(boolean ok, String label, Double confidence,
                              Map<String, Double> probabilities, String error) {

    static ClassifyResult success(String label, Double confidence, Map<String, Double> probabilities) {
        return new ClassifyResult(true, label, confidence, probabilities, null);
    }

    static ClassifyResult failure(String error) {
        return new ClassifyResult(false, null, null, null, error);
    }
}
