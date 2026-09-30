import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def load():
    return json.loads(
        (ROOT / "research/fx8u-23821-decision-surface.json").read_text(encoding="utf-8")
    )


def test_23821_surface_has_core_p0_decision_methods():
    data = load()
    methods = set(data["preview_decision_overrides"])
    assert {
        "hqRawParameters",
        "turbohdrParameters",
        "hdrParameters",
        "nightParameters",
        "sensorModeParameters",
        "updateQuickShotParams",
        "switchToMFNR",
    } <= methods


def test_23821_lens_policy_blocks_donor_ultratele():
    data = load()
    blocked = set(data["oneplus13_policy"]["block_by_default"])
    assert "utele / camera_id 4" in blocked
    assert "Tele2 / 6x donor path" in blocked


def test_23821_parameter_families_include_raw_hdr_and_sat():
    data = load()
    raw = set(data["oplus_parameter_surfaces"]["raw_hdr"])
    sat = set(data["oplus_parameter_surfaces"]["zoom_sat"])
    assert "com.oplus.aps.params.turboraw.thermallevel" in raw
    assert "com.oplus.front.uhdr.support" in raw
    assert "com.oplus.aps.sat.snapshot.sensors.mask" in sat
    assert "com.oplus.ultratele.calibration" in sat
