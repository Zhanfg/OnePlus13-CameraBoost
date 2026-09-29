import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

def load(rel):
    return json.loads((ROOT / rel).read_text(encoding="utf-8"))

def test_oss_surface_map_covers_every_p0_workstream():
    plan = load("profiles/oneplus13-p0-plan.json")
    surfaces = load("profiles/oneplus13-official-oss-surfaces.json")
    planned = {row["id"] for row in plan["workstreams"]}
    mapped = set(surfaces["workstream_mapping"])
    assert mapped == planned

def test_oss_surface_map_keeps_kernel_as_diagnosed_layer():
    surfaces = load("profiles/oneplus13-official-oss-surfaces.json")
    color = surfaces["workstream_mapping"]["color-still"]
    master = surfaces["workstream_mapping"]["master-hasselblad"]
    assert color["kernel_relevance"] == "low-initial"
    assert master["kernel_relevance"] == "low"
    assert any("HEIF" in x for x in surfaces["conclusions"])
    assert any("MMRM" in x for x in surfaces["conclusions"])

def test_official_modules_have_build_definitions():
    surfaces = load("profiles/oneplus13-official-oss-surfaces.json")
    modules = {m["id"]: m for m in surfaces["modules"]}
    assert modules["qcom-camera"]["output"] == "camera.ko"
    assert modules["oplus-camera-extension"]["output"] == "camera_extension.ko"
    assert modules["qcom-camera"]["build_definition"].endswith("camera_modules.bzl")
    assert modules["oplus-camera-extension"]["build_definition"].endswith("camera_extension_modules.bzl")
