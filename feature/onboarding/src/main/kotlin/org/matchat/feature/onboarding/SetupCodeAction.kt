package org.matchat.feature.onboarding

sealed interface SetupCodeAction {
    /** "Scan QR setup code". */
    data object Scan : SetupCodeAction

    /** A setup code came back from the scan screen (S31). */
    data class Scanned(val text: String) : SetupCodeAction

    /** "Paste setup link": the clipboard's text, or null when it was empty. */
    data class Paste(val text: String?) : SetupCodeAction

    /** "No, help me set it up". */
    data object Help : SetupCodeAction
}

sealed interface SetupCodeNav {
    data object OpenScanner : SetupCodeNav

    /** A usable code is waiting in [PendingSetupCode]: on to S3/S32. */
    data object OpenSignIn : SetupCodeNav
    data object OpenHelp : SetupCodeNav
}
