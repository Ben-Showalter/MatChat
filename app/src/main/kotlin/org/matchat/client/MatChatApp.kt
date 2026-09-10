package org.matchat.client

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import org.matchat.client.notify.MessageNotifier
import org.matchat.core.ui.prefs.UserPreferences

/** Hilt graph root. The sync foreground service (not this class) owns the SDK
 *  client; the app just constructs the graph (ARCHITECTURE.md "Sync lifecycle"). */
@HiltAndroidApp
class MatChatApp : Application() {

    // Populated by Hilt during super.onCreate() (SyncForegroundService's
    // @Inject session field is the same pattern for a non-Activity class) —
    // safe to read right after that call returns, below.
    @Inject lateinit var userPreferences: UserPreferences

    override fun onCreate() {
        super.onCreate()
        // The incoming-message channel, at whatever sound version is already
        // stored (Notifications settings round) — ensureChannel is idempotent,
        // so this just confirms the channel exists before the sync service's
        // first notification; it does not create a new version on its own.
        MessageNotifier.ensureChannel(
            this,
            userPreferences.notificationChannelVersion.value,
            userPreferences.notificationSoundUri.value,
        )
    }
}
