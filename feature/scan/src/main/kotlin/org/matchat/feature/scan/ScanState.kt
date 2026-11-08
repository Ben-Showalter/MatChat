package org.matchat.feature.scan

/** The one status line under the viewfinder (UX-SPEC S31). */
enum class ScanStatus {
    STARTING,
    POINT,
    NOT_A_SETUP_CODE,
    NOT_A_RECOVERY_KEY,
    NOT_AN_ADDRESS,
    TOO_NEW,
    NO_CAMERA,
    NO_PERMISSION,
}

/**
 * S31 Scan. [status] is the line under the viewfinder; [isProblem] colours it
 * as a problem (never by colour alone — the text says what went wrong);
 * [canRetry] turns the row into "Try again" when the camera isn't running.
 */
data class ScanState(
    val status: ScanStatus = ScanStatus.STARTING,
) {
    val isProblem: Boolean get() = status != ScanStatus.STARTING && status != ScanStatus.POINT
    val canRetry: Boolean get() = status == ScanStatus.NO_CAMERA || status == ScanStatus.NO_PERMISSION
}
