package dev.ashfaqbs.jevmcp.tools;

import java.util.List;

/** Result of a successful {@link JevTools#gate} call. Failures are thrown, not returned. */
public record GateResult(String decision, List<ClaimVerdict> claims, String riskLevel, Double riskConfidence) {

    /** Whether a single completion claim held up against the diff and evidence supplied. */
    public record ClaimVerdict(String claim, boolean supported, Double confidence) {
    }
}
