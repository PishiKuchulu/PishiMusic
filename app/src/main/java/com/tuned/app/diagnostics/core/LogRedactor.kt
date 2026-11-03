package com.tuned.app.diagnostics.core

/**
 * Masks credential-shaped values in a rendered log line before it reaches
 * disk.
 *
 * The log store is the single choke point every record passes through, so
 * redacting here protects against every current and future call site that
 * interpolates a header, token or cookie into a message by accident. The
 * rules are deliberately conservative in shape (named keys, auth schemes,
 * cookie pairs, and long opaque bearer-like runs) so ordinary prose, ids
 * and timestamps survive untouched.
 */
object LogRedactor {

    private const val MASK = "***"

    private const val KEY_NAMES =
        "access[_-]?token|refresh[_-]?token|client[_-]?token|id[_-]?token|auth[_-]?token|token|secret|client[_-]?secret|password|passwd|api[_-]?key|session[_-]?key|sp_dc|sapisid|cookie"

    /** `Cookie: a=b; c=d` — only the name=value pairs are masked. */
    private val COOKIE_HEADER = Regex(
        """(?i)(cookie\s*[:=]\s*)[A-Za-z0-9_.-]+=[^;\s]*(?:;\s*[A-Za-z0-9_.-]+=[^;\s]*)*""",
    )

    /** `Authorization: Bearer xxx` or `authorization=xxx`. */
    private val AUTHORIZATION = Regex("""(?i)(authorization\s*[:=]\s*)(?:bearer|basic|digest)?\s*\S+""")

    /** A bare `Bearer xxx` anywhere in the line. */
    private val BEARER = Regex("""(?i)\bbearer\s+[A-Za-z0-9._~+/=-]+""")

    /**
     * Named credential keys in `key=value`, `key: value` and JSON
     * `"key": "value"` shapes. Group 1 is everything up to the value, so
     * the replacement keeps the key readable and masks only the value
     * (quoted values are masked whole).
     */
    private val NAMED_VALUE = Regex(
        """(?i)((?:"(?:$KEY_NAMES)"|(?:$KEY_NAMES))\s*[:=]\s*)("[^"]*"|[^\s,;&\]]+)""",
    )

    /**
     * Long opaque runs are treated as bearer material. The length floor
     * keeps ordinary identifiers (video ids, file names, short hashes)
     * readable while catching real tokens, which are far longer.
     */
    private val LONG_OPAQUE = Regex("""[A-Za-z0-9._~+/=-]{64,}""")

    fun redact(line: String): String {
        if (line.isEmpty()) return line
        var out = COOKIE_HEADER.replace(line) { match -> match.groupValues[1] + MASK }
        out = AUTHORIZATION.replace(out) { match -> match.groupValues[1] + MASK }
        out = BEARER.replace(out) { MASK }
        out = NAMED_VALUE.replace(out) { match -> match.groupValues[1] + MASK }
        out = LONG_OPAQUE.replace(out) { MASK }
        return out
    }
}
