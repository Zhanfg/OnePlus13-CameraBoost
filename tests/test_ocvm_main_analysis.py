import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def load():
    return json.loads(
        (ROOT / "research/ocvm-main-v61.82.json").read_text(encoding="utf-8")
    )


def test_ocvm_analysis_keeps_capability_and_quality_layers_separate():
    data = load()
    decision = data["cameraboost_architecture_decision"]
    assert "feature gates" in decision["capability_layer"]
    assert "profiles" in decision["quality_layer"]
    assert "mode-aware boosts" in decision["resource_layer"]


def test_ocvm_front_turbohdr_is_marked_experimental():
    data = load()
    front = data["selected_sensor_threshold_changes"]["dodgefront"]
    assert front["TURBO_HDR_RAW_WIDTH"] == ["3280", "6560"]
    assert front["TURBO_HDR_RAW_HEIGHT"] == ["2464", "4928"]
    assert front["MFNR_SUPPORT"] == ["1", "0"]


def test_ocvm_resource_policy_does_not_adopt_unbounded_overrides():
    data = load()
    risks = " ".join(data["performance_and_thermal_risks"]).lower()
    assert "0xffffffff" in risks
    assert "mode-aware resource governor" in risks
