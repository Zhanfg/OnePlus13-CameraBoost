from __future__ import annotations

import json
from dataclasses import dataclass, asdict
from pathlib import Path
from typing import Any

CAMERA_HINTS = (
    "hdr", "mfnr", "night", "super", "portrait", "bokeh", "fusion",
    "zoom", "sat", "raw", "video", "4k", "120", "10bit", "dolby",
    "eis", "ois", "sr", "deblur", "upscale", "sensor", "isp",
    "vendor", "lux", "frame", "algorithm", "algo", "capture",
)

@dataclass(frozen=True)
class Change:
    path: str
    kind: str
    left: Any = None
    right: Any = None
    camera_relevant: bool = False

    def to_dict(self) -> dict[str, Any]:
        return asdict(self)

def load_json(path: Path) -> Any:
    with path.open("r", encoding="utf-8") as f:
        return json.load(f)

def flatten(value: Any, prefix: str = "") -> dict[str, Any]:
    out: dict[str, Any] = {}

    def walk(v: Any, p: str) -> None:
        if isinstance(v, dict):
            for key in sorted(v):
                child = f"{p}.{key}" if p else str(key)
                walk(v[key], child)
        elif isinstance(v, list):
            for i, item in enumerate(v):
                child = f"{p}[{i}]"
                walk(item, child)
        else:
            out[p or "$"] = v

    walk(value, prefix)
    return out

def is_camera_relevant(path: str) -> bool:
    low = path.lower()
    return any(h in low for h in CAMERA_HINTS)

def diff_values(left: Any, right: Any) -> list[Change]:
    a = flatten(left)
    b = flatten(right)
    changes: list[Change] = []

    for key in sorted(set(a) | set(b)):
        if key not in a:
            changes.append(Change(key, "added", None, b[key], is_camera_relevant(key)))
        elif key not in b:
            changes.append(Change(key, "removed", a[key], None, is_camera_relevant(key)))
        elif a[key] != b[key]:
            changes.append(Change(key, "changed", a[key], b[key], is_camera_relevant(key)))
    return changes

def discover_json_files(root: Path) -> dict[str, Path]:
    return {str(p.relative_to(root)): p for p in sorted(root.rglob("*.json"))}

def diff_directories(left_root: Path, right_root: Path) -> dict[str, Any]:
    left_files = discover_json_files(left_root)
    right_files = discover_json_files(right_root)

    report: dict[str, Any] = {
        "left_root": str(left_root),
        "right_root": str(right_root),
        "files": {},
        "summary": {
            "only_left": 0,
            "only_right": 0,
            "matched": 0,
            "changes": 0,
            "camera_relevant_changes": 0,
        },
    }

    for rel in sorted(set(left_files) | set(right_files)):
        if rel not in left_files:
            report["files"][rel] = {"status": "only_right"}
            report["summary"]["only_right"] += 1
            continue
        if rel not in right_files:
            report["files"][rel] = {"status": "only_left"}
            report["summary"]["only_left"] += 1
            continue

        report["summary"]["matched"] += 1
        changes = diff_values(load_json(left_files[rel]), load_json(right_files[rel]))
        encoded = [c.to_dict() for c in changes]
        report["files"][rel] = {"status": "matched", "changes": encoded}
        report["summary"]["changes"] += len(encoded)
        report["summary"]["camera_relevant_changes"] += sum(
            1 for x in encoded if x["camera_relevant"]
        )

    return report
