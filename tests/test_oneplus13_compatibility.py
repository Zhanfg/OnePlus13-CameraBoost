import json
from pathlib import Path

from cameraboost.compatibility import VALID_PRIORITIES, VALID_STATUSES, summarize

ROOT = Path(__file__).resolve().parents[1]

def load_json(rel):
    return json.loads((ROOT / rel).read_text(encoding="utf-8"))

def test_oneplus13_matrix_covers_every_official_feature_once():
    catalog = load_json("catalog/official-camera-features.json")
    profile = load_json("profiles/oneplus13-compatibility.json")
    catalog_ids = [row["id"] for row in catalog["features"]]
    profile_ids = [row["id"] for row in profile["entries"]]
    assert len(profile_ids) == len(set(profile_ids))
    assert set(profile_ids) == set(catalog_ids)

def test_oneplus13_matrix_statuses_and_priorities_are_valid():
    profile = load_json("profiles/oneplus13-compatibility.json")
    assert all(row["status"] in VALID_STATUSES for row in profile["entries"])
    assert all(row["priority"] in VALID_PRIORITIES for row in profile["entries"])

def test_hardware_blocked_rows_have_reason_and_skip_priority():
    profile = load_json("profiles/oneplus13-compatibility.json")
    blocked = [r for r in profile["entries"] if r["status"] == "hardware-blocked"]
    assert blocked
    assert all(r["priority"] == "skip" for r in blocked)
    assert all(len(r["reason"]) > 20 for r in blocked)

def test_p0_contains_core_pro_capture_targets():
    profile = load_json("profiles/oneplus13-compatibility.json")
    p0 = {r["id"] for r in profile["entries"] if r["priority"] == "P0"}
    required = {
        "heif_10bit",
        "4k120_video",
        "log_video",
        "raw_max_16bit_50mp",
        "real_time_triple_exposure",
        "ai_telescope_zoom",
    }
    assert required <= p0

def test_summary_totals_match():
    profile = load_json("profiles/oneplus13-compatibility.json")
    result = summarize(profile)
    assert result["total"] == len(profile["entries"])
    assert sum(result["by_status"].values()) == result["total"]
    assert sum(result["by_priority"].values()) == result["total"]
