package org.matchat.core.model.setup

/** What a screen asks the scan screen (S31, docs/adr/0010) to accept. Plain
 *  data, here so a ViewModel can name it without touching :core:ui. */
enum class ScanKind {
    /** A MatChat setup code (sign-in details and/or a recovery key) — S3/S4. */
    SETUP_CODE,

    /** A recovery key, plain or in a setup code — S7. */
    RECOVERY_KEY,

    /** A Matrix address: @user:server or a matrix.to link — S12, S21. */
    ADDRESS,
}
