import json
from collections import Counter
from pathlib import Path

from cameraboost.official_features import extract_text

ROOT = Path(__file__).resolve().parents[1]

def load_catalog():
    return json.loads((ROOT / "catalog/official-camera-features.json").read_text())

def test_extracts_official_camera_identifiers():
    text = """
    OPLUS_FEATURE_CAMERA_SUPERNIGHT=yes
    OPLUS_FEATURE_CAM_3A_ISP7=yes
    OPLUS_ARCH_EXTENDS_CAM_TUNING_PARAMS=yes
    CONFIG_OPLUS_CAM_EVENT_REPORT=m
    CONFIG_OPLUS_CAMERA_NOTIFY=y
    OPLUS_FEATURE_10BIT_HEIF=yes
    OPLUS_FEATRUE_HEIF_OPTIMIZE=yes
    OPLUS_FEATURE_HEIF_CONVERTER=yes
    OPLUS_FEATURE_IMAGE_PROCESSING=yes
    OPLUS_FEATURE_ROI_ENCODE_QCOM=yes
    ANDROID_REQUEST_AVAILABLE_CAPABILITIES_DYNAMIC_RANGE_TEN_BIT
    ANDROID_REQUEST_AVAILABLE_DYNAMIC_RANGE_PROFILES_MAP
    YCBCR_P010
    HEIC
    Rec.2020
    HLG10
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
    assert set(result["imaging_symbols"]) == {
        "OPLUS_FEATURE_10BIT_HEIF",
        "OPLUS_FEATRUE_HEIF_OPTIMIZE",
        "OPLUS_FEATURE_HEIF_CONVERTER",
        "OPLUS_FEATURE_IMAGE_PROCESSING",
        "OPLUS_FEATURE_ROI_ENCODE_QCOM",
    }
    assert "YCBCR_P010" in {x.upper() for x in result["android_10bit_tokens"]}
    assert "HEIC" in {x.upper() for x in result["android_10bit_tokens"]}
    assert result["color_tokens"]
    assert result["vendor_tags"] == ["com.oplus.10bits.heic.encode.support"]
    assert result["aps_algorithms"] == ["APS_ALGO_TURBO_HDR"]
    assert result["mode_tokens"] == ["Master", "Photo", "Underwater", "Video"]

def test_does_not_promote_generic_camera_words():
    result = extract_text("camera HDR photo video feature")
    assert result["compile_symbols"] == []
    assert result["imaging_symbols"] == []
    assert result["vendor_tags"] == []
    assert result["aps_algorithms"] == []

def test_official_catalog_has_no_unsourced_feature():
    data = load_catalog()
    source_ids = set(data["sources"])
    assert data["features"]
    assert all(row["sources"] for row in data["features"])
    for row in data["features"]:
        assert set(row["sources"]) <= source_ids
    assert data["audit"]["entries_without_sources"] == []

def test_catalog_ids_and_symbols_are_unique():
    data = load_catalog()
    feature_ids = [row["id"] for row in data["features"]]
    symbols = [row["symbol"] for row in data["official_kernel_feature_macros"]]
    assert len(feature_ids) == len(set(feature_ids))
    assert len(symbols) == len(set(symbols))

def test_catalog_audit_counts_are_not_stale():
    data = load_catalog()
    audit = data["audit"]

    category_counts = Counter(row["category"] for row in data["features"])
    brand_counts = Counter(
        brand
        for row in data["features"]
        for brand in row["brands"]
    )

    assert audit["official_feature_count"] == len(data["features"])
    assert audit["official_kernel_macro_count"] == len(data["official_kernel_feature_macros"])
    assert audit["source_count"] == len(data["sources"])
    assert audit["category_counts"] == dict(sorted(category_counts.items()))
    assert audit["brand_feature_memberships"] == dict(sorted(brand_counts.items()))
