from __future__ import annotations

import argparse
import json
from pathlib import Path

from .aps import diff_directories
from .matrix import markdown_matrix


def cmd_diff(args: argparse.Namespace) -> int:
    report = diff_directories(Path(args.left), Path(args.right))
    text = json.dumps(report, ensure_ascii=False, indent=2)
    if args.out:
        Path(args.out).write_text(text + "\n", encoding="utf-8")
    else:
        print(text)
    return 0


def cmd_matrix(args: argparse.Namespace) -> int:
    report = json.loads(Path(args.report).read_text(encoding="utf-8"))
    text = markdown_matrix(report)
    if args.out:
        Path(args.out).write_text(text, encoding="utf-8")
    else:
        print(text)
    return 0


def cmd_compat(args: argparse.Namespace) -> int:
    from .compatibility import load_profile, select_entries, summarize

    profile = load_profile(args.profile)
    if args.summary:
        payload = summarize(profile)
    else:
        payload = select_entries(
            profile,
            status=args.status,
            priority=args.priority,
            category=args.category,
        )
    print(json.dumps(payload, ensure_ascii=False, indent=2))
    return 0


def cmd_color_path(args: argparse.Namespace) -> int:
    from .color_still import assess_official_path, evidence_from_scan

    payload = json.loads(Path(args.scan).read_text(encoding="utf-8"))
    if isinstance(payload, dict):
        records = payload.get("records")
        if records is None:
            records = [payload]
    elif isinstance(payload, list):
        records = payload
    else:
        raise ValueError("scan JSON must be an object or list")

    evidence = evidence_from_scan(records)
    result = assess_official_path(evidence)
    text = json.dumps(result, ensure_ascii=False, indent=2)

    if args.out:
        Path(args.out).write_text(text + "\n", encoding="utf-8")
    else:
        print(text)
    return 0


def cmd_ota_index(args: argparse.Namespace) -> int:
    from .ota_inventory import index_source

    payload = index_source(
        args.source,
        max_text_bytes=args.max_text_bytes,
        max_hash_bytes=args.max_hash_bytes,
    )
    text = json.dumps(payload, ensure_ascii=False, indent=2)
    if args.out:
        Path(args.out).write_text(text + "\n", encoding="utf-8")
    else:
        print(text)
    return 0


def cmd_ota_plan(args: argparse.Namespace) -> int:
    from .ota_materialize import plan_materialization

    index = json.loads(Path(args.index).read_text(encoding="utf-8"))
    payload = plan_materialization(index)
    text = json.dumps(payload, ensure_ascii=False, indent=2)
    if args.out:
        Path(args.out).write_text(text + "\n", encoding="utf-8")
    else:
        print(text)
    return 0


def cmd_blob_audit(args: argparse.Namespace) -> int:
    from .blob_module import analyze_zip

    payload = analyze_zip(str(args.zip))
    text = json.dumps(payload, ensure_ascii=False, indent=2)
    if args.out:
        Path(args.out).write_text(text + "\n", encoding="utf-8")
    else:
        print(text)
    return 0


def build_parser() -> argparse.ArgumentParser:
    p = argparse.ArgumentParser(prog="cameraboost")
    sub = p.add_subparsers(dest="cmd", required=True)

    d = sub.add_parser("diff", help="Diff two directories of decrypted APS JSON dumps")
    d.add_argument("left", help="Target device dump directory")
    d.add_argument("right", help="Donor/reference dump directory")
    d.add_argument("--out")
    d.set_defaults(func=cmd_diff)

    m = sub.add_parser("matrix", help="Render a camera-focused Markdown matrix from a diff report")
    m.add_argument("report")
    m.add_argument("--out")
    m.set_defaults(func=cmd_matrix)

    c = sub.add_parser("compat", help="Query a target compatibility profile")
    c.add_argument("profile")
    c.add_argument("--status")
    c.add_argument("--priority")
    c.add_argument("--category")
    c.add_argument("--summary", action="store_true")
    c.set_defaults(func=cmd_compat)

    cp = sub.add_parser(
        "color-path",
        help="Assess the official-source 10-bit still/color path from an extractor scan",
    )
    cp.add_argument("scan", help="JSON produced by extract_official_camera_features.py")
    cp.add_argument("--out")
    cp.set_defaults(func=cmd_color_path)

    oi = sub.add_parser(
        "ota-index",
        help="Index camera/color/HEIF surfaces in an official OTA/ROM archive or extracted tree",
    )
    oi.add_argument("source", type=Path)
    oi.add_argument("--out")
    oi.add_argument("--max-text-bytes", type=int, default=4 * 1024 * 1024)
    oi.add_argument("--max-hash-bytes", type=int, default=8 * 1024 * 1024)
    oi.set_defaults(func=cmd_ota_index)

    op = sub.add_parser(
        "ota-plan",
        help="Plan read-only materialization of containers found in an official OTA index",
    )
    op.add_argument("index")
    op.add_argument("--out")
    op.set_defaults(func=cmd_ota_plan)

    ba = sub.add_parser(
        "blob-audit",
        help="Create a derived audit of an OPlus camera blob module ZIP without redistributing payloads",
    )
    ba.add_argument("zip", type=Path)
    ba.add_argument("--out")
    ba.set_defaults(func=cmd_blob_audit)

    return p


def main() -> int:
    args = build_parser().parse_args()
    return args.func(args)


if __name__ == "__main__":
    raise SystemExit(main())
