# MatChat setup-code page

One offline HTML file that turns sign-in details (homeserver, username,
password) and/or a recovery key into a **QR code and a link**, so nobody types
them on a T9 keypad. Rules and reasons: docs/adr/0010 and the
Flip-DumbPhoneGuide's "Setup codes" (`docs/text-input.md`).

- Open `matchat-setup.html` in a browser, fill in what the phone needs, and
  scan the QR code on the phone (Welcome › Sign in with setup code). It works
  offline, sends nothing anywhere and forgets everything when closed.
- Link format, version 1: `matchat:add?v=1&server=…&user=…&pass=…&key=…`
  (URL-encoded, empty fields left out). Parsed only in
  `core/model/.../setup/SetupCode.kt`; `v=1` is accepted forever.

| File | What it is |
|---|---|
| `matchat-setup.html` | The built page (also shipped in `feature/scan/src/main/assets/`). |
| `src/matchat-setup.html` | The source: the guide's template with MatChat's `CONFIG`. |
| `src/qrcode.js` | [qrcode-generator](https://github.com/kazuhikoarase/qrcode-generator) 1.4.4, MIT, Kazuhiko Arase |
| `build.py` | Inlines `qrcode.js` into the page so the result is one offline file |

To change it: edit `CONFIG` in `src/matchat-setup.html`, then

```bash
python3 build.py src/matchat-setup.html   # writes matchat-setup.built.html
mv matchat-setup.built.html matchat-setup.html
cp matchat-setup.html ../../feature/scan/src/main/assets/matchat-setup.html
```
