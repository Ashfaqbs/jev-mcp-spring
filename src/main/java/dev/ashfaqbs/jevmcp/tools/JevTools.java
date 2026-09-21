package dev.ashfaqbs.jevmcp.tools;

import dev.danvega.jev.JevClient;
import dev.danvega.jev.Question;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Typed-judgment MCP tools backed by {@link JevClient}. Not for text generation. */
@Component
public class JevTools {

    private static final double FLAG_THRESHOLD = 0.70;
    private static final double UNCERTAIN_MIN = 0.30;
    private static final List<String> RISK_LEVELS = List.of("Low", "Medium", "High");

    private final JevClient jev;

    public JevTools(JevClient jev) {
        this.jev = jev;
    }

    @McpTool(name = "jev_classify",
            description = "Classify state into exactly one of the given labels, with a probability per label. "
                    + "Not for text generation.")
    public ClassifyResult classify(
            @McpToolParam(description = "The text or state to classify", required = true) String state,
            @McpToolParam(description = "Map of label name to description", required = true)
            Map<String, String> labels,
            @McpToolParam(description = "The classification question", required = true) String question) {
        try {
            var response = jev.evaluate(state, Map.of("label", Question.choice(question, labels)));
            var answer = response.choice("label");
            return new ClassifyResult(answer.choice(), answer.confidence(), answer.probabilities());
        } catch (Exception exception) {
            throw new JevToolException(errorMessage(exception));
        }
    }

    @McpTool(name = "jev_score",
            description = "Rate state against an ordered rubric, lowest to highest. The score is a "
                    + "probability-weighted zero-based index and can fall between levels. Not for text generation.")
    public ScoreResult score(
            @McpToolParam(description = "The text or state to evaluate", required = true) String state,
            @McpToolParam(description = "Ordered rubric levels, lowest to highest", required = true)
            List<String> levels,
            @McpToolParam(description = "The scoring question", required = true) String question) {
        try {
            var response = jev.evaluate(state, Map.of("level", Question.score(question, levels)));
            var answer = response.score("level");
            return new ScoreResult(answer.score(), answer.confidence(), answer.probabilities(), answer.legend());
        } catch (Exception exception) {
            throw new JevToolException(errorMessage(exception));
        }
    }

    @McpTool(name = "jev_check",
            description = "Ask a reusable yes/no question over state. Returns the probability of yes and a band: "
                    + "'flag' at or above 0.70, 'clear' below 0.30, 'uncertain' between. Not for text generation.")
    public CheckResult check(
            @McpToolParam(description = "The text or state to evaluate", required = true) String state,
            @McpToolParam(description = "The yes/no question", required = true) String question,
            @McpToolParam(description = "What a positive answer means", required = false) String whenTrue,
            @McpToolParam(description = "What a negative answer means", required = false) String whenFalse) {
        try {
            var noulQuestion = (whenTrue == null && whenFalse == null)
                    ? Question.noul(question)
                    : Question.noul(question, whenTrue, whenFalse);
            var response = jev.evaluate(state, Map.of("result", noulQuestion));
            double probability = response.noul("result").noul();
            return new CheckResult(probability, band(probability));
        } catch (Exception exception) {
            throw new JevToolException(errorMessage(exception));
        }
    }

    @McpTool(name = "jev_gate",
            description = "Gate a patch before merge: verify each completion claim against the diff and the "
                    + "evidence supplied (e.g. test output), and score the diff's merge risk. Returns a verdict "
                    + "per claim plus an overall pass/review recommendation. Not for text generation.")
    public GateResult gate(
            @McpToolParam(description = "The diff or patch text", required = true) String diff,
            @McpToolParam(description = "Evidence supporting the claims, e.g. test output or CI logs",
                    required = true) String evidence,
            @McpToolParam(description = "Completion claims to verify, e.g. 'all tests pass'", required = true)
            List<String> claims) {
        try {
            var state = "Diff:\n" + diff + "\n\nEvidence:\n" + evidence;
            Map<String, Question> questions = new LinkedHashMap<>();
            for (int i = 0; i < claims.size(); i++) {
                questions.put(claimKey(i), Question.noul(
                        "Is this claim true, based on the diff and evidence: \"" + claims.get(i) + "\"?"));
            }
            questions.put("risk", Question.score("How risky is this diff to merge?", RISK_LEVELS));
            var response = jev.evaluate(state, questions);

            var verdicts = new ArrayList<GateResult.ClaimVerdict>();
            boolean allSupported = true;
            for (int i = 0; i < claims.size(); i++) {
                double probability = response.noul(claimKey(i)).noul();
                boolean supported = probability >= FLAG_THRESHOLD;
                allSupported = allSupported && supported;
                verdicts.add(new GateResult.ClaimVerdict(claims.get(i), supported, probability));
            }

            var riskAnswer = response.score("risk");
            String riskLevel = String.valueOf(riskAnswer.legend().get(String.valueOf(Math.round(riskAnswer.score()))));
            String decision = (allSupported && !"High".equals(riskLevel)) ? "pass" : "review";
            return new GateResult(decision, List.copyOf(verdicts), riskLevel, riskAnswer.confidence());
        } catch (Exception exception) {
            throw new JevToolException(errorMessage(exception));
        }
    }

    @McpTool(name = "jev_health",
            description = "Check connectivity to Jev and report the resolved model and round-trip latency.")
    public HealthResult health() {
        try {
            long started = System.nanoTime();
            var response = jev.evaluate("health check", Map.of("result", Question.noul("Is this a health check?")));
            long latencyMillis = (System.nanoTime() - started) / 1_000_000;
            return new HealthResult(response.model(), latencyMillis);
        } catch (Exception exception) {
            throw new JevToolException(errorMessage(exception));
        }
    }

    private static String claimKey(int index) {
        return "claim" + index;
    }

    private static String band(double probability) {
        if (probability >= FLAG_THRESHOLD) {
            return "flag";
        }
        if (probability >= UNCERTAIN_MIN) {
            return "uncertain";
        }
        return "clear";
    }

    /** A short, safe message: no response bodies or stack traces, which could carry sensitive detail. */
    private static String errorMessage(Exception exception) {
        if (exception instanceof RestClientResponseException responseException) {
            return "TypeSafe API request failed with status " + responseException.getStatusCode().value();
        }
        if (exception instanceof ResourceAccessException) {
            return "TypeSafe API request failed: connection error";
        }
        if (exception instanceof RestClientException) {
            return "TypeSafe API request failed: " + exception.getMessage();
        }
        if (exception instanceof IllegalArgumentException) {
            return "Invalid tool input: " + exception.getMessage();
        }
        return "Jev tool failed";
    }
}
