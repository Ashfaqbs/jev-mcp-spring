# Jev MCP Server (Spring)

[![CI](https://github.com/Ashfaqbs/jev-mcp-spring/actions/workflows/ci.yml/badge.svg)](https://github.com/Ashfaqbs/jev-mcp-spring/actions/workflows/ci.yml)
[![License: Apache-2.0](https://img.shields.io/badge/license-Apache--2.0-blue.svg)](LICENSE)

A Java/Spring Boot MCP server exposing [TypeSafe Jev](https://typesafe.ai) as typed-judgment tools
over HTTP (Streamable/SSE), for MCP clients that can reach a running service rather than spawn a
local stdio subprocess.

This is an independent community project, not affiliated with TypeSafe AI. As far as I've found,
it's the first Java-based MCP server for Jev. Two existing `jev-mcp` projects cover Python/Node
instead: [`blakestone-x/jev-mcp`](https://github.com/blakestone-x/jev-mcp) (Python/stdio) and
[`jkudish/jev-mcp`](https://github.com/jkudish/jev-mcp) (TypeScript/npm, a broader ten-tool
agent-workflow surface including diff review and completion-claim gating). This project covers a
subset over HTTP, for the Java/Spring side of the ecosystem.

You must hold your own valid TypeSafe account and API key to run this server, and you're
responsible for using it in accordance with [TypeSafe's own terms](https://typesafe.ai/legal/terms).
This project only forwards calls made with credentials you supply — it does not provide, proxy, or
resell access to TypeSafe's Services on anyone else's behalf.

## Why this exists

- [`typesafe-sdk-java`](https://github.com/Premo-Cloud/typesafe-sdk-java) and
  [`jev-spring-boot-starter`](https://github.com/danvega/jev-spring-boot-starter) give Java/Spring
  applications a Jev *client*. Neither exposes Jev as MCP *tools* an agent can call directly.
- `jev-mcp` fills that role for Python/stdio clients. Nothing filled it for Java or for clients that
  want an HTTP endpoint instead of a spawned subprocess.

This project wires [`jev-spring-boot-starter`](https://github.com/danvega/jev-spring-boot-starter)
into [Spring AI's MCP server support](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-overview.html)
to close that gap.

## Tools

| Tool | Purpose | Result |
| --- | --- | --- |
| `jev_classify` | Classify state into one of the given labels | label, confidence, probabilities |
| `jev_score` | Rate state against an ordered rubric | weighted zero-based score, confidence, legend |
| `jev_check` | A reusable yes/no question | probability of yes, and a `flag` / `uncertain` / `clear` band |
| `jev_gate` | Gate a patch before merge: verify completion claims against a diff and evidence, score merge risk | per-claim verdicts, risk level, `pass` / `review` decision |
| `jev_health` | Connectivity check | resolved model, round-trip latency |

On success each tool returns its typed result directly. On failure the tool call fails at the MCP
protocol level (`isError: true`) with a short, safe message — no response body or stack trace is
ever echoed back.

**Not implemented yet** (tracked as follow-ups, not silently missing): `jev_ask` (Jev's fully
generic, mixed-question-type call — the fixed-shape tools above cover the common cases first),
`jev_match` (best-candidate search with abstention), and `jev_screen` (prompt-injection screening).
See `jev-mcp`'s implementations of these for reference if you want to help add them here.

## Get started

Requires Java 17+ and a `TYPESAFE_API_KEY`.

```sh
export TYPESAFE_API_KEY=your-key
./mvnw spring-boot:run
```

The server starts on port 8080 with the MCP endpoint at `/mcp`. Point an HTTP-capable MCP client
at `http://localhost:8080/mcp`.

## Configuration

This project adds no configuration of its own beyond what
[`jev-spring-boot-starter`](https://github.com/danvega/jev-spring-boot-starter#jev-properties)
already provides (`jev.api-key`, `jev.base-url`, `jev.model`, `jev.enabled`), plus Spring AI's own
[MCP server properties](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-server-boot-starter-docs.html)
(`spring.ai.mcp.server.*`) for the transport itself.

## Dependency note

[`jev-spring-boot-starter`](https://github.com/danvega/jev-spring-boot-starter) is not yet published
to Maven Central (its own README says so explicitly). This project depends on it through
[JitPack](https://jitpack.io), pinned to its current single commit:

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
<dependency>
    <groupId>com.github.danvega</groupId>
    <artifactId>jev-spring-boot-starter</artifactId>
    <version>1f5d3d7bb7</version>
</dependency>
```

If that repository publishes a release to Maven Central, switch to it directly instead.

## Development

```sh
./mvnw clean verify
```

Tests mock the Jev HTTP API with `MockRestServiceServer` (no API key or network access required)
and exercise `JevTools` directly.

## License

Apache-2.0. See [LICENSE](LICENSE).
