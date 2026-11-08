package org.matchat.feature.onboarding

sealed interface SignInAction {
    /** Field values are passed at submit time — render never reads them back. */
    data class Submit(
        val username: String,
        val password: String,
        val homeserver: String,
    ) : SignInAction
    data object DismissError : SignInAction

    /** Flip the password field between masked dots and plain text. */
    data object ToggleShowPassword : SignInAction

    /** A setup code arrived: scanned (S31), or pasted ([text] null = the
     *  clipboard was empty). Never logged. */
    data class SetupCodeReceived(val text: String?) : SignInAction

    /** S32 "Sign in": use the code's details. */
    data object ConfirmSetupCode : SignInAction

    /** S32 "Use these and type the rest": copy server/username into the form. */
    data object FillFromSetupCode : SignInAction

    /** S32 "Cancel": forget the code. */
    data object CancelSetupCode : SignInAction
}

sealed interface SignInNav {
    data object Success : SignInNav

    /** Signed in, but the setup code's recovery key didn't unlock history:
     *  go on to S7 so it can be entered again. */
    data object RecoveryKeyFailed : SignInNav
}
