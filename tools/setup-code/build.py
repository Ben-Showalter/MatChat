#!/usr/bin/env python3
"""Builds setup-code.html: src/setup-code.html with src/qrcode.js inlined,
so the page is one file that works offline.

    python3 build.py                 -> setup-code.html (the generic example)
    python3 build.py my-page.html    -> builds from your edited copy instead
"""
import pathlib, sys

here = pathlib.Path(__file__).resolve().parent
source = pathlib.Path(sys.argv[1]) if len(sys.argv) > 1 else here / "src" / "setup-code.html"
lib = (here / "src" / "qrcode.js").read_text(encoding="utf-8")
page = source.read_text(encoding="utf-8")
marker = "/*__QRCODE_LIBRARY__*/"
if page.count(marker) != 1:
    sys.exit(f"{source}: expected exactly one {marker}")
if "</script" in lib.lower():
    sys.exit("qrcode.js contains </script>; can't inline it safely")
out = here / ("setup-code.html" if len(sys.argv) == 1 else source.name.replace(".html", ".built.html"))
out.write_text(page.replace(marker, "// qrcode-generator 1.4.4, (c) Kazuhiko Arase, MIT license (header below). Inlined for offline use.\n" + lib),
               encoding="utf-8")
print(f"Wrote {out}")
