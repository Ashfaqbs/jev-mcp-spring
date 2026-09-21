package dev.ashfaqbs.jevmcp.tools;

/** A sanitized, safe-to-surface failure from a Jev tool call. Never carries a response body or stack trace. */
class JevToolException extends RuntimeException {

    JevToolException(String message) {
        super(message);
    }
}
