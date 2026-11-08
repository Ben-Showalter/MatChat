package org.matchat.core.model.setup

import java.net.URLDecoder

/**
 * What a MatChat setup code carries (docs/adr/0010, UX-SPEC S4/S32): any of
 * homeserver, username, password and recovery key, so none of them has to be
 * typed on a T9 keypad. [toString] hides the secrets, so a stray log line or
 * crash report can never carry them (AGENTS.md §9).
 */
class SetupCode(
    val server: String?,
    val user: String?,
    val password: String?,
    val recoveryKey: String?,
) {
    override fun equals(other: Any?): Boolean =
        other is SetupCode && server == other.server && user == other.user &&
            password == other.password && recoveryKey == other.recoveryKey

    override fun hashCode(): Int = listOf(server, user, password, recoveryKey).hashCode()

    override fun toString(): String =
        "SetupCode(server=$server, user=$user, password=${mask(password)}, recoveryKey=${mask(recoveryKey)})"

    private fun mask(secret: String?) = if (secret == null) "null" else "••••"
}

/**
 * The one place that reads setup codes (Flip-DumbPhoneGuide "Setup codes",
 * docs/adr/0010). Pure Kotlin, unit-tested, never logs anything it reads.
 *
 * Accepted: `matchat:add?v=1&server=…&user=…&pass=…&key=…` (values
 * URL-encoded, empty fields left out), and the same after `#` in an https link
 * (`https://host/…#add?…` — the part after `#` never reaches a web server).
 * Version 1 is accepted forever: printed codes live for years. Unknown keys
 * are ignored, so a later page can add fields without breaking older apps.
 */
object SetupCodeParser {

    sealed interface Result {
        data class Ok(val code: SetupCode) : Result

        /** Not a MatChat setup code (another app's QR code, a web link, …). */
        data object NotASetupCode : Result

        /** Made by a newer MatChat than this one: update first. */
        data object TooNew : Result
    }

    const val SCHEME = "matchat"
    private const val SUPPORTED_VERSION = 1

    fun parse(text: String?): Result {
        val code = text?.trim()?.removeSurrounding("\"")?.trim().orEmpty()
        val query = linkQuery(code) ?: return Result.NotASetupCode
        val fields = HashMap<String, String>()
        for (pair in query.split('&')) {
            if (pair.isEmpty()) continue
            fields[decode(pair.substringBefore('=')).lowercase()] = decode(pair.substringAfter('=', ""))
        }
        val version = fields["v"]?.toIntOrNull() ?: SUPPORTED_VERSION
        if (version > SUPPORTED_VERSION) return Result.TooNew
        val setup = SetupCode(
            server = fields["server"]?.trim()?.takeIf { it.isNotEmpty() },
            user = fields["user"]?.trim()?.takeIf { it.isNotEmpty() },
            password = fields["pass"]?.takeIf { it.isNotEmpty() },
            recoveryKey = fields["key"]?.trim()?.takeIf { it.isNotEmpty() },
        )
        val empty = setup.server == null && setup.user == null && setup.password == null && setup.recoveryKey == null
        return if (empty) Result.NotASetupCode else Result.Ok(setup)
    }

    /** The `name=value&…` part of a setup link, or null if [code] isn't one. */
    private fun linkQuery(code: String): String? {
        val prefix = "$SCHEME:add"
        if (code.startsWith(prefix, ignoreCase = true)) {
            return code.substring(prefix.length).removePrefix("//").removePrefix("?")
        }
        if (code.startsWith("https://", ignoreCase = true) || code.startsWith("http://", ignoreCase = true)) {
            val fragment = code.substringAfter('#', "")
            if (fragment.startsWith("add?")) return fragment.removePrefix("add?")
        }
        return null
    }

    /** URL-decodes, keeping a literal `+` (the setup page's encodeURIComponent
     *  writes a space as %20, so a `+` really is a plus — common in passwords). */
    internal fun decode(value: String): String =
        try {
            URLDecoder.decode(value.replace("+", "%2B"), "UTF-8")
        } catch (e: IllegalArgumentException) {
            value
        }
}

/**
 * Reading a scanned QR code where a single value is wanted rather than a full
 * setup code (UX-SPEC S7, S12, S21). Each accepts the plain value or a setup
 * code that carries it, so one printed code can serve several screens.
 */
object ScannedValue {

    private val ADDRESS = Regex("^@[^:\\s]+:[^\\s]+$")
    private const val MATRIX_TO = "https://matrix.to/#/"

    /** A Matrix address: `@user:server`, a matrix.to link to one, or a setup
     *  code's `user` (+ `server` when `user` is just the local part). */
    fun address(text: String?): String? {
        val raw = text?.trim().orEmpty()
        if (ADDRESS.matches(raw)) return raw
        if (raw.startsWith(MATRIX_TO, ignoreCase = true)) {
            val target = SetupCodeParser.decode(raw.substring(MATRIX_TO.length).substringBefore('?'))
            if (ADDRESS.matches(target)) return target
        }
        val code = (SetupCodeParser.parse(raw) as? SetupCodeParser.Result.Ok)?.code ?: return null
        val user = code.user ?: return null
        if (ADDRESS.matches(user)) return user
        val server = code.server?.removePrefix("https://")?.removePrefix("http://")?.trimEnd('/') ?: return null
        return "@${user.removePrefix("@")}:$server".takeIf { ADDRESS.matches(it) }
    }

    /** A recovery key: a setup code's `key`, or the plain key itself — Matrix
     *  recovery keys are base58 in groups of four (`EsTj 3yST y93F …`). */
    fun recoveryKey(text: String?): String? {
        val raw = text?.trim().orEmpty()
        when (val parsed = SetupCodeParser.parse(raw)) {
            is SetupCodeParser.Result.Ok -> return parsed.code.recoveryKey
            SetupCodeParser.Result.TooNew -> return null
            SetupCodeParser.Result.NotASetupCode -> Unit
        }
        val compact = raw.replace(Regex("\\s+"), "")
        return raw.takeIf { compact.length >= MIN_KEY_CHARS && compact.all { it in BASE58 } }
    }

    private const val MIN_KEY_CHARS = 40
    private const val BASE58 = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"
}
