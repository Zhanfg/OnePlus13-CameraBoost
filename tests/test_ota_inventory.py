import json
import zipfile
from pathlib import Path

from cameraboost.ota_inventory import classify_path, index_source


def test_classifies_camera_color_and_heif_surfaces():
    assert "camera_aps_config" in classify_path(
        "vendor/etc/camera/config/oplus_camera_config.json"
    )
    assert "camera_hal" in classify_path(
        "vendor/lib64/hw/camera.qcom.so"
    )
    assert "heif_codec" in classify_path(
        "vendor/etc/media_codecs_vendor.xml"
    )
    assert "color_metadata" in classify_path(
        "vendor/etc/display/rec2020_hdr_color.xml"
    )
    assert "gallery_media" in classify_path(
        "product/app/OplusGallery/OplusGallery.apk"
    )


def test_indexes_extracted_official_tree_conservatively(tmp_path):
    config = tmp_path / "vendor/etc/camera/config/oplus_camera_config.json"
    config.parent.mkdir(parents=True)
    config.write_text(
        """
        {
          "feature": "OPLUS_FEATURE_10BIT_HEIF",
          "format": "YCBCR_P010",
          "container": "HEIC",
          "color": "Rec.2020"
        }
        """,
        encoding="utf-8",
    )

    codec = tmp_path / "vendor/etc/media_codecs_vendor.xml"
    codec.parent.mkdir(parents=True, exist_ok=True)
    codec.write_text("<MediaCodec name=\"c2.qti.heic.encoder\" />", encoding="utf-8")

    hal = tmp_path / "vendor/lib64/hw/camera.qcom.so"
    hal.parent.mkdir(parents=True, exist_ok=True)
    hal.write_bytes(b"binary-camera-hal")

    payload = tmp_path / "payload.bin"
    payload.write_bytes(b"small-placeholder")

    result = index_source(tmp_path, max_hash_bytes=1024)
    assert result["summary"]["needs_partition_materialization"] is True

    by_path = {row["path"]: row for row in result["records"]}
    cfg = by_path["vendor/etc/camera/config/oplus_camera_config.json"]
    assert "camera_aps_config" in cfg["layers"]
    assert "imaging_symbols" in cfg["text_signals"]
    assert "android_10bit_tokens" in cfg["text_signals"]
    assert cfg["sha256"]

    assert "camera_hal" in by_path["vendor/lib64/hw/camera.qcom.so"]["layers"]
    assert by_path["payload.bin"]["container"] is True


def test_indexes_zip_without_extracting_binary_payload(tmp_path):
    archive = tmp_path / "official.zip"
    with zipfile.ZipFile(archive, "w") as zf:
        zf.writestr("payload.bin", b"x" * 2048)
        zf.writestr(
            "vendor/etc/media_codecs_vendor.xml",
            "<MediaCodec name=\"c2.qti.heic.encoder\" />",
        )
        zf.writestr(
            "vendor/etc/camera/config/features.xml",
            "OPLUS_FEATURE_10BIT_HEIF Rec.2020 YCBCR_P010",
        )

    result = index_source(
        archive,
        max_text_bytes=1024,
        max_hash_bytes=1024,
    )
    by_path = {row["path"]: row for row in result["records"]}

    assert result["source_type"] == "zip"
    assert result["summary"]["needs_partition_materialization"] is True
    assert by_path["payload.bin"]["hash_status"] == "skipped-large"
    assert by_path["vendor/etc/camera/config/features.xml"]["text_signals"]


def test_inventory_is_json_serializable(tmp_path):
    f = tmp_path / "vendor/etc/media_codecs.xml"
    f.parent.mkdir(parents=True)
    f.write_text("HEIF", encoding="utf-8")
    result = index_source(tmp_path)
    json.dumps(result)
