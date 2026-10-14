#!/usr/bin/env python3
"""Draws the MatChat mark for the Kyocera cover card's icon slot.

The cover template draws the icon from a resource id only (no runtime
images), scaled into a small square slot; a 96x96 nodpi square drew best on
the DuraXV (docs/COVER-DISPLAY.md, "Clock on the card"). This is the chat
bubble from app/src/main/res/drawable/ic_launcher.xml, solid black on
transparent, scaled to fill most of the square. Re-run when the launcher
mark changes:

    python3 tools/gen_cover_icon.py

Needs Pillow.
"""
from pathlib import Path

from PIL import Image, ImageDraw

SIZE = 96
SUPERSAMPLE = 8
OUT = Path(__file__).resolve().parent.parent / "app/src/main/res/drawable-nodpi/ic_cover_matchat.png"

# The bubble in ic_launcher.xml's 48-unit viewport: a rounded body from
# (3,4) to (45,34), corner radius 5, with a tail down to (11,44).
BODY = (3, 4, 45, 34)
RADIUS = 5
TAIL = [(22, 34), (11, 44), (11, 33)]
# The bubble's bounding box in that viewport (body plus tail).
BOX = (3, 4, 45, 44)
FILL = 0.9  # share of the square the bubble's larger side takes


def main() -> None:
    big = SIZE * SUPERSAMPLE
    w, h = BOX[2] - BOX[0], BOX[3] - BOX[1]
    scale = big * FILL / max(w, h)
    off_x = (big - w * scale) / 2 - BOX[0] * scale
    off_y = (big - h * scale) / 2 - BOX[1] * scale

    def pt(x: float, y: float) -> tuple[float, float]:
        return (x * scale + off_x, y * scale + off_y)

    img = Image.new("RGBA", (big, big), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    draw.rounded_rectangle([pt(BODY[0], BODY[1]), pt(BODY[2], BODY[3])], radius=RADIUS * scale, fill=(0, 0, 0, 255))
    draw.polygon([pt(x, y) for x, y in TAIL], fill=(0, 0, 0, 255))
    OUT.parent.mkdir(parents=True, exist_ok=True)
    img.resize((SIZE, SIZE), Image.LANCZOS).save(OUT, optimize=True)
    print(f"wrote {OUT}")


if __name__ == "__main__":
    main()
