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
    rows = select_entries(
        profile,
        status=args.status,
        priority=args.priority,
        category=args.category,
    )
    if args.summary:
        print(json.dumps(summarize(profile), ensure_ascii=False, indent=2))
        return 0
    print(json.dumps(rows, ensure_ascii=False, indent=2))
    return 0
\ndef build_parser() -> argparse.ArgumentParser:
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
    return p

def main() -> int:
    args = build_parser().parse_args()
    return args.func(args)

if __name__ == "__main__":
    raise SystemExit(main())
