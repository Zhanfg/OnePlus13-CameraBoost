package dev.cameraboost.oplus10bit;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

final class CapabilityKeyPolicy {
    private static final Set<String> EXPLICIT_TRUE = new HashSet<>(Arrays.asList(
            "com.oplus.10bits.heic.encode.support",
            "com.oplus.camera.heif.support.livephoto",
            "com.oplus.livephoto.support.heif",
            "com.oplus.livephoto.support.10bit",
            "com.oplus.camera.livephoto.support",
            "com.oplus.camera.video.livephoto.support",
            "com.oplus.camera.livephoto.mastermode.support",
            "com.oplus.camera.livephoto.enable.eis",
            "com.oplus.camera.livephoto.enable.frc",
            "com.oplus.camera.livephoto.reuse.video.codec.support",
            "com.oplus.feature.retro.camera.livephoto.default.open",
            "com.oplus.ai.capture.guide.support",
            "com.oplus.auto.composition.enable",
            "com.oplus.ai.composition.enable",
            "com.oplus.ai.composition.status.on",
            "com.oplus.feature.aicomposition.realscene.support",
            "com.oplus.feature.aicomposition.inspiration.support",
            "com.oplus.feature.master.jpg.max.support",
            "com.oplus.master.mode.enable.zsl",
            "com.oplus.high.resolution.support",
            "com.oplus.preview.ais.dct.support",
            "com.oplus.feature.master.video.eis.support",
            "com.oplus.feature.master.video.camera.mode.support",
            "com.oplus.feature.video.120fps.ultrawide.support",
            "com.oplus.feature.video.120fps.ultrawide.eis.support",
            "com.oplus.feature.video.8k30fps.ultrawide.eis.support",
            "com.oplus.video.ultrawide.support",
            "com.oplus.xpan.all.camera.support",
            "com.oplus.more.tele.zoom.support",
            "com.oplus.super.tele.zoom.support",
            "com.oplus.feature.sat.tele.support",
            "com.oplus.camera.mf.tele.marco.support",
            "com.oplus.feature.filter.preloadfilterresource.enable",
            "com.oplus.feature.qingtou.hupo.filter.support",
            "com.oplus.camera.retro.filter.fisheye.enable",
            "com.oplus.feature.multi.video.ultra.wide.support",
            "com.oplus.feature.video.super.eis.none.sat.ultra.wide.support",
            "com.oplus.camera.livephoto.grmode.support",
            "com.oplus.camera.livephoto.support.fov.optimize",
            "com.oplus.livephoto.buffer.copy.first.support",
            "com.oplus.livephoto.buffer.copy.minuv.support",
            "com.oplus.full.size.livephoto.complete.support",
            "com.oplus.1.1.size.livephoto.complete.support",
            "com.oplus.4.3.size.livephoto.complete.support"
    ));

    private static final String[] SAFETY_OR_NEGATIVE_TOKENS = {
            ".not.support",
            "unsupported",
            ".disable",
            "thermal",
            "overheat",
            "temperature",
            "fault",
            "protect",
            "low.memory",
            "battery",
            "power.limit",
            "ram.limit",
            "calibration",
            "factory",
            ".ftm."
    };

    private CapabilityKeyPolicy() {}

    static boolean shouldForceBoolean(String rawKey, RuntimeArchitecture runtime) {
        String key = normalize(rawKey);
        if (!key.startsWith("com.oplus.")) {
            return false;
        }

        if (containsAny(key, SAFETY_OR_NEGATIVE_TOKENS)) {
            return false;
        }

        if (key.contains("aicomposition") || key.contains("ai.composition")) {
            return runtime.modernAiComposition;
        }

        if (key.contains("ai.capture.guide")) {
            return runtime.modernAiComposition || runtime.legacyAiCaptureGuide;
        }

        if (key.contains("livephoto")) {
            return runtime.livePhotoPipeline;
        }

        if (EXPLICIT_TRUE.contains(key)) {
            return true;
        }

        // Broad ColorOS/realmeUI software-gate rule. It intentionally does not
        // touch numeric thresholds, zoom limits, calibration, thermal or safety data.
        return key.endsWith(".support")
                || key.endsWith(".enable")
                || key.endsWith(".default.open")
                || key.endsWith(".status.on");
    }

    static boolean shouldForceValueLegal(String rawKey, Object rawValue) {
        String key = normalize(rawKey);
        String value = rawValue == null
                ? ""
                : String.valueOf(rawValue).toLowerCase(Locale.ROOT);

        if ("com.oplus.configure.video.fps".equals(key)) {
            return value.contains("120fps") || value.equals("120");
        }

        return false;
    }

    static boolean isBooleanLikeConfigEntry(String type, String count, String value) {
        String t = type == null ? "" : type.trim().toLowerCase(Locale.ROOT);
        String c = count == null ? "" : count.trim();
        String v = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);

        boolean scalar = c.isEmpty() || "1".equals(c);
        boolean booleanValue = "0".equals(v)
                || "1".equals(v)
                || "false".equals(v)
                || "true".equals(v)
                || "off".equals(v)
                || "on".equals(v);

        boolean booleanType = t.isEmpty()
                || "byte".equals(t)
                || "bool".equals(t)
                || "boolean".equals(t)
                || "int".equals(t)
                || "int32".equals(t);

        return scalar && booleanValue && booleanType;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean containsAny(String value, String[] needles) {
        for (String needle : needles) {
            if (value.contains(needle)) {
                return true;
            }
        }
        return false;
    }
}
