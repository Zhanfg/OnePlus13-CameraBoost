from cameraboost.tenbit import (
    TAG_10BIT_HEIC,
    inspect_oplus_tenbit_flags,
    tenbit_photo_gate_enabled,
)
from tools.verify_10bit import classify_ffprobe

def test_detects_oplus_10bit_heic_gate():
    config = {
        "file_version": 1,
        "file_data": [
            {
                "VendorTag": TAG_10BIT_HEIC,
                "Type": "Byte",
                "Count": "1",
                "Value": "1",
            }
        ],
    }
    flags = inspect_oplus_tenbit_flags(config)
    assert flags[TAG_10BIT_HEIC].present is True
    assert flags[TAG_10BIT_HEIC].value == "1"
    assert tenbit_photo_gate_enabled(config) is True

def test_ffprobe_verifier_accepts_10bit_pixel_format():
    result = classify_ffprobe({
        "streams": [{
            "codec_name": "hevc",
            "pix_fmt": "yuv420p10le",
            "bits_per_raw_sample": "10",
        }]
    })
    assert result["verified_10bit"] is True
    assert result["status"] == "verified-10bit"

def test_ffprobe_verifier_rejects_8bit_pixel_format():
    result = classify_ffprobe({
        "streams": [{
            "codec_name": "hevc",
            "pix_fmt": "yuv420p",
            "bits_per_raw_sample": "8",
        }]
    })
    assert result["verified_10bit"] is False
