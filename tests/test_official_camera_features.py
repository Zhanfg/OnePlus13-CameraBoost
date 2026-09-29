from tools.extract_official_camera_features import extract_text

def test_extracts_official_camera_identifiers():
    text = """
    OPLUS_FEATURE_CAMERA_SUPERNIGHT=yes
    OPLUS_ARCH_EXTENDS_CAM_TUNING_PARAMS=yes
    CONFIG_OPLUS_CAM_EVENT_REPORT=m
    VendorTag: com.oplus.10bits.heic.encode.support
    APS_ALGO_TURBO_HDR
    Rear: Photo, Video, Master, Underwater
    """
    result = extract_text(text)
    assert "OPLUS_FEATURE_CAMERA_SUPERNIGHT" in result["compile_symbols"]
    assert "OPLUS_ARCH_EXTENDS_CAM_TUNING_PARAMS" in result["compile_symbols"]
    assert "CONFIG_OPLUS_CAM_EVENT_REPORT" in result["compile_symbols"]
    assert result["vendor_tags"] == ["com.oplus.10bits.heic.encode.support"]
    assert result["aps_algorithms"] == ["APS_ALGO_TURBO_HDR"]
    assert result["mode_tokens"] == ["Master", "Photo", "Underwater", "Video"]

def test_does_not_promote_generic_camera_words():
    result = extract_text("camera HDR photo video feature")
    assert result["compile_symbols"] == []
    assert result["vendor_tags"] == []
    assert result["aps_algorithms"] == []
