#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
from pathlib import Path

from cameraboost.ota_inventory import index_source


def main() -> int:
    ap = argparse.ArgumentParser(
        description="Index camera/10-bit-related surfaces in an official OTA/ROM archive or extracted tree."
    )
    ap.add_argument("source", type=Path)
    ap.add_argument("-o", "--output", type=Path)
    ap.add_argument("--max-text-bytes", type=int, default=4 * 1024 * 1024)
    ap.add_argument("--max-hash-bytes", type=int, default=8 * 1024 * 1024)
    args = ap.parse_args()

    payload = index_source(
        args.source,
        max_text_bytes=args.max_text_bytes,
        max_hash_bytes=args.max_hash_bytes,
    )
    text = json.dumps(payload, ensure_ascii=False, indent=2) + "\n"

    if args.output:
        args.output.write_text(text, encoding="utf-8")
    else:
        print(text, end="")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
