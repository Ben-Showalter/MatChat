# Tzibbur integration — feasibility

Can MatChat reach [Tzibbur](https://tzibbur.me) the way
[UnTzibburBot](https://github.com/TripleU613/UnTzibburBot) reaches it from
Telegram? Short answer: **yes, technically** — but two constraints (no E2EE, and
a license conflict) shape *how*, and one of them collides with a core MatChat
goal. This is a research note, not a decision. No code has been written.

## What Tzibbur is, and what UnTzibburBot built

**Tzibbur** is a closed group-messaging service for filtered/kosher phones
(`api.tzibbur.me`, REST + WebSocket). Phone-number + SMS-OTP auth; groups,
members, and **text-only** messages (≤ 1000 chars, no edit/delete). It has its
own `kosher` platform type — the same audience MatChat targets. It is **not
end-to-end encrypted**: the server stores message bodies, assigns per-group
sequence numbers, and prefixes each body with the sender's display name.

UnTzibburBot is two Rust crates:

- **`tzibbur-api`** — a from-scratch Tzibbur client (REST, WS, SQLite store,
  sync engine, outbox, session/crypto), reverse-engineered from Tzibbur's
  Android app and verified against the live server. **AGPL-3.0**, pure Rust
  library (no FFI layer).
- **`bridge`** — a public **Telegram bot**: each user links their own Tzibbur
  account, every Tzibbur group becomes a Telegram topic, relayed both ways. A
  server (Docker + Directus/Postgres), not part of the Telegram app.

The key point: he did **not** modify a client. He made Telegram a front-end to
Tzibbur through a **relay service**. The faithful analogy for us is a
Matrix↔Tzibbur relay, which leaves MatChat itself untouched.

## Two hard constraints found up front

1. **No end-to-end encryption.** Tzibbur is a plaintext relay; any bridge, and
   Tzibbur itself, sees cleartext. This contradicts MatChat's **G4 —
   "end-to-end encrypted by default"** (`PLAN.md §2`). Bridged/Tzibbur
   conversations cannot be E2EE; that is inherent, not an implementation gap. It
   would need a documented, user-visible exception to G4 (much like the existing
   "unencrypted room" warning band).
2. **License.** `tzibbur-api` is **AGPL-3.0**; MatChat is **GPL-2.0**. AGPL-3.0
   cannot be linked into a GPLv2 program, and relicensing MatChat to AGPL would
   pull in exactly the GPLv3-family anti-tivoization terms that were
   deliberately rejected (see the License section of `README.md`). This blocks
   *embedding* his crate in the app — but not a *separate* program that talks to
   it over the network.

Neither is a blocker for a bridge; both bite Option 2.

## What is *not* a problem

- **No-discovery principle (G3).** Tzibbur has no directory or user search
  either — membership is by phone contact and by being added to a group. Groups
  arriving as rooms is membership, not discovery. Consistent with MatChat.
- **Rust-on-Android.** MatChat already ships a Rust `.so` (the Matrix SDK) via
  FFI, so a Tzibbur Rust client on `arm64-v8a`/`armeabi-v7a` is proven ground.
- **Model shape.** Tzibbur maps cleanly onto MatChat's `:core:model`:
  group → `RoomSummary`, message → `TimelineItem`, member → `RoomMemberSummary`,
  the read-only "Tzibbur System" thread → an unencrypted room. Text-only, no
  threads/reactions — a *subset* of what the UI already renders.

## Option 1 — Matrix↔Tzibbur bridge service (recommended if we build)

A standalone puppeting bridge beside the homeserver (the pattern the mautrix
bridges use). Each user links their Tzibbur account once; each Tzibbur group
becomes a portal room; messages relay both ways.

- **MatChat changes:** none. It stays a pure Matrix client; the
  `:core:matrix`-only architecture rule is untouched.
- **License:** fine. A separate program communicating over the network is not
  linking, so his AGPL-3.0 `tzibbur-api` can be reused as-is and the bridge can
  be AGPL while MatChat stays GPLv2.
- **Where it lives:** a new repo/component, deployed with the homeserver
  (fits the "run your own server" model in `docs/SERVER.md`).
- **Cost/risks:** operating a stateful relay (holds each user's Tzibbur session,
  sees plaintext — same threat model UnTzibburBot documents); Matrix appservice
  registration; portal-room lifecycle. Most of the Tzibbur half already exists
  in `tzibbur-api`.

## Option 2 — native Tzibbur backend in the app (`:core:tzibbur`)

A second backend module mirroring `:core:matrix`, so MatChat speaks Tzibbur
directly with no homeserver or bridge.

- **License wall:** cannot embed the AGPL-3.0 crate in a GPLv2 app. Requires
  writing our own Tzibbur client from `docs/tzibbur-api.md` (substantial: 26 REST
  endpoints, the WS protocol, a sync engine, SQLite, outbox) **plus** a UniFFI/JNI
  layer the upstream crate doesn't have. Reverse-engineering a proprietary
  service also carries clean-room/legal sensitivity.
- **Architecture cost:** breaks two stated tenets — "UI only over the Matrix
  SDK" and E2EE-by-default. Would need a second session abstraction
  (`TzibburSession`) and an account-switch UX the app has no screens for.
- **Upside:** no server to run; best fit for the exact kosher-phone audience;
  fully offline-capable per device.

## Recommendation

If the goal is "let MatChat users reach Tzibbur with the least disruption,"
**Option 1** is the faithful analogue of what UnTzibburBot did, needs no app
changes, and sidesteps the license wall. **Option 2** is only worth it if the
intent is for MatChat to become a dual-backend (Matrix *and* Tzibbur) client —
a product-scope decision, not a feature — and it forces a from-scratch,
non-AGPL reimplementation.

Either way, **the E2EE exception is a product decision to make explicitly first**:
Tzibbur traffic is plaintext, and users on a security-first client must be told
so, in the UI, per conversation.

## Open questions before any build

1. Is the intent to reach Tzibbur *at all costs*, or to keep MatChat E2EE-pure
   and treat Tzibbur as an explicitly-labelled, non-encrypted exception?
2. Bridge (server we run) vs. native (app speaks Tzibbur) — who operates it, and
   is a homeserver already in the deployment?
3. Legality/ToS of interoperating with Tzibbur's private API for our use — the
   upstream client is reverse-engineered and unaffiliated.
4. Does MatChat's audience actually use Tzibbur, or is this exploratory?

---

*Source reviewed: `github.com/TripleU613/UnTzibburBot` @ HEAD, 2026-09-09 —
`README.md`, `docs/tzibbur-api.md`, `crates/tzibbur-api/README.md`,
`crates/bridge/README.md`.*
