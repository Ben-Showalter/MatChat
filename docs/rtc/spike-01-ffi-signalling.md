# Spike 1 — Can the FFI carry MatrixRTC signalling?

**Question (VOICE.md §7.1, ADR 0006):** can the pinned
`org.matrix.rustcomponents:sdk-android:26.09.3` send and observe the
`m.call.member` state events that MatrixRTC (MSC4143) rides on? This gates the
whole route: a "no" means a Rust shim or a headless widget driver.

**Answer: YES — decisively, and more cheaply than the ADR assumed.** The pinned
AAR already carries native MatrixRTC support. No shim, no widget driver.

## Evidence (from the AAR, `javap` on `classes.jar`)

Send side — arbitrary events, including `m.call.member`:

- `Room.sendStateEventRaw(eventType: String, stateKey: String, content: String): String`
  — sends any state event from a JSON string. This is exactly how we publish
  our `m.call.member` membership (join / update / leave).
- `Room.sendRaw(eventType: String, content: String)` — sends any message-like
  event from JSON, e.g. the RTC ring notification.

Observe side — the SDK already parses call membership for us, so we do **not**
hand-parse state events:

- `RoomInfo.hasRoomCall: Boolean`
- `RoomInfo.activeRoomCallParticipants: List<String>`
- `RoomInfo.activeRoomCallConsensusIntent: RtcCallIntentConsensus`

  These arrive through the existing `Room.subscribeToRoomInfoUpdates(...)`
  stream — the same mechanism the room list already uses. An active call, and
  who is in it, is observable with zero new parsing.

Ring / incoming-call detection — modelled as a first-class event:

- `MessageLikeEventContent.RtcNotification { notificationType: RtcNotificationType
  (RING | NOTIFICATION), callIntent: RtcCallIntent (AUDIO | VIDEO), lifetimeMs }`
  — an incoming RING is a timeline event we can match, with an explicit
  lifetime for the "don't ring if stale" rule (VOICE.md §5).

Helpers the ADR did not know we had:

- `Client.isLivekitRtcSupported(): Boolean` — capability probe before offering
  a call.
- `Client.enableAutomaticCallStatus(Boolean)` — the SDK can maintain call
  status for us.
- The SDK also ships a full widget driver (`WidgetDriver`, `WidgetSettings`, …).
  We are **not** using it (ADR 0006 rejects the widget route), but its presence
  is the fallback if the native path ever hits a wall.

## Consequence for the architecture

Because the SDK exposes these primitives, **all Rust-SDK usage stays inside
`:core:matrix`** — the existing "only `:core:matrix` imports the SDK" Konsist
rule is kept intact. `:core:matrix` exposes thin, SDK-free primitives
(`sendStateEvent`, `sendRawEvent`, an active-call read/observe), and the
MatrixRTC *protocol logic* (`m.call.member` content shape, membership lifecycle,
LiveKit focus selection) lives in `:core:rtc` built on those primitives, using
only `:core:model` types.

This is a small, deliberate refinement of ADR 0006, which anticipated
`:core:rtc` importing the SDK directly under a narrow exception. Keeping the SDK
in one module is stricter, not looser, so the exception is not needed. Recorded
here rather than silently diverging.

## What is NOT answered here (still open, need infra / hardware)

- **Spike 2 — two-way audio to Element.** Needs `io.livekit:livekit-android`
  (bundles WebRTC natives), a running LiveKit SFU, and `lk-jwt-service` issuing
  tokens. Cannot be run without that server infrastructure. This is the next
  gate and the one that decides APK size and minSdk-24 viability.
- **Spikes 3–5 — ring latency, cell-call interaction, CPU/heat/battery.** All
  on-device measurements.

The signalling half is now cheap and buildable in-tree; the audio half waits on
the LiveKit + lk-jwt-service deployment and on-device measurement.
