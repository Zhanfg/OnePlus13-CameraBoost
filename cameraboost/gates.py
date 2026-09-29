from __future__ import annotations

from dataclasses import dataclass, asdict
from typing import Any

STAGES = (
    "discovered",
    "advertised",
    "callable",
    "streaming",
    "stable",
    "validated",
)

@dataclass(frozen=True)
class FeatureEvidence:
    name: str
    discovered: bool = False
    advertised: bool = False
    callable: bool = False
    streaming: bool = False
    stable: bool = False
    validated: bool = False
    app_gate_observed: bool = False
    aps_entry_observed: bool = False
    hal_tag_observed: bool = False
    sensor_mode_observed: bool = False
    notes: str = ""

def highest_contiguous_stage(e: FeatureEvidence) -> str:
    reached = "unknown"
    for stage in STAGES:
        if not getattr(e, stage):
            break
        reached = stage
    return reached

def required_probe_layer(e: FeatureEvidence) -> str:
    stage = highest_contiguous_stage(e)

    if stage == "unknown":
        return "discovery"
    if stage == "discovered" and not e.advertised:
        return "hal-or-config"
    if stage == "advertised" and not e.callable:
        return "app-or-feature-gate"
    if stage == "callable" and not e.streaming:
        return "vendor-isp-sensor-kernel"
    if stage == "streaming" and not e.stable:
        return "resource-thermal-stability"
    if stage == "stable" and not e.validated:
        return "output-quality-validation"
    return "complete"

def assess(e: FeatureEvidence) -> dict[str, Any]:
    return {
        **asdict(e),
        "stage": highest_contiguous_stage(e),
        "next_probe_layer": required_probe_layer(e),
        "safe_to_auto_enable": bool(e.validated),
    }
