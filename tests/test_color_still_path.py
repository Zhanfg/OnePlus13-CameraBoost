import json
from pathlib import Path

from cameraboost.color_still import (
    ColorPathEvidence,
    OPLUS_10BIT_HEIF_SYMBOLS,
    assess_official_path,
    evidence_from_scan,
)
from cameraboost.official_features import extract_text

ROOT = Path(__file__).resolve().parents[1]

def test_official_path_profile_covers_color_still_p0_scope():
    plan = json.loads((ROOT / "profiles/oneplus13-p0-plan.json").read_text())
    path = json.loads((ROOT / "profiles/oneplus13-color-still-official-path.json").read_text())
    expected = next(x for x in plan["workstreams"] if x["id"] == "color-still")["feature_ids"]
    assert set(path["scope"]) == set(expected)

def test_historical_oplus_imaging_symbols_are_explicit():
    path = json.loads((ROOT / "profiles/oneplus13-color-still-official-path.json").read_text())
    symbols = {x["symbol"] for x in path["historical_oplus_imaging_symbols"]}
    assert symbols == OPLUS_10BIT_HEIF_SYMBOLS

def test_path_assessment_does_not_skip_missing_hal():
    evidence = ColorPathEvidence(
        oplus_system_symbols=frozenset(OPLUS_10BIT_HEIF_SYMBOLS),
        heif_encoder_declared=True,
        ten_bit_heif_encoder_declared=True,
        color_metadata_declared=True,
        camera_or_aps_gate_identified=True,
    )
    result = assess_official_path(evidence)
    assert result["official_path_complete"] is False
    assert result["next_layer"] == "camera-hal"

def test_path_reaches_implementation_ready_only_with_all_layers():
    evidence = ColorPathEvidence(
        oplus_system_symbols=frozenset(OPLUS_10BIT_HEIF_SYMBOLS),
        hal_dynamic_range_ten_bit=True,
        hal_dynamic_range_profiles=True,
        p010_or_private_ten_bit_stream=True,
        heif_encoder_declared=True,
        ten_bit_heif_encoder_declared=True,
        color_metadata_declared=True,
        camera_or_aps_gate_identified=True,
    )
    result = assess_official_path(evidence)
    assert result["official_path_complete"] is True
    assert result["next_layer"] == "implementation-ready-for-device-validation"

def test_scan_builds_conservative_evidence():
    scan = extract_text(
        """
        OPLUS_FEATURE_10BIT_HEIF=yes
        OPLUS_FEATURE_HEIF_CONVERTER=yes
        ANDROID_REQUEST_AVAILABLE_CAPABILITIES_DYNAMIC_RANGE_TEN_BIT
        ANDROID_REQUEST_AVAILABLE_DYNAMIC_RANGE_PROFILES_MAP
        YCBCR_P010
        HEIC
        REC2020
        """
    )
    evidence = evidence_from_scan([scan])
    result = assess_official_path(evidence)
    assert evidence.hal_dynamic_range_ten_bit is True
    assert evidence.hal_dynamic_range_profiles is True
    assert evidence.p010_or_private_ten_bit_stream is True
    assert evidence.heif_encoder_declared is True
    assert evidence.color_metadata_declared is True
    assert evidence.ten_bit_heif_encoder_declared is False
    assert result["next_layer"] == "heif-encoder"
