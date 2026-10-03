package dev.cameraboost.oplus10bit;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

final class CapabilityValuePolicy {
    static final class OverrideSpec {
        final String type;
        final String count;
        final String value;

        OverrideSpec(String type, String count, String value) {
            this.type = type;
            this.count = count;
            this.value = value;
        }
    }

    private static final Map<String, OverrideSpec> OVERRIDES;

    static {
        Map<String, OverrideSpec> values = new LinkedHashMap<>();

        // Stable semantic/version gates from the public OPCameraPro baseline.
        values.put("com.oplus.feature.master.mode.version",
                new OverrideSpec("Float", "1", "2.0"));
        values.put("com.oplus.xpan.mode.version",
                new OverrideSpec("Int32", "1", "3"));
        values.put("com.oplus.facebeauty.version",
                new OverrideSpec("Int32", "1", "7"));

        // ColorOS Live Photo defaults. These are conservative values already used
        // by the legacy implementation and are only applied in the full-unlock build.
        values.put("com.oplus.camera.livephoto.video.bitrate",
                new OverrideSpec("Int32", "1", "45"));
        values.put("com.oplus.camera.livephoto.video.max.duration",
                new OverrideSpec("Int32", "1", "3200"));
        values.put("com.oplus.camera.livephoto.video.min.duration",
                new OverrideSpec("Int32", "1", "500"));
        values.put("com.oplus.camera.livephoto.color.dataspace.value",
                new OverrideSpec("String", "3", "1,2,2"));
        values.put("com.oplus.camera.livephoto.gyro.threshould.vector",
                new OverrideSpec("Float", "2", "2.6,2.6"));

        // 120 FPS ranges used by OPlus' own feature table when the mode is exposed.
        values.put("com.oplus.feature.video.4k.120fps.zoom.range",
                new OverrideSpec("Float", "2", "1,10"));
        values.put("com.oplus.feature.video.4k120fps.max.zoom.list",
                new OverrideSpec("Float", "3", "1,2.9,10"));
        values.put("com.oplus.feature.video.1080p.120fps.zoom.range",
                new OverrideSpec("Float", "2", "1,10"));
        values.put("com.oplus.feature.video.1080p120fps.max.zoom.list",
                new OverrideSpec("Float", "3", "1,2.9,10"));

        // Feature tuning values from the public cross-device baseline.
        values.put("com.oplus.ai.hd.zoom.value.default",
                new OverrideSpec("Float", "1", "60"));
        values.put("com.oplus.ai.hd.gan.zoom.value",
                new OverrideSpec("Float", "1", "20"));
        values.put("com.oplus.motion.capture.max.zoom.value",
                new OverrideSpec("Float", "1", "30.0"));
        values.put("com.oplus.camera.preview.hdr.brightness.ratio",
                new OverrideSpec("Float", "1", "5"));
        values.put("com.oplus.camera.preview.hdr.video.brightness.ratio",
                new OverrideSpec("Float", "1", "5"));
        values.put("com.oplus.camera.preview.hdr.cap.mode.value",
                new OverrideSpec("String", "5", "common,night,highPixel,sticker,idPhoto"));
        values.put("com.oplus.camera.capture.hdr.cap.mode.value",
                new OverrideSpec("String", "6", "common,portrait,night,highPixel,sticker,idPhoto"));

        // Keep the modern XPAN UI path; do not fall back to the legacy layout.
        values.put("com.oplus.xpan.legacy.ui.style",
                new OverrideSpec("Byte", "1", "0"));

        OVERRIDES = Collections.unmodifiableMap(values);
    }

    private CapabilityValuePolicy() {}

    static OverrideSpec find(String rawKey, RuntimeArchitecture runtime) {
        if (rawKey == null) {
            return null;
        }

        String key = rawKey.trim().toLowerCase(Locale.ROOT);
        OverrideSpec spec = OVERRIDES.get(key);
        if (spec == null) {
            return null;
        }

        if (key.contains("livephoto") && !runtime.livePhotoPipeline) {
            return null;
        }

        return spec;
    }

    static Map<String, OverrideSpec> all() {
        return OVERRIDES;
    }
}
