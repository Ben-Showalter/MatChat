# ADR 0010 — QR setup codes: our own scanner (ZXing + the classic camera API)

**Status:** Accepted · **Date:** 2026-11

## Context

Typing a username, a password or a 48-character recovery key on a T9 keypad is
slow and error-prone; long Matrix addresses are not much better. The user asked
for QR scanning everywhere MatChat asks for a long string of characters,
following the Flip-DumbPhoneGuide's "Setup codes" rules (`docs/text-input.md`,
`templates/setup-code/` on its `claude/setup-code-template` branch), which
BusyCall already implements on the same hardware.

Matrix's own QR sign-in (MSC4108, device linking) is a different feature: it
needs an OIDC-native homeserver (MAS) and is not built. It stays a later
option (PLAN.md §7); this ADR does not rule it out.

## Decision

1. **A setup code is a link**, `matchat:add?v=1&server=…&user=…&pass=…&key=…`
   (every value URL-encoded, empty fields left out), also accepted after `#` in
   an https link. It is parsed in **one** place, `SetupCodeParser` in
   `:core:model`, and `v=1` is accepted forever: printed codes live for years.
   Unknown keys are ignored. The same text works as a QR code, a pasted link,
   or a printed page.
2. **Codes are made on a computer** with the offline page in
   `tools/setup-code/` (the guide's template with MatChat's `CONFIG`; the QR
   library, qrcode-generator 1.4.4, MIT, is inlined). It sends nothing
   anywhere. The built page is also shipped in the app (`:feature:scan`
   assets) and can be saved to Download, to the SD card, or sent by Bluetooth
   from S33.
3. **A new module, `:feature:scan`**, holds the camera screen (S31) and S33.
   Every feature that takes a long string opens it through
   `Navigator.toScan(kind)` and gets the accepted text back through the
   Fragment Result API (`ScanRequest`), so features still never depend on
   each other.
4. **The decoder is ZXing core** (`com.google.zxing:core:3.5.3`, Apache-2.0,
   pure Java, no transitive dependencies). Never Google ML Kit (needs Play
   services, which these phones don't have) and never a third-party scanner
   app (most of these phones have none).
5. **The camera is the classic `android.hardware.Camera` API**, not CameraX.
   It is deprecated, but every target flip phone runs it, it adds no library,
   and a ~640×480 preview is light enough for a Snapdragon 215 — BusyCall's
   experience on the same hardware. Frames are decoded one at a time on a
   background thread and never stored.
6. **Secrets stay in memory**: a scanned or pasted code is shown first (S32,
   password and recovery key hidden) and used only on "Sign in"; it is never
   logged (`SetupCode.toString()` masks them), never put in a nav argument
   that Android could write to disk, and the clipboard is cleared after a
   paste. A recovery key in the code is applied right after sign-in.

## Consequences

- One new dependency (ZXing core) and one new module.
- `CAMERA` permission, asked only on first scan; `android.hardware.camera`
  stays `required="false"`, and every scan place also accepts typing or
  pasting, so a phone without a usable camera loses nothing.
- If a later page adds fields, older apps ignore them; if the format ever has
  to change incompatibly, bump `v` and keep reading `v=1`.
