package dev.cameraboost.oplus10bit;

import android.os.Build;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

final class FeaturePolicy {
    private static final Set<String> ONEPLUS_13_MODELS = new HashSet<>(
            Arrays.asList("PJZ110", "CPH2653", "CPH2655")
    );

    private FeaturePolicy() {}

    static boolean isTargetDevice() {
        String model = safeUpper(Build.MODEL);
        String device = safeUpper(Build.DEVICE);
        String product = safeUpper(Build.PRODUCT);

        for (String allowed : ONEPLUS_13_MODELS) {
            String needle = allowed.toUpperCase(Locale.ROOT);
            if (model.contains(needle) || device.contains(needle) || product.contains(needle)) {
                return true;
            }
        }

        // OnePlus 13 platform codename.
        return "DODGE".equals(device) || product.contains("DODGE");
    }

    static boolean canEnable10BitStill() {
        return StaticEvidencePolicy.allowMutation(
                BuildConfig.ENABLE_10BIT_HEIC,
                isTargetDevice(),
                BuildConfig.STATIC_OPLUS_GATE_VERIFIED,
                BuildConfig.STATIC_HAL_TEN_BIT_PATH_VERIFIED,
                BuildConfig.STATIC_HEIF_ENCODER_10BIT_VERIFIED,
                BuildConfig.STATIC_COLOR_METADATA_VERIFIED
        );
    }

    static boolean canEnable10BitLivePhoto() {
        return BuildConfig.ENABLE_10BIT_LIVE_PHOTO && canEnable10BitStill();
    }

    static String tenBitMutationStatus() {
        return StaticEvidencePolicy.explain(
                BuildConfig.ENABLE_10BIT_HEIC,
                isTargetDevice(),
                BuildConfig.STATIC_OPLUS_GATE_VERIFIED,
                BuildConfig.STATIC_HAL_TEN_BIT_PATH_VERIFIED,
                BuildConfig.STATIC_HEIF_ENCODER_10BIT_VERIFIED,
                BuildConfig.STATIC_COLOR_METADATA_VERIFIED
        );
    }

    static String deviceIdentity() {
        return "MODEL=" + Build.MODEL
                + ", DEVICE=" + Build.DEVICE
                + ", PRODUCT=" + Build.PRODUCT
                + ", SDK=" + Build.VERSION.SDK_INT;
    }

    private static String safeUpper(String value) {
        return value == null ? "" : value.toUpperCase(Locale.ROOT);
    }
}
