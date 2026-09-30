from __future__ import annotations

import hashlib
import json
import zipfile
from dataclasses import dataclass, asdict
from pathlib import Path
from typing import Iterable

from .official_features import extract_text

TEXT_EXTENSIONS = {
    ".xml", ".json", ".txt", ".prop", ".conf", ".cfg", ".mk", ".rc",
    ".ini", ".yaml", ".yml", ".csv", ".toml",
}

CONTAINER_NAMES = {
    "payload.bin",
    "super.img",
    "vendor.img",
    "odm.img",
    "system.img",
    "system_ext.img",
    "product.img",
}

LAYER_PATTERNS = {
    "system_features": (
        "oplus_native_features",
        "build.prop",
        "vendor.prop",
        "product.prop",
        "/etc/permissions/",
        "/etc/sysconfig/",
    ),
    "camera_hal": (
        "camera.provider",
        "camera-provider",
        "/hw/camera.",
        "/hw/camera_",
        "camera.qcom",
        "camera.oplus",
        "camera.default",
        "android.hardware.camera.provider",
        "cameraserver",
        "camera_service",
    ),
    "camera_aps_config": (
        "oplus_camera_config",
        "/camera/config/",
        "/camera/",
        "camx",
        "chromatix",
        "/chi/",
        "aps",
        "camera_config",
        "sensor_mode",
        "sensormode",
    ),
    "heif_codec": (
        "heif",
        "heic",
        "media_codecs",
        "codec2",
        "/c2.",
        "imagecodec",
        "hevc",
    ),
    "color_metadata": (
        ".icc",
        "cicp",
        "bt2020",
        "bt.2020",
        "rec2020",
        "rec.2020",
        "display_p3",
        "display-p3",
        "dci_p3",
        "dci-p3",
        "dataspace",
        "hdr",
        "color",
    ),
    "gallery_media": (
        "gallery",
        "photos",
        "mediaprovider",
        "media-provider",
        "media.module",
    ),
}

MODEL_EXTENSIONS = {
    ".dlc", ".tflite", ".onnx", ".bin", ".model", ".weights",
}


@dataclass(frozen=True)
class ArtifactRecord:
    path: str
    size: int
    layers: tuple[str, ...]
    sha256: str | None = None
    hash_status: str = "not-requested"
    text_signals: dict | None = None
    container: bool = False
    source_type: str = "directory"

    def to_dict(self) -> dict:
        return asdict(self)


def classify_path(path: str) -> list[str]:
    normalized = "/" + path.replace("\\", "/").lower().lstrip("/")
    layers = [
        layer
        for layer, patterns in LAYER_PATTERNS.items()
        if any(pattern in normalized for pattern in patterns)
    ]

    suffix = Path(path).suffix.lower()
    if suffix in MODEL_EXTENSIONS and (
        "camera_aps_config" in layers
        or "camera" in normalized
        or "aps" in normalized
    ):
        layers.append("camera_ml_model")

    return sorted(set(layers))


def is_container(path: str) -> bool:
    return Path(path).name.lower() in CONTAINER_NAMES


def _hash_bytes(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def _text_candidate(path: str, size: int, max_text_bytes: int) -> bool:
    return size <= max_text_bytes and Path(path).suffix.lower() in TEXT_EXTENSIONS


def _signals_from_bytes(path: str, data: bytes, max_text_bytes: int) -> dict | None:
    if not _text_candidate(path, len(data), max_text_bytes):
        return None
    text = data.decode("utf-8", errors="replace")
    signals = extract_text(text)
    compact = {key: value for key, value in signals.items() if value}
    return compact or None


def _record_from_bytes(
    path: str,
    data: bytes,
    *,
    source_type: str,
    max_text_bytes: int,
    max_hash_bytes: int,
) -> ArtifactRecord | None:
    layers = classify_path(path)
    container = is_container(path)
    if not layers and not container:
        return None

    if len(data) <= max_hash_bytes:
        digest = _hash_bytes(data)
        hash_status = "sha256"
    else:
        digest = None
        hash_status = "skipped-large"

    return ArtifactRecord(
        path=path,
        size=len(data),
        layers=tuple(layers),
        sha256=digest,
        hash_status=hash_status,
        text_signals=_signals_from_bytes(path, data, max_text_bytes),
        container=container,
        source_type=source_type,
    )


def _scan_directory(
    root: Path,
    *,
    max_text_bytes: int,
    max_hash_bytes: int,
) -> list[ArtifactRecord]:
    records: list[ArtifactRecord] = []

    for file_path in sorted(p for p in root.rglob("*") if p.is_file()):
        rel = str(file_path.relative_to(root))
        layers = classify_path(rel)
        container = is_container(rel)
        if not layers and not container:
            continue

        size = file_path.stat().st_size
        data: bytes | None = None
        if size <= max(max_text_bytes, max_hash_bytes):
            data = file_path.read_bytes()

        if size <= max_hash_bytes:
            if data is None:
                data = file_path.read_bytes()
            digest = _hash_bytes(data)
            hash_status = "sha256"
        else:
            digest = None
            hash_status = "skipped-large"

        signals = None
        if _text_candidate(rel, size, max_text_bytes):
            if data is None:
                data = file_path.read_bytes()
            signals = _signals_from_bytes(rel, data, max_text_bytes)

        records.append(ArtifactRecord(
            path=rel,
            size=size,
            layers=tuple(layers),
            sha256=digest,
            hash_status=hash_status,
            text_signals=signals,
            container=container,
            source_type="directory",
        ))

    return records


def _scan_zip(
    archive: Path,
    *,
    max_text_bytes: int,
    max_hash_bytes: int,
) -> list[ArtifactRecord]:
    records: list[ArtifactRecord] = []

    with zipfile.ZipFile(archive) as zf:
        for info in sorted(zf.infolist(), key=lambda x: x.filename):
            if info.is_dir():
                continue

            path = info.filename
            layers = classify_path(path)
            container = is_container(path)
            if not layers and not container:
                continue

            size = info.file_size
            needs_bytes = (
                size <= max_hash_bytes
                or _text_candidate(path, size, max_text_bytes)
            )
            data = zf.read(info) if needs_bytes else None

            if size <= max_hash_bytes and data is not None:
                digest = _hash_bytes(data)
                hash_status = "sha256"
            else:
                digest = None
                hash_status = "skipped-large"

            signals = None
            if _text_candidate(path, size, max_text_bytes) and data is not None:
                signals = _signals_from_bytes(path, data, max_text_bytes)

            records.append(ArtifactRecord(
                path=path,
                size=size,
                layers=tuple(layers),
                sha256=digest,
                hash_status=hash_status,
                text_signals=signals,
                container=container,
                source_type="zip",
            ))

    return records


def summarize_records(records: Iterable[ArtifactRecord]) -> dict:
    rows = list(records)
    layer_counts: dict[str, int] = {}
    signal_counts: dict[str, int] = {}

    for row in rows:
        for layer in row.layers:
            layer_counts[layer] = layer_counts.get(layer, 0) + 1
        for signal_family, values in (row.text_signals or {}).items():
            signal_counts[signal_family] = signal_counts.get(signal_family, 0) + len(values)

    containers = [row.path for row in rows if row.container]

    return {
        "relevant_records": len(rows),
        "layer_counts": dict(sorted(layer_counts.items())),
        "signal_counts": dict(sorted(signal_counts.items())),
        "container_artifacts": containers,
        "needs_partition_materialization": bool(containers),
    }


def index_source(
    source: str | Path,
    *,
    max_text_bytes: int = 4 * 1024 * 1024,
    max_hash_bytes: int = 8 * 1024 * 1024,
) -> dict:
    path = Path(source)

    if path.is_dir():
        records = _scan_directory(
            path,
            max_text_bytes=max_text_bytes,
            max_hash_bytes=max_hash_bytes,
        )
        source_type = "directory"
    elif path.is_file() and zipfile.is_zipfile(path):
        records = _scan_zip(
            path,
            max_text_bytes=max_text_bytes,
            max_hash_bytes=max_hash_bytes,
        )
        source_type = "zip"
    else:
        raise ValueError("source must be an extracted directory or ZIP archive")

    return {
        "schema_version": 1,
        "source": str(path),
        "source_type": source_type,
        "limits": {
            "max_text_bytes": max_text_bytes,
            "max_hash_bytes": max_hash_bytes,
        },
        "summary": summarize_records(records),
        "records": [row.to_dict() for row in records],
    }
