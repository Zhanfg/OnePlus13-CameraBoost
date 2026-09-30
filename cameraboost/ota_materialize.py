from __future__ import annotations

from pathlib import Path
from typing import Any

TARGET_PARTITIONS = ("vendor", "odm", "system_ext", "product", "system")

CONTAINER_BASENAMES = {
    "payload.bin": "payload",
    "super.img": "super",
    "vendor.img": "partition",
    "odm.img": "partition",
    "system_ext.img": "partition",
    "product.img": "partition",
    "system.img": "partition",
}


def _basename(path: str) -> str:
    return Path(path).name.lower()


def _container_type(path: str) -> str | None:
    return CONTAINER_BASENAMES.get(_basename(path))


def plan_materialization(index: dict[str, Any]) -> dict[str, Any]:
    records = index.get("records") or []
    containers = [
        row.get("path", "")
        for row in records
        if row.get("container") or _container_type(row.get("path", ""))
    ]
    containers = sorted(set(path for path in containers if path))

    payloads = [p for p in containers if _container_type(p) == "payload"]
    supers = [p for p in containers if _container_type(p) == "super"]
    partition_images = [p for p in containers if _container_type(p) == "partition"]

    steps: list[dict[str, Any]] = []

    if payloads:
        steps.append({
            "id": "extract-payload",
            "input": payloads,
            "tool_role": "payload_extractor",
            "tool_examples": ["payload-dumper-go", "payload_dumper.py"],
            "target_outputs": [f"{p}.img" for p in TARGET_PARTITIONS],
            "execution": "manual",
            "notes": "Extract only the partitions needed for camera/userspace evidence when possible.",
        })

    if supers:
        steps.append({
            "id": "unpack-super",
            "input": supers,
            "tool_role": "dynamic_partition_unpacker",
            "tool_examples": ["lpunpack"],
            "target_outputs": [f"{p}.img" for p in TARGET_PARTITIONS],
            "execution": "manual",
            "notes": "Keep only target logical partitions; do not add images to git.",
        })

    if partition_images or payloads or supers:
        image_inputs = sorted(set(
            partition_images
            + [f"{p}.img" for p in TARGET_PARTITIONS]
        ))
        steps.extend([
            {
                "id": "detect-image-format",
                "input": image_inputs,
                "tool_role": "filesystem_detection",
                "tool_examples": ["file", "fsck.erofs", "debugfs"],
                "execution": "manual-read-only",
                "notes": "Determine sparse/raw and EROFS/ext4 before extraction.",
            },
            {
                "id": "normalize-sparse-if-needed",
                "input": image_inputs,
                "tool_role": "sparse_converter",
                "tool_examples": ["simg2img"],
                "execution": "conditional-manual",
                "notes": "Only run for Android sparse images.",
            },
            {
                "id": "extract-filesystems-read-only",
                "input": image_inputs,
                "tool_role": "filesystem_extractor",
                "tool_examples": ["fsck.erofs --extract", "extract.erofs", "debugfs rdump"],
                "execution": "manual-read-only",
                "target_directories": [f"extracted/{p}" for p in TARGET_PARTITIONS],
                "notes": "Never mount writable and never modify original partition images.",
            },
            {
                "id": "reindex-extracted-tree",
                "input": [f"extracted/{p}" for p in TARGET_PARTITIONS],
                "tool_role": "cameraboost_ota_index",
                "tool_examples": ["cameraboost ota-index extracted/"],
                "execution": "local",
                "notes": "Generate derived evidence index only.",
            },
        ])
    else:
        steps.append({
            "id": "reindex-extracted-tree",
            "input": [index.get("source", "<extracted-tree>")],
            "tool_role": "cameraboost_ota_index",
            "tool_examples": ["cameraboost ota-index <extracted-tree>"],
            "execution": "local",
            "notes": "Input already appears materialized; index relevant userspace surfaces directly.",
        })

    return {
        "schema_version": 1,
        "source": index.get("source"),
        "source_type": index.get("source_type"),
        "auto_execute": False,
        "containers_detected": containers,
        "target_partitions": list(TARGET_PARTITIONS),
        "steps": steps,
        "safety": {
            "flash_device": False,
            "writable_mounts": False,
            "automatic_tool_download": False,
            "commit_partition_images": False,
            "commit_proprietary_blobs": False,
        },
    }
