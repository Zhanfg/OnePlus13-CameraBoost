package dev.cameraboost.oplus10bit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Shared OPPO/OnePlus camera feature registry.
 *
 * Brand is deliberately not part of the decision. A feature is considered present when
 * the current camera APK contains stable implementation/configuration evidence.
 */
final class OplusFeatureRegistry {
    enum Layer {
        SOFTWARE_GATE,
        ASSET_REQUIRED,
        HARDWARE_SENSITIVE
    }

    static final class Feature {
        final String id;
        final Layer layer;
        final List<String> anchors;

        Feature(String id, Layer layer, String... anchors) {
            this.id = id;
            this.layer = layer;
            this.anchors = Collections.unmodifiableList(Arrays.asList(anchors));
        }
    }

    private static final List<Feature> FEATURES = Collections.unmodifiableList(Arrays.asList(
            new Feature("ai_composition", Layer.SOFTWARE_GATE,
                    "com.oplus.ai.composition.enable",
                    "com.oplus.feature.aicomposition.realscene.support"),
            new Feature("ai_inspiration", Layer.SOFTWARE_GATE,
                    "com.oplus.feature.aicomposition.inspiration.support"),
            new Feature("ai_hd_zoom", Layer.SOFTWARE_GATE,
                    "com.oplus.ai.hd.switch.support",
                    "com.oplus.feature.aihd.sdsr.enable"),
            new Feature("live_photo", Layer.SOFTWARE_GATE,
                    "com.oplus.camera.livephoto.support",
                    "com.oplus.camera.video.livephoto.support"),
            new Feature("master_live_photo", Layer.SOFTWARE_GATE,
                    "com.oplus.camera.livephoto.mastermode.support"),
            new Feature("high_pixel_live_photo", Layer.HARDWARE_SENSITIVE,
                    "com.oplus.camera.high_pixel.mode.4k.live.support"),
            new Feature("live_photo_eis", Layer.SOFTWARE_GATE,
                    "com.oplus.camera.livephoto.enable.eis"),
            new Feature("heif_live_photo", Layer.SOFTWARE_GATE,
                    "com.oplus.camera.heif.support.livephoto",
                    "com.oplus.livephoto.support.heif"),
            new Feature("10bit_live_photo", Layer.HARDWARE_SENSITIVE,
                    "com.oplus.livephoto.support.10bit"),
            new Feature("10bit_heic", Layer.HARDWARE_SENSITIVE,
                    "com.oplus.10bits.heic.encode.support"),
            new Feature("heif", Layer.SOFTWARE_GATE,
                    "com.oplus.heif_enable",
                    "com.oplus.heic.codec.format"),
            new Feature("ultra_hdr", Layer.HARDWARE_SENSITIVE,
                    "com.oplus.feature.ultra.hdr.enable"),
            new Feature("preview_hdr", Layer.HARDWARE_SENSITIVE,
                    "com.oplus.preview.hdr.support",
                    "com.oplus.camera.preview.hdr.support"),
            new Feature("master_mode", Layer.SOFTWARE_GATE,
                    "com.oplus.is.master.mode",
                    "com.oplus.feature.master.mode.version"),
            new Feature("raw_max", Layer.HARDWARE_SENSITIVE,
                    "com.oplus.isRawMax"),
            new Feature("jpg_max", Layer.HARDWARE_SENSITIVE,
                    "com.oplus.feature.master.jpg.max.support"),
            new Feature("high_pixel", Layer.HARDWARE_SENSITIVE,
                    "com.oplus.feature.high.pixel.support",
                    "com.oplus.high.pixel.to.support"),
            new Feature("ai_high_pixel", Layer.HARDWARE_SENSITIVE,
                    "com.oplus.feature.ai.high.pixel.enable",
                    "com.oplus.feature.ai.scenery.mode.high.pixel.support"),
            new Feature("xpan", Layer.SOFTWARE_GATE,
                    "com.oplus.camera.feature.xpan",
                    "com.oplus.feature.xpan.mode.support"),
            new Feature("motion_capture", Layer.SOFTWARE_GATE,
                    "com.oplus.motion.capture.support",
                    "com.oplus.camera.feature.motion_capture"),
            new Feature("tele_sdsr", Layer.HARDWARE_SENSITIVE,
                    "com.oplus.tele.sdsr.support"),
            new Feature("macro_depth_fusion", Layer.HARDWARE_SENSITIVE,
                    "com.oplus.feature.macro.depth.of.field.fusion.support"),
            new Feature("portrait_rear_flash", Layer.SOFTWARE_GATE,
                    "com.oplus.portrait.rear.flash.support"),
            new Feature("front_4k", Layer.HARDWARE_SENSITIVE,
                    "com.oplus.feature.front.video.4k.support"),
            new Feature("video_sound_focus", Layer.SOFTWARE_GATE,
                    "com.oplus.video.sound.focus.support"),
            new Feature("video_auto_fps", Layer.SOFTWARE_GATE,
                    "com.oplus.video.auto.fps.setting.support"),
            new Feature("dolby_video", Layer.HARDWARE_SENSITIVE,
                    "com.oplus.configure.video.dolby"),
            new Feature("video_4k120", Layer.HARDWARE_SENSITIVE,
                    "com.oplus.feature.video.4k.120fps.support"),
            new Feature("video_1080p120", Layer.HARDWARE_SENSITIVE,
                    "com.oplus.feature.video.1080p.120fps.support"),
            new Feature("video_dv120", Layer.HARDWARE_SENSITIVE,
                    "com.oplus.feature.video.dv.120fps.support"),
            new Feature("master_video_120", Layer.HARDWARE_SENSITIVE,
                    "com.oplus.feature.master.video.120fps.support"),
            new Feature("custom_hasselblad_watermark", Layer.ASSET_REQUIRED,
                    "com.oplus.camera.support.custom.hasselblad.watermark"),
            new Feature("gr_filters", Layer.ASSET_REQUIRED,
                    "gr.posi.rgba.bin",
                    "gr.nega.rgba.bin",
                    "gr.bw.rgba.bin",
                    "gr.hi.bw.rgba.bin"),
            new Feature("positive_filters", Layer.ASSET_REQUIRED,
                    "positive_normal.bin",
                    "positive_master.bin",
                    "positive_hdr_normal_a_1.bin",
                    "positive_sdr_gen_a_1.bin")
    ));

    private final Set<String> matchedAnchors;
    private final Map<String, Feature> present = new LinkedHashMap<>();

    private OplusFeatureRegistry(Set<String> matchedAnchors) {
        this.matchedAnchors = matchedAnchors;
        for (Feature feature : FEATURES) {
            if (hasAnyAnchor(feature)) {
                present.put(feature.id, feature);
            }
        }
    }

    static OplusFeatureRegistry scan(String cameraApkPath) {
        Set<String> found = new DexAnchorIndex(allAnchors()).scanApk(cameraApkPath);
        return new OplusFeatureRegistry(found);
    }

    static Collection<String> allAnchors() {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        for (Feature feature : FEATURES) out.addAll(feature.anchors);
        return out;
    }

    boolean has(String featureId) {
        return present.containsKey(featureId);
    }

    Feature feature(String featureId) {
        return present.get(featureId);
    }

    Set<String> presentFeatureIds() {
        return Collections.unmodifiableSet(present.keySet());
    }

    Set<String> matchedAnchors() {
        return Collections.unmodifiableSet(matchedAnchors);
    }

    String summarize() {
        StringBuilder out = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, Feature> entry : present.entrySet()) {
            if (!first) out.append(", ");
            first = false;
            out.append(entry.getKey()).append('[').append(entry.getValue().layer).append(']');
        }
        return out.toString();
    }

    private boolean hasAnyAnchor(Feature feature) {
        for (String anchor : feature.anchors) {
            if (matchedAnchors.contains(anchor)) return true;
        }
        return false;
    }
}
