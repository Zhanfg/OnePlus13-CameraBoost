from __future__ import annotations

from dataclasses import dataclass
from typing import Any

TAG_10BIT_HEIC = "com.oplus.10bits.heic.encode.support"
TAG_HEIF_LIVE_PHOTO = "com.oplus.camera.heif.support.livephoto"
TAG_10BIT_LIVE_PHOTO = "com.oplus.livephoto.support.10bit"

TARGET_TAGS = (
    TAG_10BIT_HEIC,
    TAG_HEIF_LIVE_PHOTO,
    TAG_10BIT_LIVE_PHOTO,
)

@dataclass(frozen=True)
class VendorFlag:
    tag: str
    present: bool
    value: str | None
    type: str | None = None
    count: str | None = None

def _array_from_config(config: Any) -> list[Any]:
    if isinstance(config, list):
        return config
    if isinstance(config, dict) and isinstance(config.get("file_data"), list):
        return config["file_data"]
    return []

def inspect_oplus_tenbit_flags(config: Any) -> dict[str, VendorFlag]:
    rows = _array_from_config(config)
    found: dict[str, VendorFlag] = {
        tag: VendorFlag(tag=tag, present=False, value=None)
        for tag in TARGET_TAGS
    }

    for row in rows:
        if not isinstance(row, dict):
            continue
        tag = row.get("VendorTag")
        if tag not in found:
            continue
        found[tag] = VendorFlag(
            tag=tag,
            present=True,
            value=str(row.get("Value")) if row.get("Value") is not None else None,
            type=str(row.get("Type")) if row.get("Type") is not None else None,
            count=str(row.get("Count")) if row.get("Count") is not None else None,
        )

    return found

def tenbit_photo_gate_enabled(config: Any) -> bool:
    flag = inspect_oplus_tenbit_flags(config)[TAG_10BIT_HEIC]
    return flag.present and flag.value == "1"
