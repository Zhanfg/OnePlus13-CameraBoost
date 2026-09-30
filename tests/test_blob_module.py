import zipfile

from cameraboost.blob_module import analyze_zip


def test_blob_audit_detects_module_and_candidate_buckets(tmp_path):
    archive = tmp_path / "module.zip"
    with zipfile.ZipFile(archive, "w") as zf:
        zf.writestr(
            "module.prop",
            "id=test_addon\nname=Test\nversion=1\nauthor=Example\n",
        )
        zf.writestr(
            "common/install.sh",
            "# ! [ -d /data/adb/modules/um8s_op13_ocvm ] && abort missing\n",
        )
        zf.writestr("odm/etc/camera/hybridraw_models/unet.bix", b"x" * 100)
        zf.writestr("odm/etc/camera/hybridraw_models/photo_tele2_binning_ev0_6x.bin", b"y" * 80)
        zf.writestr("odm/etc/camera/AIAE_Models/AIAEVideoModelUltraTele.bin", b"z" * 40)
        zf.writestr(
            "odm/etc/camera/target.json",
            '{"project":"23821","product":"8750","camera_name":"utele","fmt":"P010"}',
        )
        zf.writestr(
            "odm/lib64/libDecision.so",
            (
                b"prefix\x00/odm/lib64/libapsultrahdr.so\x00"
                b"/odm/etc/camera/config/oplus_camera_preview_decision_config.json\x00"
                b"highmagsol_20x.bin\x00"
            ),
        )

    result = analyze_zip(str(archive))

    assert result["module"]["id"] == "test_addon"
    assert result["buckets"]["stable_diffusion_sr_core"]["files"] == 1
    assert result["buckets"]["tele2_6x_specific"]["files"] == 1
    assert result["buckets"]["ultratele_video_model"]["files"] == 1
    assert result["intended_main_module_dependency"]["commented_out"] is True
    assert "odm/etc/camera/target.json" in result["text_signal_files"]["23821"]
    assert result["candidate_prune"]["compressed_bytes"] > 0

    missing = {
        row["reference"]: row
        for row in result["runtime_reference_closure"]["referenced_not_packaged"]
    }
    assert "/odm/lib64/libapsultrahdr.so" in missing
    assert "/odm/etc/camera/config/oplus_camera_preview_decision_config.json" in missing
    assert "highmagsol_20x.bin" in missing
    assert missing["/odm/lib64/libapsultrahdr.so"]["referenced_by"] == ["libDecision.so"]


def test_runtime_reference_is_not_missing_when_packaged(tmp_path):
    archive = tmp_path / "module.zip"
    with zipfile.ZipFile(archive, "w") as zf:
        zf.writestr("odm/lib64/libDecision.so", b"/odm/lib64/libPlugin.so\x00")
        zf.writestr("odm/lib64/libPlugin.so", b"plugin")

    result = analyze_zip(str(archive))
    refs = {
        row["reference"]: row
        for row in result["runtime_reference_closure"]["references"]
    }
    assert refs["/odm/lib64/libPlugin.so"]["packaged"] is True
