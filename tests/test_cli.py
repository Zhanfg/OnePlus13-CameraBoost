import json

from cameraboost.cli import build_parser, cmd_color_path


def test_cli_registers_compat_and_color_path():
    parser = build_parser()

    compat = parser.parse_args([
        "compat",
        "profiles/oneplus13-compatibility.json",
        "--summary",
    ])
    assert compat.cmd == "compat"

    color = parser.parse_args([
        "color-path",
        "official-camera-scan.json",
    ])
    assert color.cmd == "color-path"


def test_color_path_cli_reports_next_missing_layer(tmp_path, capsys):
    scan = {
        "schema_version": 1,
        "records": [
            {
                "imaging_symbols": [
                    "OPLUS_FEATURE_10BIT_HEIF",
                    "OPLUS_FEATURE_HEIF_CONVERTER",
                ],
                "android_10bit_tokens": [
                    "ANDROID_REQUEST_AVAILABLE_CAPABILITIES_DYNAMIC_RANGE_TEN_BIT",
                    "ANDROID_REQUEST_AVAILABLE_DYNAMIC_RANGE_PROFILES_MAP",
                    "YCBCR_P010",
                    "HEIC",
                ],
                "color_tokens": ["Rec.2020"],
            }
        ],
    }
    path = tmp_path / "scan.json"
    path.write_text(json.dumps(scan), encoding="utf-8")

    args = build_parser().parse_args(["color-path", str(path)])
    assert cmd_color_path(args) == 0

    payload = json.loads(capsys.readouterr().out)
    assert payload["hal_contract_complete"] is True
    assert payload["next_layer"] == "heif-encoder"


def test_cli_registers_ota_index():
    parser = build_parser()
    args = parser.parse_args(["ota-index", "official.zip"])
    assert args.cmd == "ota-index"


def test_cli_registers_ota_plan():
    parser = build_parser()
    args = parser.parse_args(["ota-plan", "ota-index.json"])
    assert args.cmd == "ota-plan"


def test_cli_registers_blob_audit():
    parser = build_parser()
    args = parser.parse_args(["blob-audit", "camera-module.zip"])
    assert args.cmd == "blob-audit"


def test_cli_registers_config_diff():
    parser = build_parser()
    args = parser.parse_args([
        "config-diff",
        "ocvm.config",
        "stock.config",
        "--summary",
    ])
    assert args.cmd == "config-diff"
    assert args.kind == "auto"
    assert args.summary is True
