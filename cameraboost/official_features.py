from __future__ import annotations

import re
from pathlib import Path

PATTERNS = {
    "compile_symbols": re.compile(
        r"\b(?:"
        r"OPLUS_FEATURE_CAMERA[A-Z0-9_]*|"
        r"OPLUS_FEATURE_CAM_[A-Z0-9_]+|"
        r"OPLUS_ARCH_EXTENDS_CAM[A-Z0-9_]*|"
        r"CONFIG_OPLUS_CAM[A-Z0-9_]*"
        r")\b"
    ),
    "vendor_tags": re.compile(r"\bcom\.oplus\.[A-Za-z0-9._-]+\b"),
    "aps_algorithms": re.compile(r"\bAPS_ALGO_[A-Z0-9_]+\b"),
}

MODE_LINE = re.compile(
    r"(?im)^\s*(?:rear|front|shooting modes?|camera modes?)\s*:\s*(.+)$"
)

def _dedupe(values):
    return sorted(set(v.strip() for v in values if v and v.strip()))

def extract_text(text: str) -> dict[str, list[str]]:
    result = {
        name: _dedupe(pattern.findall(text))
        for name, pattern in PATTERNS.items()
    }
    mode_tokens: list[str] = []
    for match in MODE_LINE.finditer(text):
        mode_tokens.extend(re.split(r"\s*[,;/|]\s*", match.group(1)))
    result["mode_tokens"] = _dedupe(mode_tokens)
    return result

def extract_file(path: Path) -> dict:
    text = path.read_text(encoding="utf-8", errors="replace")
    return {"path": str(path), **extract_text(text)}
