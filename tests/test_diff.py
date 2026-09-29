from cameraboost.aps import diff_values
from cameraboost.matrix import markdown_matrix

def test_diff_marks_camera_keys():
    left = {"video": {"fps": 60}, "ui": {"theme": "a"}}
    right = {"video": {"fps": 120}, "ui": {"theme": "b"}}
    changes = diff_values(left, right)
    by_path = {x.path: x for x in changes}
    assert by_path["video.fps"].camera_relevant is True
    assert by_path["ui.theme"].camera_relevant is False

def test_matrix_only_renders_camera_relevant_changes():
    report = {
        "files": {
            "a.json": {
                "changes": [
                    {"path": "hdr.enabled", "left": False, "right": True, "camera_relevant": True},
                    {"path": "theme.name", "left": "a", "right": "b", "camera_relevant": False},
                ]
            }
        }
    }
    md = markdown_matrix(report)
    assert "hdr.enabled" in md
    assert "theme.name" not in md
