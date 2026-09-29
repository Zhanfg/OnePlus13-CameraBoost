from cameraboost.gates import FeatureEvidence, assess, highest_contiguous_stage

def test_stage_is_contiguous():
    evidence = FeatureEvidence(
        name="4k120",
        discovered=True,
        advertised=True,
        callable=False,
        streaming=True,
    )
    assert highest_contiguous_stage(evidence) == "advertised"
    assert assess(evidence)["next_probe_layer"] == "app-or-feature-gate"
    assert assess(evidence)["safe_to_auto_enable"] is False

def test_validated_feature_can_be_marked_safe():
    evidence = FeatureEvidence(
        name="preview_hdr",
        discovered=True,
        advertised=True,
        callable=True,
        streaming=True,
        stable=True,
        validated=True,
    )
    result = assess(evidence)
    assert result["stage"] == "validated"
    assert result["next_probe_layer"] == "complete"
    assert result["safe_to_auto_enable"] is True
