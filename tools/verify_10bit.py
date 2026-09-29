#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
import shutil
import subprocess
import sys
from pathlib import Path
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

def run_ffprobe(path: Path) -> dict[str, Any]:
    ffprobe = shutil.which("ffprobe")
    if not ffprobe:
        raise RuntimeError("ffprobe is not installed or not in PATH")

    cmd = [
        ffprobe,
        "-v", "error",
        "-select_streams", "v:0",
        "-show_entries",
        "stream=codec_name,codec_long_name,pix_fmt,bits_per_raw_sample,color_space,color_transfer,color_primaries,width,height",
        "-of", "json",
        str(path),
    ]
    proc = subprocess.run(cmd, check=False, capture_output=True, text=True)
    if proc.returncode != 0:
        raise RuntimeError(proc.stderr.strip() or f"ffprobe failed with {proc.returncode}")
    return json.loads(proc.stdout)

def main() -> int:
    ap = argparse.ArgumentParser(
        description="Verify whether an output photo is actually encoded with >=10-bit samples."
    )
    ap.add_argument("image", type=Path)
    ap.add_argument("--json", action="store_true", help="Print machine-readable JSON")
    args = ap.parse_args()

    try:
        payload = run_ffprobe(args.image)
        result = classify_ffprobe(payload)
    except Exception as exc:
        result = {
            "verified_10bit": False,
            "status": "inconclusive",
            "error": str(exc),
        }

    if args.json:
        print(json.dumps(result, ensure_ascii=False, indent=2))
    else:
        print(f"status: {result['status']}")
        print(f"verified_10bit: {result['verified_10bit']}")
        if "stream" in result and result["stream"]:
            for key, value in result["stream"].items():
                print(f"{key}: {value}")
        if result.get("error"):
            print(f"error: {result['error']}")

    return 0 if result.get("verified_10bit") else 2

if __name__ == "__main__":
    raise SystemExit(main())
