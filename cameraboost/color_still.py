from __future__ import annotations

from dataclasses import dataclass, asdict
from typing import Iterable

OPLUS_10BIT_HEIF_SYMBOLS = {
    "OPLUS_FEATURE_10BIT_HEIF",
    "OPLUS_FEATRUE_HEIF_OPTIMIZE",
    "OPLUS_FEATURE_HEIF_CONVERTER",
    "OPLUS_FEATURE_IMAGE_PROCESSING",
    "OPLUS_FEATURE_ROI_ENCODE_QCOM",
}

HAL_CORE_SIGNALS = {
    "dynamic_range_ten_bit",
    "dynamic_range_profiles",
    "ten_bit_stream",
}

@dataclass(frozen=True)
class ColorPathEvidence:
    oplus_system_symbols: frozenset[str] = frozenset()
    hal_dynamic_range_ten_bit: bool = False
    hal_dynamic_range_profiles: bool = False
    p010_or_private_ten_bit_stream: bool = False
    heif_encoder_declared: bool = False
    ten_bit_heif_encoder_declared: bool = False
    color_metadata_declared: bool = False
    camera_or_aps_gate_identified: bool = False

def assess_official_path(evidence: ColorPathEvidence) -> dict:
    found_symbols = sorted(OPLUS_10BIT_HEIF_SYMBOLS & set(evidence.oplus_system_symbols))
    hal_signals = {
        "dynamic_range_ten_bit": evidence.hal_dynamic_range_ten_bit,
        "dynamic_range_profiles": evidence.hal_dynamic_range_profiles,
        "ten_bit_stream": evidence.p010_or_private_ten_bit_stream,
    }
    hal_complete = all(hal_signals.values())

    if not found_symbols:
        next_layer = "oplus-system-feature-or-ota-config"
    elif not hal_complete:
        next_layer = "camera-hal"
    elif not evidence.ten_bit_heif_encoder_declared:
        next_layer = "heif-encoder"
    elif not evidence.color_metadata_declared:
        next_layer = "color-metadata"
    elif not evidence.camera_or_aps_gate_identified:
        next_layer = "oplus-camera-aps-gate"
    else:
        next_layer = "implementation-ready-for-device-validation"

    return {
        **asdict(evidence),
        "oplus_system_symbols": found_symbols,
        "hal_signals": hal_signals,
        "hal_contract_complete": hal_complete,
        "official_path_complete": next_layer == "implementation-ready-for-device-validation",
        "next_layer": next_layer,
    }

def evidence_from_scan(records: Iterable[dict]) -> ColorPathEvidence:
    imaging_symbols: set[str] = set()
    android_tokens: set[str] = set()
    color_tokens: set[str] = set()

    for record in records:
        imaging_symbols.update(record.get("imaging_symbols", []))
        android_tokens.update(token.lower() for token in record.get("android_10bit_tokens", []))
        color_tokens.update(token.lower() for token in record.get("color_tokens", []))

    has_dynamic = any("dynamic_range_ten_bit" in token for token in android_tokens)
    has_profiles = any("dynamic_range_profiles" in token or "dynamicrangeprofiles" in token for token in android_tokens)
    has_p010 = any("p010" in token for token in android_tokens)
    has_heif = any(token in {"heif", "heic", "heic_ultrahdr"} for token in android_tokens)
    has_color = bool(color_tokens)

    return ColorPathEvidence(
        oplus_system_symbols=frozenset(imaging_symbols),
        hal_dynamic_range_ten_bit=has_dynamic,
        hal_dynamic_range_profiles=has_profiles,
        p010_or_private_ten_bit_stream=has_p010,
        heif_encoder_declared=has_heif,
        ten_bit_heif_encoder_declared=False,
        color_metadata_declared=has_color,
        camera_or_aps_gate_identified=False,
    )
