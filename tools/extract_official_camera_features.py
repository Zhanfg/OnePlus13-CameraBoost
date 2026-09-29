#!/usr/bin/env python3
"""Scan plaintext official OPPO/OnePlus artifacts for camera identifiers."""
from __future__ import annotations

import argparse
import json
from pathlib import Path

from cameraboost.official_features import extract_file

def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("paths", nargs="+", type=Path)
    ap.add_argument("-o", "--output", type=Path)
    args = ap.parse_args()
    payload = {"schema_version": 1, "records": [extract_file(p) for p in args.paths]}
    rendered = json.dumps(payload, ensure_ascii=False, indent=2) + "\n"
    if args.output:
        args.output.write_text(rendered, encoding="utf-8")
    else:
        print(rendered, end="")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
