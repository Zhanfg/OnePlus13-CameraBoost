from cameraboost.oplus_config import (
    auto_diff,
    diff_keyed_configs,
    diff_sections,
    parse_keyed_config,
    summarize_diff,
)


def test_parse_keyed_config_tolerates_multiline_value():
    text = r"""
{
  "Key": "HQRAW_DRCGAIN_TRRIGGER",
  "Type": "Float",
  "Count": "1",
  "Value": "3.2"
},
{
  "VendorTag": "aps.params.snapshot.hdr",
  "Type": "Float",
  "Value": "
     6400/*iso*/, 0/*SharpLevel*/,
     120/*lux*/"
}
"""
    parsed = parse_keyed_config(text)
    assert parsed["HQRAW_DRCGAIN_TRRIGGER"].value == "3.2"
    assert parsed["aps.params.snapshot.hdr"].value == (
        "6400/*iso*/, 0/*SharpLevel*/, 120/*lux*/"
    )


def test_diff_keyed_config_reports_added_removed_changed():
    modified = r"""
{"Key":"A","Value":"2"}
{"VendorTag":"B","Value":"same"}
{"Key":"C","Value":"new"}
"""
    stock = r"""
{"Key":"A","Value":"1"}
{"VendorTag":"B","Value":"same"}
{"Key":"D","Value":"old"}
"""
    report = diff_keyed_configs(modified, stock)
    assert report["added"] == ["C"]
    assert report["removed"] == ["D"]
    assert [row["key"] for row in report["changed"]] == ["A"]


def test_diff_sections_and_summary():
    modified = """
[CameraIdConfig]
NumLogicalCameras = 5

[ZoomRange]
max = 20

[NewSection]
enable = true
"""
    stock = """
[CameraIdConfig]
NumLogicalCameras = 5

[ZoomRange]
max = 18

[OldSection]
enable = true
"""
    report = diff_sections(modified, stock)
    summary = summarize_diff(report)

    assert summary["added"] == ["NewSection"]
    assert summary["removed"] == ["OldSection"]
    assert summary["changed"] == ["ZoomRange"]


def test_auto_diff_selects_keyed_parser():
    report = auto_diff(
        '{"Key":"A","Value":"2"}',
        '{"Key":"A","Value":"1"}',
    )
    assert report["kind"] == "keyed"
