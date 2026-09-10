# ADR 0007 — A user preference to swap the Left/Right softkeys

**Status:** Accepted · **Date:** 2026-09

## Context

`AGENTS.md` §4 and `UX-SPEC.md` §2 both state a global, unconditional rule:
LEFT softkey is always Options, RIGHT is always Back — "no screen reassigns a
key to a different meaning." That rule exists to keep the app predictable
across every screen.

Some feature-phone hardware exists (or is configured) with its physical
Left/Right softkeys mounted or wired in reverse of the AOSP default, so the
key that is physically on the left dispatches what the platform reports as
`KEYCODE_SOFT_RIGHT` (or vice versa). On that hardware, the app's own
consistent LEFT=Options mapping becomes *inconsistent with the phone itself*
— Options is reachable on the physical right, Back on the physical left,
with no visual cue why.

## Decision

Add one user preference, `Settings > Advanced > "Swap Left/Right keys"`
(`UserPreferences.softkeysSwapped`, `core/ui`), off by default. When on:

- `KeyMap.map(event, swapped)` flips `LogicalKey.SOFT_LEFT`/`SOFT_RIGHT` after
  its normal per-device keycode table, so a physical-left press yields
  `SOFT_RIGHT` and vice versa.
- Every screen's `SoftkeyFragment` still receives `LogicalKey.SOFT_LEFT` as
  "Options" and `SOFT_RIGHT` as "Back" — the swap happens once, centrally, in
  `KeyMap`; no feature code changes meaning.
- The on-screen labels mirror position (`mirroredLabels`,
  `SoftkeyFragment.kt`) so the visible text still matches which physical
  button does what; `SoftkeyBarView`'s tap fallback dispatches to match.
- **`KEYCODE_BACK` is exempt, always.** Some hardware has a dedicated Back
  key distinct from the two labeled positional softkeys — a universal "go
  back" affordance, not one of the two things this preference is about.
  `KeyMap.map` special-cases it before the swap step, unconditionally
  returning `SOFT_RIGHT` regardless of `swapped`. (First shipped without
  this exemption — `KEYCODE_BACK` and `KEYCODE_SOFT_RIGHT` originally shared
  one `LogicalKey` in the per-device table, a fine simplification before
  this preference existed, since they meant the same thing; swapping then
  incorrectly carried the dedicated Back key along with the positional
  right softkey. Fixed same day, on user report.)

This is a **narrow, explicit exception** to the "no screen reassigns a key"
rule, not a repeal of it: the rule is about screens giving LEFT/RIGHT
*different meanings from each other* (e.g. a screen making LEFT mean Back).
This preference doesn't do that — Options is still always reachable via one
softkey and Back via the other; only which *physical* key produces which
*logical* key changes, uniformly, for every screen, at once.

## Rationale

- Centralizing the flip in `KeyMap` (the one file that reads a raw keycode,
  per `AGENTS.md` §4) keeps every downstream consumer — 20+ `SoftkeyFragment`
  subclasses — unaware the preference exists. No screen re-implements it.
- A `StateFlow<Boolean>` read directly (`.value`) at dispatch time, not
  collected/cached: the swap takes effect on the very next key press, no
  Activity recreate needed (unlike the theme preferences, which do recreate
  — this one has no visual chrome to rebuild, just future key events).

## Consequences

- `AGENTS.md` §4 carries a named-exception note pointing here.
- Any *future* per-screen key reassignment proposal is still a "stop and
  ask" per `AGENTS.md` §1 — this ADR authorizes exactly this one global,
  uniform swap, not a precedent for screen-specific remapping.
- If a specific device SKU is later found to need this permanently (not as a
  user toggle), that's a `KeyMap` per-device table entry instead, per its own
  existing convention — not a reason to remove this preference, which serves
  a different case (unconfigured/unknown hardware, or a device that's
  reversed but not per-device-detectable).

## Addendum: an optional AccessibilityService for a predictive-text conflict

Diagnosed with the user via `adb logcat` on real hardware: on a device
running a system predictive-text ("T9word") keyboard, that IME consumes
`KEYCODE_SOFT_RIGHT` before `MainActivity.dispatchKeyEvent` ever sees it —
but only while composing (an `EditText`/IME is active). Outside of
composing, the normal path already worked. (A separate app on that device,
TurboText, also runs its own accessibility-based key *logger* — confirmed
via its own log output to be observing, not consuming, the key; a red
herring in the investigation, not the actual cause.)

This is a real platform conflict between two apps' hardware-key handling,
not something fixable by changing what `KeyMap`/`dispatchKeyEvent` do with
an event they never receive. The fix: `MatChatKeyAccessibilityService`
(`app/accessibility`), an `AccessibilityService` requesting
`FLAG_REQUEST_FILTER_KEY_EVENTS` — a flag whose documented purpose is
letting a service see hardware keys earlier in the platform's dispatch
pipeline than IME processing does. It claims only the four softkey-ish
codes (`SOFT_LEFT`/`SOFT_RIGHT`/`MENU`/`BACK`) and hands them to
`MainActivity.handleExternalSoftkey`, which runs them through the exact
same `KeyMap` + `LogicalKeyReceiver` path `dispatchKeyEvent` already uses —
no parallel/divergent key-handling logic. Every other code (T9's own
digit/D-pad/CENTER input) is explicitly never touched, so text entry is
unaffected whether or not this service is enabled.

It requests no window-content access (`canRetrieveWindowContent="false"` —
this service never reads the screen) and is entirely inert unless the user
explicitly enables it in system Accessibility settings; `Settings >
Advanced` (S25) got a second row linking straight there, since it's a
system-level toggle this app can neither read nor set itself. Nothing about
normal `dispatchKeyEvent` handling changes for a device or user that
doesn't enable it.
