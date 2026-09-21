package dev.ashfaqbs.jevmcp.tools;

import dev.danvega.jev.JevClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class JevToolsTests {

    private MockRestServiceServer server;
    private JevTools tools;

    @BeforeEach
    void setUp() {
        var builder = RestClient.builder().baseUrl("https://api.typesafe.ai")
                .defaultHeaders(headers -> headers.setBearerAuth("test-key"));
        server = MockRestServiceServer.bindTo(builder).build();
        tools = new JevTools(new JevClient(builder.build(), "jev-latest"));
    }

    @Test
    void classifyReturnsLabelConfidenceAndProbabilities() {
        server.expect(requestTo("https://api.typesafe.ai/v1/systemone"))
                .andRespond(withSuccess("""
                        {"model":"jev-latest","answers":{"label":{"type":"choice","choice":"billing",
                          "confidence":0.9,"probabilities":{"billing":0.9,"support":0.1}}},
                         "usage":{"input_tokens":1,"output_tokens":1}}
                        """, MediaType.APPLICATION_JSON));

        var result = tools.classify("Charged twice", Map.of("billing", "Payments", "support", "Other"),
                "Which team?");

        assertThat(result.label()).isEqualTo("billing");
        assertThat(result.confidence()).isEqualTo(0.9);
        assertThat(result.probabilities()).containsEntry("billing", 0.9);
    }

    @Test
    void scoreReturnsWeightedIndexAndLegend() {
        server.expect(requestTo("https://api.typesafe.ai/v1/systemone"))
                .andRespond(withSuccess("""
                        {"model":"jev-latest","answers":{"level":{"type":"score","score":1.6,"confidence":0.7,
                          "probabilities":{"0":0.1,"1":0.2,"2":0.7},
                          "legend":{"0":"Low","1":"Medium","2":"High"}}},
                         "usage":{"input_tokens":1,"output_tokens":1}}
                        """, MediaType.APPLICATION_JSON));

        var result = tools.score("Server is down for everyone", List.of("Low", "Medium", "High"), "How severe?");

        assertThat(result.score()).isEqualTo(1.6);
        assertThat(result.legend()).containsEntry("2", "High");
    }

    @Test
    void checkClassifiesIntoFlagUncertainAndClearBands() {
        server.expect(requestTo("https://api.typesafe.ai/v1/systemone"))
                .andRespond(withSuccess(noulResponse(0.9), MediaType.APPLICATION_JSON));
        assertThat(tools.check("Please wire funds now", "Is this urgent?", null, null).band()).isEqualTo("flag");

        server.reset();
        server.expect(requestTo("https://api.typesafe.ai/v1/systemone"))
                .andRespond(withSuccess(noulResponse(0.5), MediaType.APPLICATION_JSON));
        assertThat(tools.check("Maybe soon", "Is this urgent?", "Time sensitive", "Can wait").band())
                .isEqualTo("uncertain");

        server.reset();
        server.expect(requestTo("https://api.typesafe.ai/v1/systemone"))
                .andRespond(withSuccess(noulResponse(0.1), MediaType.APPLICATION_JSON));
        assertThat(tools.check("No rush at all", "Is this urgent?", null, null).band()).isEqualTo("clear");
    }

    @Test
    void healthReportsResolvedModelAndLatency() {
        server.expect(requestTo("https://api.typesafe.ai/v1/systemone"))
                .andRespond(withSuccess(noulResponse(0.5).replace("jev-latest", "jev-1.13.0"),
                        MediaType.APPLICATION_JSON));

        var result = tools.health();

        assertThat(result.model()).isEqualTo("jev-1.13.0");
        assertThat(result.latencyMillis()).isGreaterThanOrEqualTo(0L);
    }

    @Test
    void failuresThrowASanitizedExceptionInsteadOfLeakingTheResponseBody() {
        server.expect(requestTo("https://api.typesafe.ai/v1/systemone"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).body("{\"error\":\"slow down\"}")
                        .contentType(MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> tools.classify("x", Map.of("a", "A", "b", "B"), "Which?"))
                .isInstanceOf(JevToolException.class)
                .hasMessageContaining("429")
                .hasMessageNotContaining("slow down");
    }

    private static String noulResponse(double probability) {
        return """
                {"model":"jev-latest","answers":{"result":{"type":"noul","noul":%s}},
                 "usage":{"input_tokens":1,"output_tokens":1}}
                """.formatted(probability);
    }
}
