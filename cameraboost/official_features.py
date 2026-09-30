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
    "imaging_symbols": re.compile(
        r"\b(?:"
        r"OPLUS_FEATURE_10BIT_HEIF|"
        r"OPLUS_FEATRUE_HEIF_OPTIMIZE|"
        r"OPLUS_FEATURE_HEIF_CONVERTER|"
        r"OPLUS_FEATURE_IMAGE_PROCESSING|"
        r"OPLUS_FEATURE_ROI_ENCODE_QCOM"
        r")\b"
    ),
    "vendor_tags": re.compile(r"\bcom\.oplus\.[A-Za-z0-9._-]+\b"),
    "aps_algorithms": re.compile(r"\bAPS_ALGO_[A-Z0-9_]+\b"),
    "android_10bit_tokens": re.compile(
        r"\b(?:"
        r"ANDROID_REQUEST_AVAILABLE_CAPABILITIES_DYNAMIC_RANGE_TEN_BIT|"
        r"ANDROID_REQUEST_AVAILABLE_DYNAMIC_RANGE_PROFILES_MAP|"
        r"ANDROID_REQUEST_RECOMMENDED_TEN_BIT_DYNAMIC_RANGE_PROFILE|"
        r"DYNAMIC_RANGE_TEN_BIT|"
        r"recommendedTenBitDynamicRangeProfile|"
        r"availableDynamicRangeProfilesMap|"
        r"YCBCR_P010|"
        r"HEIC_ULTRAHDR|"
        r"HEIC|"
        r"HEIF"
        r")\b",
        re.IGNORECASE,
    ),
    "color_tokens": re.compile(
        r"\b(?:"
        r"REC[ ._-]?2020|"
        r"BT[ ._-]?2020|"
        r"DCI[ ._-]?P3|"
        r"DISPLAY[ ._-]?P3|"
        r"HLG10|"
        r"HDR10\+?|"
        r"DOLBY[ ._-]?VISION|"
        r"PROXDR"
        r")\b",
        re.IGNORECASE,
    ),
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
