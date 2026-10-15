package org.matchat.client.sync

import android.content.Context
import android.os.SystemClock
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.matchat.client.notify.MessageNotifier
import org.matchat.client.notify.NotificationContent
import org.matchat.core.model.RoomSummary
import org.matchat.core.model.notify.RoomNotificationSounds
import org.matchat.core.ui.prefs.UserPreferences
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Turns a joined-room list into per-room "new messages" notifications, raising
 * one when a room's unread count climbs and cancelling it when the room is read.
 * A new notification waits until its text is readable — decrypted, and the
 * room's latest message caught up with the count — or posts without the text
 * after [NotificationGate.DECRYPT_WAIT_MS] (NotificationGate).
 *
 * A [Singleton] so its unread baseline survives the [SyncForegroundService] ->
 * [SyncWorker] handoff when the Android 15 `dataSync` cap forces it (ADR 0004):
 * both observers feed the same instance, so a message counted while the
 * foreground service was alive is not re-alerted by the first fallback sync.
 * The first emission after the process starts only seeds the baseline, so
 * existing unread history never alerts — the same rule the foreground service
 * has always applied on a cold start (the accepted "delayed messages" cost of
 * ADR 0004 is a message that arrives in a window where no observer is running).
 */
@Singleton
class MessageNotifications @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userPreferences: UserPreferences,
    private val roomSounds: RoomNotificationSounds,
) {
    /** Per-room state (NotificationGate), guarded by [lock]: room-list
     *  collectors and the wait timer both touch it. */
    private val lock = Mutex()
    private val states = HashMap<String, NotificationGate.RoomState>()
    private var seeded = false

    /** The latest room list, re-checked when a waiting notification's
     *  [NotificationGate.DECRYPT_WAIT_MS] runs out with no further update. */
    private var lastRooms: List<RoomSummary> = emptyList()
    private val timerScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    suspend fun onRooms(rooms: List<RoomSummary>) = lock.withLock {
        lastRooms = rooms
        if (!seeded) {
            rooms.forEach { states[it.id.value] = NotificationGate.seed(it) }
            seeded = true
            // Cover-screen diagnostics (docs/COVER-DISPLAY.md). The first
            // emission only seeds the baseline, so a message that arrives
            // before this will never alert — a common "nothing showed" cause.
            Log.d(MessageNotifier.COVER_TAG, "onRooms: seeded baseline for ${rooms.size} rooms (no alerts this pass)")
            return@withLock
        }
        evaluate(rooms, SystemClock.elapsedRealtime())
    }

    /** Posts any notification still waiting for its text, without the text,
     *  now — for a caller about to stop observing (SyncWorker's catch-up
     *  window), so a waiting message is never dropped. */
    suspend fun flushPending() = lock.withLock {
        if (seeded) evaluate(lastRooms, SystemClock.elapsedRealtime() + NotificationGate.DECRYPT_WAIT_MS)
    }

    private suspend fun evaluate(rooms: List<RoomSummary>, nowMs: Long) {
        val enabled = userPreferences.notificationsEnabled.value
        var startedWaiting = false
        rooms.forEach { room ->
            val key = room.id.value
            val prev = states[key]
            val result = NotificationGate.decide(prev, room, nowMs, enabled)
            states[key] = result.state
            val id = MessageNotifier.notifId(room.id)
            if (result.action != NotificationGate.Action.NONE) {
                // No PII: notifId, counts, the decision and the enabled flag only.
                Log.d(
                    MessageNotifier.COVER_TAG,
                    "onRooms: id=$id prev=${prev?.unread} now=${room.unreadCount} " +
                        "action=${result.action} enabled=$enabled",
                )
            }
            when (result.action) {
                NotificationGate.Action.POST_WITH_TEXT -> post(room, contentFor(room, room.unreadCount))
                NotificationGate.Action.POST_GENERIC -> post(room, genericContentFor(room, room.unreadCount))
                NotificationGate.Action.CANCEL -> MessageNotifier.cancel(context, room.id)
                NotificationGate.Action.HOLD -> if (prev?.pendingSinceMs == null) startedWaiting = true
                NotificationGate.Action.NONE -> Unit
            }
        }
        if (startedWaiting) scheduleRecheck()
    }

    /** One re-check after the wait, in case no further room-list update
     *  arrives to post a notification that is still waiting. */
    private fun scheduleRecheck() {
        timerScope.launch {
            delay(NotificationGate.DECRYPT_WAIT_MS + RECHECK_MARGIN_MS)
            lock.withLock { evaluate(lastRooms, SystemClock.elapsedRealtime()) }
        }
    }

    private suspend fun post(room: RoomSummary, content: NotificationContent) {
        MessageNotifier.show(
            context,
            room.id,
            content,
            channelVersion = userPreferences.notificationChannelVersion.value,
            soundUri = userPreferences.notificationSoundUri.value,
            // A room's own sound (Room info) wins over the app-wide one.
            roomSound = roomSounds.overrides.value[room.id],
            coverMessageHidden = userPreferences.coverMessageHidden.value,
        )
    }

    /** The latest message's text, unless it is our own — then just the count
     *  (the unread count climbed because of someone else's message that the
     *  room list hasn't caught up with yet). */
    private fun contentFor(room: RoomSummary, unread: Int) = NotificationContent(
        roomName = room.name.ifBlank { room.id.value },
        sender = room.lastMessageSender.takeUnless { room.lastMessageIsOwn },
        text = room.lastMessage.takeUnless { room.lastMessageIsOwn },
        media = room.lastMessageMedia.takeUnless { room.lastMessageIsOwn },
        unread = unread,
    )

    /** The text couldn't be read in time (NotificationGate): the count only. */
    private fun genericContentFor(room: RoomSummary, unread: Int) = NotificationContent(
        roomName = room.name.ifBlank { room.id.value },
        sender = null,
        text = null,
        media = null,
        unread = unread,
    )

    private companion object {
        /** So the re-check lands just after the wait, not a hair before it. */
        const val RECHECK_MARGIN_MS = 100L
    }
}
