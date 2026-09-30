import zipfile
from pathlib import Path

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

    result = analyze_zip(str(archive))

    assert result["module"]["id"] == "test_addon"
    assert result["buckets"]["stable_diffusion_sr_core"]["files"] == 1
    assert result["buckets"]["tele2_6x_specific"]["files"] == 1
    assert result["buckets"]["ultratele_video_model"]["files"] == 1
    assert result["intended_main_module_dependency"]["commented_out"] is True
    assert "odm/etc/camera/target.json" in result["text_signal_files"]["23821"]
    assert result["candidate_prune"]["compressed_bytes"] > 0
