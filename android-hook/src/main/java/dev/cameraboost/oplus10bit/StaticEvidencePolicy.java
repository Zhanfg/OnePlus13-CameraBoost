package dev.cameraboost.oplus10bit;

import java.util.ArrayList;
import java.util.List;

final class StaticEvidencePolicy {
    private StaticEvidencePolicy() {}

    static boolean allowMutation(
            boolean requested,
            boolean targetDevice,
            boolean oplusGateVerified,
            boolean halTenBitPathVerified,
            boolean heif10BitEncoderVerified,
            boolean colorMetadataVerified
    ) {
        return requested
                && targetDevice
                && oplusGateVerified
                && halTenBitPathVerified
                && heif10BitEncoderVerified
                && colorMetadataVerified;
    }

    static String explain(
            boolean requested,
            boolean targetDevice,
            boolean oplusGateVerified,
            boolean halTenBitPathVerified,
            boolean heif10BitEncoderVerified,
            boolean colorMetadataVerified
    ) {
        List<String> missing = new ArrayList<>();
        if (!requested) missing.add("build-variant-request");
        if (!targetDevice) missing.add("target-device");
        if (!oplusGateVerified) missing.add("oplus-camera-aps-gate");
        if (!halTenBitPathVerified) missing.add("hal-ten-bit-stream");
        if (!heif10BitEncoderVerified) missing.add("ten-bit-heif-encoder");
        if (!colorMetadataVerified) missing.add("color-metadata");

        if (missing.isEmpty()) {
            return "mutation-allowed";
        }
        return "probe-only; missing=" + String.join(",", missing);
    }
}
