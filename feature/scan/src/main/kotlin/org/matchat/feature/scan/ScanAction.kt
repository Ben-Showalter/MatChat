package org.matchat.feature.scan

sealed interface ScanAction {
    /** The camera found a QR code with this text (never logged). */
    data class Decoded(val text: String) : ScanAction

    /** CENTER on "Scan again" / "Try again", or Options > Scan again. */
    data object ScanAgain : ScanAction

    data object CameraStarted : ScanAction
    data object CameraFailed : ScanAction
    data object PermissionDenied : ScanAction
}

/** One-shot results of S31. */
sealed interface ScanNav {
    /** An accepted value: hand it to the caller and close. */
    data class Accepted(val text: String) : ScanNav

    /** Open the camera again (after Try again). */
    data object RestartCamera : ScanNav
}
