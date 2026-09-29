import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

def load(rel):
    return json.loads((ROOT / rel).read_text(encoding="utf-8"))

def test_p0_plan_covers_every_p0_exactly_once():
    profile = load("profiles/oneplus13-compatibility.json")
    plan = load("profiles/oneplus13-p0-plan.json")

    expected = {r["id"] for r in profile["entries"] if r["priority"] == "P0"}
    listed = [
        feature_id
        for workstream in plan["workstreams"]
        for feature_id in workstream["feature_ids"]
    ]

    assert len(listed) == len(set(listed))
    assert set(listed) == expected
    assert plan["audit"]["missing_p0"] == []
    assert plan["audit"]["non_p0_in_plan"] == []

def test_p0_workstreams_define_layers_and_evidence_gates():
    plan = load("profiles/oneplus13-p0-plan.json")
    assert plan["workstreams"]
    for row in plan["workstreams"]:
        assert row["primary_layers"]
        assert len(row["evidence_gate"]) > 30
        assert row["donor_refs"]
