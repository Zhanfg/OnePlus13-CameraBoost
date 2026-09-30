#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
from pathlib import Path

from cameraboost.ota_materialize import plan_materialization


def main() -> int:
    ap = argparse.ArgumentParser(
        description="Plan safe read-only materialization of an official OTA index."
    )
    ap.add_argument("index", type=Path)
    ap.add_argument("-o", "--output", type=Path)
    args = ap.parse_args()

    index = json.loads(args.index.read_text(encoding="utf-8"))
    payload = plan_materialization(index)
    text = json.dumps(payload, ensure_ascii=False, indent=2) + "\n"

    if args.output:
        args.output.write_text(text, encoding="utf-8")
    else:
        print(text, end="")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
