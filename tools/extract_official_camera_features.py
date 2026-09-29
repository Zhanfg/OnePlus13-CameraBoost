#!/usr/bin/env python3
"""Extract camera capability identifiers from official OPPO/OnePlus public text artifacts.

This deliberately does not decrypt proprietary blobs and does not contain vendor keys.
Feed it plaintext files obtained from official source releases or user-extracted official OTA files.
"""
from __future__ import annotations

import argparse
import json
import re
from pathlib import Path

PATTERNS = {
    "compile_symbols": re.compile(r"\b(?:OPLUS_FEATURE_CAMERA[A-Z0-9_]*|OPLUS_ARCH_EXTENDS_CAM[A-Z0-9_]*|CONFIG_OPLUS_CAM[A-Z0-9_]*)\b"),
    "vendor_tags": re.compile(r"\bcom\.oplus\.[A-Za-z0-9._-]+\b"),
    "aps_algorithms": re.compile(r"\bAPS_ALGO_[A-Z0-9_]+\b"),
}

MODE_LINE = re.compile(r"(?im)^\s*(?:rear|front|shooting modes?|camera modes?)\s*:\s*(.+)$")

def _dedupe(values):
    return sorted(set(v.strip() for v in values if v and v.strip()))

def extract_text(text: str) -> dict[str, list[str]]:
    result = {name: _dedupe(pattern.findall(text)) for name, pattern in PATTERNS.items()}
    mode_tokens: list[str] = []
    for match in MODE_LINE.finditer(text):
        raw = match.group(1)
        mode_tokens.extend(re.split(r"\s*[,;/|]\s*", raw))
    result["mode_tokens"] = _dedupe(mode_tokens)
    return result

def extract_file(path: Path) -> dict:
    text = path.read_text(encoding="utf-8", errors="replace")
    return {"path": str(path), **extract_text(text)}

def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("paths", nargs="+", type=Path)
    ap.add_argument("-o", "--output", type=Path)
    args = ap.parse_args()

    records = [extract_file(path) for path in args.paths]
    payload = {"schema_version": 1, "records": records}
    rendered = json.dumps(payload, ensure_ascii=False, indent=2) + "\n"
    if args.output:
        args.output.write_text(rendered, encoding="utf-8")
    else:
        print(rendered, end="")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
