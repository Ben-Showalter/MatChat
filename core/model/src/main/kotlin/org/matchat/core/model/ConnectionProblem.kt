package org.matchat.core.model

/**
 * Why a sign-in could not reach the server, found by retrying the same address
 * through Android's own networking after the Matrix library reported only
 * "error sending request" (which hides the reason). Shown in plain language on
 * S3/S32 so a flip-phone user, who has no other way to look, sees the cause.
 */
enum class ConnectionProblem {
    /** The phone can't look up the name (no DNS answer, no signal, typo). */
    NO_ADDRESS,

    /** The server answered, but turned the connection away. */
    REFUSED,

    /** Nothing answered in time. */
    TIMED_OUT,

    /** Connected, but the secure (TLS) connection failed: often a wrong phone
     *  date/time, or a certificate problem on the server. */
    SECURE_CONNECTION,

    /** Android's own networking reached the server; only the Matrix library's
     *  connection failed. */
    LIBRARY_ONLY,

    /** Some other connection failure; the detail says what. */
    OTHER,
}

/**
 * A sign-in failure where the server could not be reached. [detail] is the
 * technical reason (an exception name and message, no secrets) shown after the
 * plain-language text so it can be reported.
 */
class ServerConnectionException(
    val host: String,
    val problem: ConnectionProblem,
    val detail: String,
) : Exception("could not reach $host: $problem ($detail)")
