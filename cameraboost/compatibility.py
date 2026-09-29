from __future__ import annotations

import json
from pathlib import Path
from typing import Any

VALID_STATUSES = {
    "native",
    "high-confidence",
    "portable-with-adaptation",
    "hardware-blocked",
    "unknown",
}
VALID_PRIORITIES = {"P0", "P1", "P2", "native", "skip"}

def load_profile(path: str | Path) -> dict[str, Any]:
    return json.loads(Path(path).read_text(encoding="utf-8"))

def select_entries(
    profile: dict[str, Any],
    *,
    status: str | None = None,
    priority: str | None = None,
    category: str | None = None,
) -> list[dict[str, Any]]:
    rows = profile.get("entries", [])
    if status is not None:
        rows = [r for r in rows if r.get("status") == status]
    if priority is not None:
        rows = [r for r in rows if r.get("priority") == priority]
    if category is not None:
        rows = [r for r in rows if r.get("category") == category]
    return rows

def summarize(profile: dict[str, Any]) -> dict[str, Any]:
    by_status: dict[str, int] = {}
    by_priority: dict[str, int] = {}
    for row in profile.get("entries", []):
        by_status[row["status"]] = by_status.get(row["status"], 0) + 1
        by_priority[row["priority"]] = by_priority.get(row["priority"], 0) + 1
    return {
        "total": len(profile.get("entries", [])),
        "by_status": dict(sorted(by_status.items())),
        "by_priority": dict(sorted(by_priority.items())),
    }
