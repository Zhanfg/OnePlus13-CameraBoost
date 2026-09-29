package dev.cameraboost.oplus10bit;

import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.params.DynamicRangeProfiles;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Build;
import android.util.Size;

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

final class CameraCapabilityProbe {
    private CameraCapabilityProbe() {}

    static String buildReport(Context context) {
        StringBuilder out = new StringBuilder();
        out.append("CameraBoost OPlus 10-bit capability report\n");
        out.append("=========================================\n");
        out.append(FeaturePolicy.deviceIdentity()).append('\n');
        out.append("hook variant: enable10BitHEIC=")
                .append(BuildConfig.ENABLE_10BIT_HEIC)
                .append(", enable10BitLivePhoto=")
                .append(BuildConfig.ENABLE_10BIT_LIVE_PHOTO)
                .append("\n\n");

        CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        try {
            for (String id : manager.getCameraIdList()) {
                CameraCharacteristics c = manager.getCameraCharacteristics(id);
                appendCamera(out, id, c);
            }
        } catch (CameraAccessException e) {
            out.append("CameraAccessException: ").append(e).append('\n');
        } catch (Throwable t) {
            out.append("Probe failure: ").append(t).append('\n');
        }

        out.append("\nInterpretation:\n");
        out.append("- DYNAMIC_RANGE_TEN_BIT + P010 proves Android public 10-bit output capability.\n");
        out.append("- HEIC output support alone does not prove 10-bit HEIC encoding.\n");
        out.append("- JPEG_R / HEIC_ULTRAHDR are gain-map Ultra HDR formats, distinct from plain 10-bit HEIC.\n");
        out.append("- The OPlus feature gate still requires final-file verification.\n");
        return out.toString();
    }

    private static void appendCamera(
            StringBuilder out,
            String id,
            CameraCharacteristics c
    ) {
        out.append("Camera ID ").append(id).append('\n');

        Integer facing = c.get(CameraCharacteristics.LENS_FACING);
        Integer level = c.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
        out.append("  facing: ").append(facingName(facing)).append('\n');
        out.append("  hardwareLevel: ").append(levelName(level)).append('\n');

        int[] caps = c.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES);
        boolean tenBit = contains(
                caps,
                CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_DYNAMIC_RANGE_TEN_BIT
        );
        boolean raw = contains(caps, CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_RAW);

        out.append("  dynamicRangeTenBitCapability: ").append(tenBit).append('\n');
        out.append("  rawCapability: ").append(raw).append('\n');
        out.append("  capabilities: ").append(Arrays.toString(caps)).append('\n');

        if (Build.VERSION.SDK_INT >= 33) {
            DynamicRangeProfiles profiles =
                    c.get(CameraCharacteristics.REQUEST_AVAILABLE_DYNAMIC_RANGE_PROFILES);
            if (profiles != null) {
                Set<Long> supported = new TreeSet<>(profiles.getSupportedProfiles());
                out.append("  dynamicRangeProfiles: ");
                boolean first = true;
                for (Long p : supported) {
                    if (!first) out.append(", ");
                    first = false;
                    out.append(profileName(p)).append("(").append(p).append(")");
                }
                out.append('\n');
            }

            Long recommended =
                    c.get(CameraCharacteristics.REQUEST_RECOMMENDED_TEN_BIT_DYNAMIC_RANGE_PROFILE);
            out.append("  recommendedTenBitProfile: ")
                    .append(recommended == null ? "<none>" : profileName(recommended) + "(" + recommended + ")")
                    .append('\n');
        }

        StreamConfigurationMap map =
                c.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        if (map != null) {
            appendSizes(out, "P010", safeSizes(map, ImageFormat.YCBCR_P010));
            appendSizes(out, "HEIC", safeSizes(map, ImageFormat.HEIC));
            appendSizes(out, "RAW10", safeSizes(map, ImageFormat.RAW10));
            appendSizes(out, "RAW12", safeSizes(map, ImageFormat.RAW12));
            if (Build.VERSION.SDK_INT >= 34) {
                appendSizes(out, "JPEG_R(UltraHDR)", safeSizes(map, ImageFormat.JPEG_R));
            }
            if (Build.VERSION.SDK_INT >= 36) {
                appendSizes(out, "HEIC_ULTRAHDR", safeSizes(map, ImageFormat.HEIC_ULTRAHDR));
            }
        }

        out.append('\n');
    }

    private static Size[] safeSizes(StreamConfigurationMap map, int format) {
        try {
            return map.getOutputSizes(format);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void appendSizes(StringBuilder out, String name, Size[] sizes) {
        out.append("  ").append(name).append(": ");
        if (sizes == null || sizes.length == 0) {
            out.append("<none>\n");
            return;
        }

        int limit = Math.min(sizes.length, 12);
        for (int i = 0; i < limit; i++) {
            if (i > 0) out.append(", ");
            out.append(sizes[i].getWidth()).append('x').append(sizes[i].getHeight());
        }
        if (sizes.length > limit) {
            out.append(" ... +").append(sizes.length - limit);
        }
        out.append('\n');
    }

    private static boolean contains(int[] values, int needle) {
        if (values == null) return false;
        for (int value : values) {
            if (value == needle) return true;
        }
        return false;
    }

    private static String facingName(Integer facing) {
        if (facing == null) return "<unknown>";
        if (facing == CameraCharacteristics.LENS_FACING_FRONT) return "front";
        if (facing == CameraCharacteristics.LENS_FACING_BACK) return "back";
        if (facing == CameraCharacteristics.LENS_FACING_EXTERNAL) return "external";
        return String.valueOf(facing);
    }

    private static String levelName(Integer level) {
        if (level == null) return "<unknown>";
        if (level == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) return "LEGACY";
        if (level == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED) return "LIMITED";
        if (level == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL) return "FULL";
        if (level == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3) return "LEVEL_3";
        if (level == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_EXTERNAL) return "EXTERNAL";
        return String.valueOf(level);
    }

    private static String profileName(long profile) {
        if (profile == DynamicRangeProfiles.STANDARD) return "STANDARD";
        if (profile == DynamicRangeProfiles.HLG10) return "HLG10";
        if (profile == DynamicRangeProfiles.HDR10) return "HDR10";
        if (profile == DynamicRangeProfiles.HDR10_PLUS) return "HDR10_PLUS";
        if (profile == DynamicRangeProfiles.DOLBY_VISION_10B_HDR_REF) return "DV_10B_REF";
        if (profile == DynamicRangeProfiles.DOLBY_VISION_10B_HDR_OEM) return "DV_10B_OEM";
        if (profile == DynamicRangeProfiles.DOLBY_VISION_10B_HDR_REF_PO) return "DV_10B_REF_PO";
        if (profile == DynamicRangeProfiles.DOLBY_VISION_10B_HDR_OEM_PO) return "DV_10B_OEM_PO";
        return "PROFILE";
    }
}
