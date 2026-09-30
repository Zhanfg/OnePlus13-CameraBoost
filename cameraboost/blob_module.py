from __future__ import annotations

import hashlib
import os
import re
import zipfile
from collections import defaultdict
from pathlib import PurePosixPath
from typing import Any

STABLE_DIFFUSION_CORE = {
    "unet.bix",
    "vae_decoder.bix",
    "vae_decoder_vertical.bix",
    "vae_encoder.bix",
    "vae_encoder_vertical.bix",
}

TELE2_RE = re.compile(r"(?:tele2|(?:^|_)6x(?:_|$))", re.IGNORECASE)
ULTRATELE_RE = re.compile(r"ultra.?tele", re.IGNORECASE)
ASCII_STRING_RE = re.compile(rb"[ -~]{6,}")
ABS_RUNTIME_REF_RE = re.compile(
    r"^/(?:odm|vendor|system(?:_ext)?|product)/.+",
    re.IGNORECASE,
)
REL_MODEL_REF_RE = re.compile(
    r"^(?:highmagsol_\d+x|gan_enhancer_\d+x(?:_sd)?)\.bin$",
    re.IGNORECASE,
)
RUNTIME_FILE_SUFFIXES = {
    ".so", ".json", ".bin", ".bix", ".dlc", ".txt", ".xml", ".ini",
    ".cfg", ".conf", ".model", ".data", ".license", ".png", ".bmp",
    ".vcfg", ".tflite", ".onnx",
}

TEXT_SUFFIXES = {".json", ".ini", ".sh", ".prop", ".txt", ".xml", ".cfg", ".conf"}


def _parse_prop(text: str) -> dict[str, str]:
    out: dict[str, str] = {}
    for raw in text.splitlines():
        line = raw.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        out[key.strip()] = value.strip()
    return out


def _bucket(filename: str) -> str:
    path = PurePosixPath(filename)
    name = path.name.lower()

    if filename.startswith("odm/etc/camera/hybridraw_models/"):
        if name in STABLE_DIFFUSION_CORE:
            return "stable_diffusion_sr_core"
        if TELE2_RE.search(name):
            return "tele2_6x_specific"
        if any(x in name for x in ("60x", "80x", "90x", "40x", "20x", "10x", "highmagsol", "gan_enhancer")):
            return "high_zoom_sr"
        if "tele1" in name or name.startswith("tele") or "_tele_" in name:
            return "tele_3x_general"
        if name.startswith("uw_") or "photo_uw" in name:
            return "ultrawide"
        if name.startswith("front_") or "front_main" in name:
            return "front"
        if name.startswith("photo_main") or name.startswith("main_"):
            return "main"
        return "hybridraw_general"

    if filename.startswith("odm/etc/camera/basictone/"):
        return "basictone"
    if filename.startswith("odm/etc/camera/aigc/"):
        return "aigc"
    if filename.startswith("odm/etc/camera/oplus_seg_sdk/"):
        return "segmentation"
    if filename.startswith("odm/etc/camera/AIAE_Models/"):
        if ULTRATELE_RE.search(name):
            return "ultratele_video_model"
        return "aiae_video"
    if filename.startswith("odm/etc/camera/"):
        return "camera_misc"
    if filename.startswith("odm/lib64/"):
        return "odm_lib64"
    if filename.startswith("common/"):
        return "installer_common"
    return "other"


def _runtime_reference_candidates(data: bytes) -> set[str]:
    refs: set[str] = set()

    for match in ASCII_STRING_RE.finditer(data):
        text = match.group(0).decode("ascii", "ignore").strip()
        if not text or len(text) > 512:
            continue

        # Search-path strings often contain semicolon-delimited alternatives.
        for candidate in text.split(";"):
            candidate = candidate.strip().rstrip("),:;")
            if not candidate or "%" in candidate:
                continue

            if REL_MODEL_REF_RE.match(candidate):
                refs.add(candidate)
                continue

            if not ABS_RUNTIME_REF_RE.match(candidate):
                continue

            suffix = PurePosixPath(candidate).suffix.lower()
            basename = PurePosixPath(candidate).name
            if suffix in RUNTIME_FILE_SUFFIXES:
                refs.add(candidate)
            elif "/config/" in candidate.lower() and basename:
                refs.add(candidate)

    return refs


def _runtime_reference_closure(zf: zipfile.ZipFile, infos: list[zipfile.ZipInfo]) -> dict[str, Any]:
    packaged = {info.filename for info in infos}
    by_ref: dict[str, set[str]] = defaultdict(set)

    for info in infos:
        if not info.filename.endswith(".so"):
            continue
        # The addon contains reasonably sized camera libraries. Avoid scanning
        # unexpectedly giant binary payloads in a generic module.
        if info.file_size > 128 * 1024 * 1024:
            continue

        data = zf.read(info)
        for ref in _runtime_reference_candidates(data):
            by_ref[ref].add(PurePosixPath(info.filename).name)

    rows: list[dict[str, Any]] = []
    for ref, libraries in sorted(by_ref.items()):
        if ref.startswith("/"):
            packaged_path = ref.lstrip("/")
            present = packaged_path in packaged
        else:
            # Relative model references are resolved by basename search because
            # tuning libraries may omit the directory.
            matches = [name for name in packaged if PurePosixPath(name).name == ref]
            present = bool(matches)
            packaged_path = matches[0] if matches else None

        rows.append({
            "reference": ref,
            "packaged": present,
            "packaged_path": packaged_path,
            "referenced_by": sorted(libraries),
        })

    missing = [row for row in rows if not row["packaged"]]
    return {
        "scanned_shared_libraries": sum(1 for i in infos if i.filename.endswith(".so")),
        "references": rows,
        "referenced_not_packaged": missing,
        "referenced_not_packaged_count": len(missing),
        "warning": (
            "A binary string reference is evidence of a runtime/plugin/config lookup surface, "
            "not proof that the referenced file is mandatory for every camera mode. "
            "Resolve feature-selection paths before treating a missing reference as a hard dependency."
        ),
    }


def analyze_zip(path: str) -> dict[str, Any]:
    sha = hashlib.sha256()
    with open(path, "rb") as source:
        for chunk in iter(lambda: source.read(8 * 1024 * 1024), b""):
            sha.update(chunk)

    with zipfile.ZipFile(path) as zf:
        infos = [i for i in zf.infolist() if not i.is_dir()]
        total_uncompressed = sum(i.file_size for i in infos)
        total_compressed = sum(i.compress_size for i in infos)

        module_prop: dict[str, str] = {}
        if "module.prop" in zf.namelist():
            module_prop = _parse_prop(zf.read("module.prop").decode("utf-8", "replace"))

        buckets: dict[str, dict[str, int]] = defaultdict(lambda: {"files": 0, "uncompressed": 0, "compressed": 0})
        for info in infos:
            row = buckets[_bucket(info.filename)]
            row["files"] += 1
            row["uncompressed"] += info.file_size
            row["compressed"] += info.compress_size

        text_hits: dict[str, list[str]] = defaultdict(list)
        for info in infos:
            suffix = PurePosixPath(info.filename).suffix.lower()
            if suffix not in TEXT_SUFFIXES or info.file_size > 2 * 1024 * 1024:
                continue
            text = zf.read(info).decode("utf-8", "replace")
            for token in ("23821", "8750", "utele", "Camera4", "P010", "HEIC", "HEIF", "Dolby", "TurboHDR", "HybridRaw"):
                if token.lower() in text.lower():
                    text_hits[token].append(info.filename)

        dependency_line = None
        if "common/install.sh" in zf.namelist():
            install = zf.read("common/install.sh").decode("utf-8", "replace")
            for line in install.splitlines():
                if "um8s_op13_ocvm" in line:
                    dependency_line = {
                        "text": line.strip(),
                        "commented_out": line.lstrip().startswith("#"),
                    }
                    break

        candidate_prune = {
            "stable_diffusion_sr_core",
            "tele2_6x_specific",
            "ultratele_video_model",
        }
        prune_uncompressed = sum(buckets[x]["uncompressed"] for x in candidate_prune)
        prune_compressed = sum(buckets[x]["compressed"] for x in candidate_prune)

        largest = sorted(infos, key=lambda x: x.file_size, reverse=True)[:20]

        return {
            "schema_version": 1,
            "archive": {
                "sha256": sha.hexdigest(),
                "entries": len(infos),
                "archive_bytes": os.path.getsize(path),
                "compressed_payload_bytes": total_compressed,
                "uncompressed_bytes": total_uncompressed,
            },
            "module": module_prop,
            "buckets": dict(sorted(buckets.items())),
            "largest_files": [
                {
                    "path": i.filename,
                    "uncompressed": i.file_size,
                    "compressed": i.compress_size,
                }
                for i in largest
            ],
            "text_signal_files": {k: sorted(v) for k, v in sorted(text_hits.items())},
            "intended_main_module_dependency": dependency_line,
            "runtime_reference_closure": _runtime_reference_closure(zf, infos),
            "candidate_prune": {
                "buckets": sorted(candidate_prune),
                "uncompressed_bytes": prune_uncompressed,
                "compressed_bytes": prune_compressed,
                "estimated_remaining_compressed_payload_bytes": total_compressed - prune_compressed,
                "warning": "Filename-based candidate only; verify runtime/model dependency closure before deleting assets.",
            },
        }
