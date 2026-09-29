from __future__ import annotations

from typing import Any

TEN_BIT_PIXEL_MARKERS = ("10", "p010", "yuv420p10", "yuv422p10", "yuv444p10")

def classify_ffprobe(payload: dict[str, Any]) -> dict[str, Any]:
    streams = payload.get("streams") or []
    if not streams:
        return {"verified_10bit": False, "status": "no-image-stream", "stream": None}

    stream = streams[0]
    pix_fmt = str(stream.get("pix_fmt") or "").lower()
    bits_raw = stream.get("bits_per_raw_sample")

    try:
        bits = int(bits_raw) if bits_raw not in (None, "") else None
    except (TypeError, ValueError):
        bits = None

    ten_by_pix = any(marker in pix_fmt for marker in TEN_BIT_PIXEL_MARKERS)
    ten_by_bits = bits is not None and bits >= 10

    return {
        "verified_10bit": bool(ten_by_pix or ten_by_bits),
        "status": "verified-10bit" if (ten_by_pix or ten_by_bits) else "not-verified-10bit",
        "stream": {
            "codec_name": stream.get("codec_name"),
            "codec_long_name": stream.get("codec_long_name"),
            "pix_fmt": stream.get("pix_fmt"),
            "bits_per_raw_sample": stream.get("bits_per_raw_sample"),
            "color_space": stream.get("color_space"),
            "color_transfer": stream.get("color_transfer"),
            "color_primaries": stream.get("color_primaries"),
            "width": stream.get("width"),
            "height": stream.get("height"),
        },
    }
