#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
from pathlib import Path

from cameraboost.blob_module import analyze_zip


def main() -> int:
    ap = argparse.ArgumentParser(
        description="Create a derived, non-redistributive audit of an OPlus camera blob module ZIP."
    )
    ap.add_argument("zip", type=Path)
    ap.add_argument("-o", "--output", type=Path)
    args = ap.parse_args()

    payload = analyze_zip(str(args.zip))
    text = json.dumps(payload, ensure_ascii=False, indent=2) + "\n"
    if args.output:
        args.output.write_text(text, encoding="utf-8")
    else:
        print(text, end="")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
