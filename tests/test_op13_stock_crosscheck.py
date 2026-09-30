import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def load(rel):
    return json.loads((ROOT / rel).read_text(encoding="utf-8"))


def test_stock_crosscheck_marks_dodgetele2_as_real_target_route():
    data = load("research/op13-stock-slot-crosscheck.json")
    modules = {row["camera_id"]: row["sensor"] for row in data["engineering_mode_modules"]}
    assert modules[2] == "dodgetele2"
    assert data["corrected_policy"]["deletion"].startswith("No tele2/6x/utele")


def test_stock_and_addon_unet_are_different():
    data = load("research/op13-stock-slot-crosscheck.json")
    stock = data["stock_hybridraw_baseline"]["stock_unet"]
    addon = data["stock_hybridraw_baseline"]["addon_unet"]
    assert stock["lfs_sha256"] != addon["sha256"]
    assert stock["size_bytes"] != addon["size_bytes"]


def test_decision_surface_does_not_block_tele2_by_name():
    data = load("research/fx8u-23821-decision-surface.json")
    blocked = " ".join(data["oneplus13_policy"]["block_by_default"]).lower()
    assert "tele2" not in blocked
    preserve = " ".join(data["oneplus13_policy"]["preserve_until_mapped"]).lower()
    assert "tele2" in preserve
