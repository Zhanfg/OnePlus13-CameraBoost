import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def load():
    return json.loads(
        (ROOT / "research/oneplus13-10bit-evidence-closure.json").read_text(
            encoding="utf-8"
        )
    )


def test_static_closure_is_not_mutation_ready():
    data = load()
    assert data["status"] == "not-static-closed"
    assert data["mutation_ready"] is False
    statuses = {row["id"]: row["status"] for row in data["layers"]}
    assert statuses["heic-encoder-present"].startswith("confirmed")
    assert statuses["heic-encoder-10bit-negotiation"] == "unresolved-critical"
    assert statuses["color-metadata"] == "unresolved-critical"
    assert statuses["oplus-camera-aps-runtime-gate"] == "unresolved-critical"


def test_raw10_and_p010_do_not_imply_final_heif():
    data = load()
    non_equiv = set(data["non_equivalences"])
    assert "RAW10 != 10-bit HEIF" in non_equiv
    assert "HEIC encoder presence != 10-bit HEIC negotiation" in non_equiv
    assert "P010 allocator support != target Camera HAL ten-bit still exposure" in non_equiv


def test_p010_hw_encoder_and_heic_encoder_are_recorded_separately():
    data = load()
    layers = {row["id"]: row for row in data["layers"]}
    assert layers["p010-buffer-format"]["status"].startswith("confirmed")
    assert layers["heic-encoder-present"]["status"].startswith("confirmed")
    assert layers["heic-encoder-10bit-negotiation"]["status"].startswith("unresolved")
