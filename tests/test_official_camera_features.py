import json
from pathlib import Path

from cameraboost.official_features import extract_text

ROOT = Path(__file__).resolve().parents[1]

def test_extracts_official_camera_identifiers():
    text = """
    OPLUS_FEATURE_CAMERA_SUPERNIGHT=yes
    OPLUS_FEATURE_CAM_3A_ISP7=yes
    OPLUS_ARCH_EXTENDS_CAM_TUNING_PARAMS=yes
    CONFIG_OPLUS_CAM_EVENT_REPORT=m
    CONFIG_OPLUS_CAMERA_NOTIFY=y
    VendorTag: com.oplus.10bits.heic.encode.support
    APS_ALGO_TURBO_HDR
    Rear: Photo, Video, Master, Underwater
    """
    result = extract_text(text)
    assert "OPLUS_FEATURE_CAMERA_SUPERNIGHT" in result["compile_symbols"]
    assert "OPLUS_FEATURE_CAM_3A_ISP7" in result["compile_symbols"]
    assert "OPLUS_ARCH_EXTENDS_CAM_TUNING_PARAMS" in result["compile_symbols"]
    assert "CONFIG_OPLUS_CAM_EVENT_REPORT" in result["compile_symbols"]
    assert "CONFIG_OPLUS_CAMERA_NOTIFY" in result["compile_symbols"]
    assert result["vendor_tags"] == ["com.oplus.10bits.heic.encode.support"]
    assert result["aps_algorithms"] == ["APS_ALGO_TURBO_HDR"]
    assert result["mode_tokens"] == ["Master", "Photo", "Underwater", "Video"]

def test_does_not_promote_generic_camera_words():
    result = extract_text("camera HDR photo video feature")
    assert result["compile_symbols"] == []
    assert result["vendor_tags"] == []
    assert result["aps_algorithms"] == []

def test_official_catalog_has_no_unsourced_feature():
    data = json.loads((ROOT / "catalog/official-camera-features.json").read_text())
    source_ids = set(data["sources"])
    assert data["features"]
    assert all(row["sources"] for row in data["features"])
    for row in data["features"]:
        assert set(row["sources"]) <= source_ids
    assert data["audit"]["entries_without_sources"] == []

def test_catalog_ids_and_symbols_are_unique():
    data = json.loads((ROOT / "catalog/official-camera-features.json").read_text())
    feature_ids = [row["id"] for row in data["features"]]
    symbols = [row["symbol"] for row in data["official_kernel_feature_macros"]]
    assert len(feature_ids) == len(set(feature_ids))
    assert len(symbols) == len(set(symbols))
