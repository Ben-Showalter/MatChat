package org.matchat.feature.onboarding

import org.matchat.core.model.ErrorText

/**
 * S3 Sign in. The homeserver is read-only when policy pins it (shown grey with a
 * lock glyph). loading/error live here, not in a separate code path (AGENTS.md §3).
 * [found] is the S32 "Setup code found" panel shown in place of the form;
 * [fill] copies a setup code's server/username into the form ("Use these and
 * type the rest"); [recoveryKeyPending] says a scanned recovery key will be
 * used right after sign-in.
 */
data class SignInState(
    val homeserver: String = "",
    val homeserverPinned: Boolean = false,
    val isSubmitting: Boolean = false,
    val showPassword: Boolean = false,
    val error: ErrorText? = null,
    val found: SetupFound? = null,
    val fill: FormFill? = null,
    val recoveryKeyPending: Boolean = false,
    val setupProblem: SetupProblem? = null,
)

/** S32 "Setup code found": what the code holds, secrets never shown. */
data class SetupFound(
    val server: String,
    /** null = the code has no username. */
    val user: String?,
    val hasPassword: Boolean,
    val hasRecoveryKey: Boolean,
    /** The code's server when policy pins a different one: explained, never used. */
    val blockedServer: String? = null,
) {
    val isComplete: Boolean get() = user != null && hasPassword
    val canSignIn: Boolean get() = isComplete && blockedServer == null
}

/** Values to copy into the form once; [id] changes each time so render can
 *  tell a new fill from one it already applied. */
data class FormFill(val id: Int, val server: String?, val user: String?)

/** Why a scanned or pasted code wasn't used (shown in place of the S3 error line). */
enum class SetupProblem { NOT_A_SETUP_CODE, TOO_NEW, NOTHING_TO_PASTE }
