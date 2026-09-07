# ADR 0006 — Voice calls via native MatrixRTC + LiveKit, not a widget

**Status:** Accepted · **Date:** 2026-09

## Context

Voice calls were a v1 non-goal. They are now a requirement, and the requirement
is specifically that MatChat users can call, and be called by, Element users.

The landscape as of now:

- **Legacy 1:1 VoIP** (`m.call.invite` / `answer` / `candidates` / `hangup`) is
  spoken only by Element classic. Element X and the current generation do not
  implement it.
- **MatrixRTC** (MSC4143) is how Element calls work: membership and signalling as
  Matrix state events, media to a pluggable backend. LiveKit (MSC4195, with
  `lk-jwt-service` issuing tokens) is the only working backend.
- **Element Call** is the web UI over MatrixRTC. Element X embeds it in a WebView
  through the Rust SDK's widget driver.

Our target hardware is a 240×320 flip phone, Helio A22 / Snapdragon 215, 2 GB
RAM, no Play Services, no touchscreen.

## Decision

Implement MatrixRTC natively: our own `m.call.member` signalling through the
Matrix SDK, and the **LiveKit Android SDK** for audio, in a native D-pad call UI.
Audio only. No WebView, no Element Call SPA.

First release is **SFU-trusted** (media encrypted in transit, decryptable at our
own LiveKit); full E2EE matching Element Call's key distribution is a scheduled
follow-up, not an omission.

## Alternatives rejected

**Embed the Element Call widget** (what Element X does). Least protocol code, and
interop would be exact. But it means a React application plus WebRTC running in
whatever system WebView a 2019-era AOSP flip happens to ship, driving a UI
designed for touch on a device with no touchscreen. The most valuable property of
this project — that it is usable with a D-pad — cannot survive it.

**Legacy 1:1 `m.call.*`.** Simple, direct WebRTC, no SFU. Fails the actual
requirement: modern Element does not speak it. It solves calling between MatChat
phones and nothing else.

**No Matrix voice; use the PSTN dialer.** These are phones; cell calls work.
Worth stating explicitly because it is the honest baseline — but it does not give
cross-server calling, does not work over data-only connections, and does not
answer the requirement.

## Consequences

- We take on MatrixRTC signalling as our own code — the first significant
  protocol code in a project whose first rule is "do not write protocol code"
  (`AGENTS.md §0`). It is confined to `:core:rtc`, and that rule gains an
  explicit, narrow exception rather than being quietly bent.
- **The route's cost hinges on one unknown**: whether the pinned `sdk-android`
  FFI can send and observe arbitrary `m.call.member` state events. If not, we
  either drive the widget machinery headlessly or ship a small Rust shim with our
  own uniffi binding. This is spike #1 and it gates the estimate.
- Server-side we now run **LiveKit + `lk-jwt-service`** beside Synapse. The SFU
  also removes the need for a separate TURN server in most deployments.
- Bundled WebRTC natives are the largest single addition to the APK; the 25 MB
  per-ABI budget must be re-measured and reset honestly.
- Group calls come nearly free with an SFU. Do not build a separate 1:1 path.
- Because v1 is not end-to-end encrypted, the in-call screen says so. An app that
  encrypts messages and quietly does not encrypt calls is telling the user
  something untrue.
- Element Call 0.24.0 deprecated `.well-known` transport discovery in favour of
  the MSC4519 endpoint; implement the endpoint with a `.well-known` fallback and
  expect this area to keep moving.
