"""Generates dated test photos for trying the app on an emulator.

Usage: python gen_test_media.py <output dir> [YYYY-MM-DD]

The photos carry EXIF capture times on the given day (default: today) in several years:
single photos, a series of four taken within seconds, and one photo on the day before,
which must not show up in the review. Requires Pillow (`pip install pillow`).

The time zone offset tag matters: without it MediaStore only derives a capture date if the
file was modified within a day of the EXIF time, and the app falls back to the modification
date, so freshly generated files would all count as taken today.
"""

import sys
from datetime import date, datetime, time, timedelta
from pathlib import Path

from PIL import Image, ImageDraw

out = Path(sys.argv[1])
day = date.fromisoformat(sys.argv[2]) if len(sys.argv) > 2 else date.today()
out.mkdir(parents=True, exist_ok=True)


def at(years_ago: int, clock: str, days: int = 0) -> datetime:
    moment = datetime.combine(day.replace(year=day.year - years_ago), time.fromisoformat(clock))
    return moment + timedelta(days=days)


shots = [
    (at(0, "09:15:00"), (40, 120, 160)),
    (at(0, "13:40:00"), (200, 120, 40)),
    # A series: each taken within 30 seconds of the previous one.
    (at(1, "10:00:00"), (60, 160, 80)),
    (at(1, "10:00:05"), (70, 150, 90)),
    (at(1, "10:00:12"), (80, 140, 100)),
    (at(1, "10:00:20"), (90, 130, 110)),
    (at(1, "18:30:00"), (160, 60, 120)),
    (at(3, "12:00:00"), (90, 90, 180)),
    # The day before: only visible when paging back.
    (at(1, "12:00:00", days=-1), (120, 120, 120)),
]

for number, (moment, color) in enumerate(shots):
    stamp = moment.strftime("%Y:%m:%d %H:%M:%S")
    image = Image.new("RGB", (1200, 900) if number % 2 else (900, 1200), color)
    ImageDraw.Draw(image).text((60, 60), f"#{number}  {stamp}", fill="white", font_size=56)
    exif = Image.Exif()
    exif[0x0132] = stamp  # DateTime
    exif.get_ifd(0x8769)[0x9003] = stamp  # DateTimeOriginal
    exif.get_ifd(0x8769)[0x9011] = "+00:00"  # OffsetTimeOriginal
    image.save(out / f"IMG_{number}_{moment:%Y%m%d_%H%M%S}.jpg", exif=exif)

print(f"wrote {len(shots)} photos to {out}")
