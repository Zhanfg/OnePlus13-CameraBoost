package dev.cameraboost.oplus10bit;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class StaticEvidencePolicyTest {
    @Test
    public void mutationRequiresEveryLayer() {
        assertTrue(StaticEvidencePolicy.allowMutation(
                true, true, true, true, true, true
        ));

        assertFalse(StaticEvidencePolicy.allowMutation(
                false, true, true, true, true, true
        ));
        assertFalse(StaticEvidencePolicy.allowMutation(
                true, false, true, true, true, true
        ));
        assertFalse(StaticEvidencePolicy.allowMutation(
                true, true, false, true, true, true
        ));
        assertFalse(StaticEvidencePolicy.allowMutation(
                true, true, true, false, true, true
        ));
        assertFalse(StaticEvidencePolicy.allowMutation(
                true, true, true, true, false, true
        ));
        assertFalse(StaticEvidencePolicy.allowMutation(
                true, true, true, true, true, false
        ));
    }

    @Test
    public void explanationNamesTheMissingEvidence() {
        String status = StaticEvidencePolicy.explain(
                true,
                true,
                false,
                false,
                false,
                false
        );

        assertTrue(status.contains("oplus-camera-aps-gate"));
        assertTrue(status.contains("hal-ten-bit-stream"));
        assertTrue(status.contains("ten-bit-heif-encoder"));
        assertTrue(status.contains("color-metadata"));
    }
}
