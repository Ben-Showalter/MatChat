package org.matchat.feature.scan

/** The one line under the S33 rows: where the page was saved, or what went wrong. */
sealed interface SetupHelpMessage {
    data class Saved(val where: String) : SetupHelpMessage
    data object SaveFailed : SetupHelpMessage
    data object NoSdCard : SetupHelpMessage
    data object NoShare : SetupHelpMessage
    data object NeedsStorage : SetupHelpMessage
}

/** S33 "Make a setup code" (UX-SPEC §S33). */
data class SetupHelpState(
    val message: SetupHelpMessage? = null,
)
