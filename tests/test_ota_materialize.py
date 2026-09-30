from cameraboost.ota_materialize import TARGET_PARTITIONS, plan_materialization


def make_index(*paths, source_type="zip"):
    return {
        "source": "official.zip",
        "source_type": source_type,
        "records": [
            {
                "path": path,
                "container": True,
            }
            for path in paths
        ],
    }


def test_payload_plan_targets_only_relevant_partitions():
    plan = plan_materialization(make_index("payload.bin"))
    ids = [row["id"] for row in plan["steps"]]
    assert ids[0] == "extract-payload"
    assert "extract-filesystems-read-only" in ids
    assert "reindex-extracted-tree" in ids
    assert plan["target_partitions"] == list(TARGET_PARTITIONS)
    assert plan["auto_execute"] is False
    assert plan["safety"]["flash_device"] is False


def test_super_plan_uses_lpunpack_role():
    plan = plan_materialization(make_index("super.img"))
    first = plan["steps"][0]
    assert first["id"] == "unpack-super"
    assert "lpunpack" in first["tool_examples"]


def test_direct_partition_images_skip_payload_and_super_steps():
    plan = plan_materialization(make_index("vendor.img", "odm.img"))
    ids = [row["id"] for row in plan["steps"]]
    assert "extract-payload" not in ids
    assert "unpack-super" not in ids
    assert "detect-image-format" in ids
    assert "extract-filesystems-read-only" in ids


def test_materialized_tree_only_needs_reindex():
    plan = plan_materialization({
        "source": "extracted/",
        "source_type": "directory",
        "records": [],
    })
    assert [row["id"] for row in plan["steps"]] == ["reindex-extracted-tree"]


def test_plan_never_enables_unsafe_actions():
    plan = plan_materialization(make_index("payload.bin"))
    assert not any(plan["safety"].values())
