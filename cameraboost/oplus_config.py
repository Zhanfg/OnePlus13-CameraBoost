from __future__ import annotations

from dataclasses import asdict, dataclass
from typing import Any, Iterable


@dataclass(frozen=True)
class ConfigEntry:
    identity: str
    identity_field: str
    value: str | None
    type: str | None
    count: str | None


def _extract_string_field(block: str, field: str) -> str | None:
    marker = f'"{field}"'
    pos = block.find(marker)
    if pos < 0:
        return None
    colon = block.find(":", pos + len(marker))
    if colon < 0:
        return None
    quote = block.find('"', colon + 1)
    if quote < 0:
        return None

    out: list[str] = []
    escaped = False
    for ch in block[quote + 1 :]:
        if escaped:
            out.append(ch)
            escaped = False
        elif ch == "\\":
            out.append(ch)
            escaped = True
        elif ch == '"':
            return "".join(out)
        else:
            out.append(ch)
    return None


def _object_blocks(text: str) -> Iterable[str]:
    depth = 0
    start: int | None = None
    in_string = False
    escaped = False

    for index, ch in enumerate(text):
        if in_string:
            if escaped:
                escaped = False
            elif ch == "\\":
                escaped = True
            elif ch == '"':
                in_string = False
            continue

        if ch == '"':
            in_string = True
        elif ch == "{":
            if depth == 0:
                start = index
            depth += 1
        elif ch == "}" and depth:
            depth -= 1
            if depth == 0 and start is not None:
                yield text[start : index + 1]
                start = None


def _normalize(value: str | None) -> str | None:
    if value is None:
        return None
    return " ".join(value.split())


def parse_keyed_config(text: str) -> dict[str, ConfigEntry]:
    """Parse OPlus Key/VendorTag pseudo-JSON without requiring valid JSON."""
    entries: dict[str, ConfigEntry] = {}
    for block in _object_blocks(text):
        identity_field = ""
        identity = None
        for field in ("Key", "VendorTag"):
            value = _extract_string_field(block, field)
            if value is not None:
                identity_field = field
                identity = value.strip()
                break
        if not identity:
            continue
        entries[identity] = ConfigEntry(
            identity=identity,
            identity_field=identity_field,
            value=_normalize(_extract_string_field(block, "Value")),
            type=_normalize(_extract_string_field(block, "Type")),
            count=_normalize(_extract_string_field(block, "Count")),
        )
    return entries


def parse_sections(text: str) -> dict[str, str]:
    sections: dict[str, list[str]] = {"__preamble__": []}
    current = "__preamble__"
    for raw in text.splitlines():
        line = raw.strip()
        if line.startswith("[") and line.endswith("]") and len(line) > 2:
            current = line[1:-1].strip()
            sections.setdefault(current, [])
        else:
            sections.setdefault(current, []).append(raw)
    return {name: "\n".join(lines).strip() for name, lines in sections.items()}


def diff_keyed_configs(left: str, right: str) -> dict[str, Any]:
    a = parse_keyed_config(left)
    b = parse_keyed_config(right)
    changed = []
    for key in sorted(set(a) & set(b)):
        if a[key] != b[key]:
            changed.append({"key": key, "right": asdict(b[key]), "left": asdict(a[key])})
    return {
        "kind": "keyed",
        "left_entries": len(a),
        "right_entries": len(b),
        "added": sorted(set(a) - set(b)),
        "removed": sorted(set(b) - set(a)),
        "changed": changed,
    }


def diff_sections(left: str, right: str) -> dict[str, Any]:
    a = parse_sections(left)
    b = parse_sections(right)
    changed = []
    for key in sorted(set(a) & set(b)):
        if a[key] != b[key]:
            changed.append({"section": key, "right": b[key], "left": a[key]})
    return {
        "kind": "sections",
        "left_sections": len(a),
        "right_sections": len(b),
        "added": sorted(set(a) - set(b)),
        "removed": sorted(set(b) - set(a)),
        "changed": changed,
    }


def summarize_diff(report: dict[str, Any]) -> dict[str, Any]:
    changed = report.get("changed", [])
    return {
        "kind": report["kind"],
        "left_count": report.get("left_entries", report.get("left_sections")),
        "right_count": report.get("right_entries", report.get("right_sections")),
        "added_count": len(report.get("added", [])),
        "removed_count": len(report.get("removed", [])),
        "changed_count": len(changed),
        "added": report.get("added", []),
        "removed": report.get("removed", []),
        "changed": [row.get("key", row.get("section")) for row in changed],
    }


def auto_diff(left: str, right: str, kind: str = "auto") -> dict[str, Any]:
    if kind == "keyed":
        return diff_keyed_configs(left, right)
    if kind == "sections":
        return diff_sections(left, right)
    if kind != "auto":
        raise ValueError(f"unsupported config kind: {kind}")

    sample = left + "\n" + right
    if '"Key"' in sample or '"VendorTag"' in sample:
        return diff_keyed_configs(left, right)
    return diff_sections(left, right)
