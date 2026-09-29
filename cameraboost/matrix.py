from __future__ import annotations

from typing import Any

def _bucket(path: str) -> str:
    p = path.lower()
    if any(x in p for x in ("sensor", "ois", "eeprom", "actuator")):
        return "Sensor / optics"
    if any(x in p for x in ("isp", "vendor", "hal", "sat")):
        return "HAL / ISP"
    if any(x in p for x in ("video", "4k", "120", "dolby", "10bit", "eis")):
        return "Video"
    if any(x in p for x in ("hdr", "mfnr", "night", "fusion", "bokeh", "sr", "deblur", "upscale", "raw")):
        return "APS algorithms"
    return "Other"

def markdown_matrix(report: dict[str, Any]) -> str:
    rows: list[tuple[str, str, str, str, str]] = []
    for filename, entry in report.get("files", {}).items():
        for change in entry.get("changes", []):
            if not change.get("camera_relevant"):
                continue
            rows.append((
                _bucket(change["path"]),
                filename,
                change["path"],
                repr(change.get("left")),
                repr(change.get("right")),
            ))

    out = [
        "# CameraBoost Feature Matrix",
        "",
        "| Layer | File | Key | Target | Donor |",
        "|---|---|---|---|---|",
    ]
    for layer, filename, key, left, right in rows:
        esc = lambda s: s.replace("|", "\\|").replace("\n", " ")
        out.append(f"| {esc(layer)} | `{esc(filename)}` | `{esc(key)}` | `{esc(left)}` | `{esc(right)}` |")

    if not rows:
        out.append("| — | — | No camera-relevant differences detected | — | — |")

    out += [
        "",
        "> A difference is evidence of a configuration difference, not proof that enabling the donor value is safe.",
    ]
    return "\n".join(out) + "\n"
