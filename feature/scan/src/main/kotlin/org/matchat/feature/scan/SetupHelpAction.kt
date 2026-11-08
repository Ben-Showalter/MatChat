package org.matchat.feature.scan

sealed interface SetupHelpAction {
    /** CENTER on "Save setup page to phone". */
    data object SaveToPhone : SetupHelpAction

    /** Options > Save to SD card. */
    data object SaveToSdCard : SetupHelpAction

    /** Android's answer to the storage-permission request (API 24–28 only). */
    data class StoragePermissionResult(val granted: Boolean) : SetupHelpAction

    /** Options > Send by Bluetooth found nothing that could send the file. */
    data object ShareFailed : SetupHelpAction
}

sealed interface SetupHelpNav {
    /** Ask for WRITE_EXTERNAL_STORAGE (API 24–28) before saving to Download. */
    data object RequestStoragePermission : SetupHelpNav
}
