# MatChat

A [Matrix](https://matrix.org) messaging app for **D-pad feature phones** —
Kyocera flip phones and similar AOSP "dumbphones" used as filtered / "kosher"
phones. Group chat that works with a directional pad and two softkeys, with **no
way to browse or search for people or rooms** — by design, and enforced by the
build.

## Who it's for

People who carry a locked-down feature phone — no touchscreen, no Google Play,
a 2.6″ 240×320 screen — and still need real group messaging. Every mainstream
messenger ships a directory, a people search, or a media feed that makes it
unacceptable on such a device. MatChat deliberately has none of those.

## What it does

- **Fully operable with the D-pad and two softkeys.** No touchscreen is
  assumed or required. LEFT softkey is always *Options*, RIGHT is always *Back*,
  CENTER activates the focused item — on every screen.
- **End-to-end encrypted** by default, using the same Matrix Rust SDK that
  Element X ships. An unencrypted room shows a visible warning.
- **No discovery, ever.** No public room directory, no user search, no filter
  boxes. You join a room only by *accepting an invitation* or by *starting a
  chat with an address you already know* (e.g. `@wayne:example.org`). Contacts
  and recents are short scrollable lists, never a search surface.
- **Manageable by an administrator.** On a managed device, an allow-list of
  homeserver domains can be pushed over standard Android managed configuration
  (MDM). On an unmanaged phone it stays open — the phone is never left unable to
  message anyone with no explanation.
- **Legible without reading glasses** at 2.6″: a strict type floor (body 16 sp,
  labels 14 sp, metadata 11 sp) and high-contrast focus highlighting.

Designed for the Kyocera DuraXV Extreme+ (reference device), DuraXV Extreme,
DuraXE Epic, and similar Sonim / TCL AOSP flip phones (2 GB RAM, `minSdk 24`).

## How it's delivered

These phones have no app store. MatChat is **sideloaded** (ADB / WebADB) and
kept in sync by a foreground service, since there is no Google push. See
[`docs/DEVICE-SETUP.md`](docs/DEVICE-SETUP.md) for enrollment and
[`docs/SERVER.md`](docs/SERVER.md) / [`docs/MDM.md`](docs/MDM.md) for the
homeserver and managed-configuration setup an administrator provides.

## Project status

Actively under development, pre-release. The module graph, build, and CI are in
place; every screen is reachable with the D-pad and softkeys only. `:core:matrix`
is wired to the Matrix Rust SDK (`org.matrix.rustcomponents:sdk-android`):
password sign-in, Keystore-encrypted session persistence and restore, the sync
foreground service, and the joined **room list** and **timelines** (send, read,
media) via sliding sync are implemented.

Invitations, device verification (emoji SAS), and direct-chat-by-address are
partly wired and being completed — search for `FFI follow-up` in `:core:matrix`
for the remaining SDK bring-up points. This is an FFI integration in progress;
version-sensitive SDK calls are marked `FFI:` for the first on-device compile.

Not yet built (see [`PLAN.md`](PLAN.md) for milestones and non-goals): battery
tuning on hardware, the Help screen, and the field pilot. Voice/video calls,
spaces, threads, and any room/user discovery are **permanent non-goals**.

## Documentation

| File | What it is |
|---|---|
| [`PLAN.md`](PLAN.md) | Development plan: goals, stack, architecture, milestones, risks |
| [`AGENTS.md`](AGENTS.md) | Rules for AI agents (and new humans) contributing code |
| [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) | Module contracts and data flow |
| [`docs/UX-SPEC.md`](docs/UX-SPEC.md) | Every screen, key map, and focus order |
| [`docs/MDM.md`](docs/MDM.md) | Managed-configuration keys and the domain allow-list |
| [`docs/SERVER.md`](docs/SERVER.md) | Synapse hardening for the lockdown half |
| [`docs/DEVICE-SETUP.md`](docs/DEVICE-SETUP.md) | Sideloading to Kyocera flips |
| [`docs/adr/`](docs/adr) | One file per irreversible decision |

## Building

Requires the Android SDK (`compileSdk 35`, `minSdk 24`) and **JDK 17**.

```bash
./gradlew spotlessApply detektAll test   # format, static analysis, unit + architecture tests
./gradlew verifyPaparazziDebug           # screenshot diffs (240×320)
./gradlew :app:assembleDebug             # build the debug APK
./gradlew :app:installDebug              # install to the reference device (Kyocera DuraXV Extreme+)
```

Architecture rules (SDK confined to `:core:matrix`, no feature→feature deps, no
discovery APIs) run as JVM unit tests in `:app` under
`org.matchat.client.arch.*` and are covered by `test` above.

For UI work, use an emulator profile of **240×320 mdpi, API 24, touch disabled**.
The nightly key-only traversal suite runs on that profile
(`.github/workflows/traversal.yml`).

On an SSL-inspecting corporate proxy, Gradle downloads fail with `PKIX path
building failed` until the proxy's root CA is imported — see
[`PLAN.md`](PLAN.md) §11.

## Contributing

Read [`AGENTS.md`](AGENTS.md) first — it applies to human contributors too. The
three rules that matter most: don't write Matrix protocol code (the SDK does
it), don't write touch code (D-pad + softkeys only), and don't add discovery.
