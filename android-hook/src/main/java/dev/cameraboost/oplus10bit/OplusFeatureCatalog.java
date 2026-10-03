package dev.cameraboost.oplus10bit;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * OPlus camera feature gates observed in OPCameraPro and the ColorOS 17 camera stack.
 *
 * The "all" build removes product/brand gating for these software-visible capabilities.
 * Hardware-only modes are intentionally not synthesized here.
 */
final class OplusFeatureCatalog {
    static final class OverrideValue {
        final String type;
        final String count;
        final String value;

        OverrideValue(String type, String count, String value) {
            this.type = type;
            this.count = count;
            this.value = value;
        }

        boolean asBoolean() {
            return !"0".equals(value) && !"0.0".equals(value) && !value.isEmpty();
        }
    }

    private static final Map<String, OverrideValue> ALL_OVERRIDES;
    private static final Set<String> COLOROS17_COMPAT_GATES;

    static {
        LinkedHashMap<String, OverrideValue> m = new LinkedHashMap<>();

        // Core still / Master / RAW.
        byteFlag(m, "com.oplus.turboraw.re.support");
        put(m, "com.oplus.feature.master.mode.version", "Float", "1", "2.0");
        byteFlag(m, "com.oplus.feature.master.hq.raw.support");
        byteFlag(m, "com.oplus.feature.effect.style.support");
        byteFlag(m, "com.oplus.camera.feature.scale.focus");
        byteFlag(m, "com.oplus.portrait.global.ev.support");
        byteFlag(m, "com.oplus.camera.hasselblad.super.definition.support");

        // Portrait / focus / zoom.
        byteFlag(m, "com.oplus.rear.portrait.zoom.support");
        byteFlag(m, "com.oplus.save.portrait.zoom.value");
        byteFlag(m, "com.oplus.portrait.rear.flash.support");
        byteFlag(m, "com.oplus.feature.front.camera.wide.zoom.support");
        byteFlag(m, "com.oplus.switch.lens.focal.length.support");

        // Macro and computational stills.
        byteFlag(m, "com.oplus.feature.tilt.shift.macro.support");
        byteFlag(m, "com.oplus.feature.macro.closeup.none.sat.tele.support");
        byteFlag(m, "com.oplus.feature.macro.depth.of.field.fusion.support");
        byteFlag(m, "com.oplus.motion.capture.support");
        byteFlag(m, "com.oplus.support.multi.frame.burst.shot");
        byteFlag(m, "com.oplus.ai.hd.switch.support");
        byteFlag(m, "com.oplus.tele.sdsr.support");

        // HEIF / HDR / Live Photo.
        byteFlag(m, "com.oplus.heif.blur.edit.in.gallery.support");
        byteFlag(m, OplusConfigPatcher.TAG_10BIT_HEIC);
        byteFlag(m, OplusConfigPatcher.TAG_HEIF_LIVE_PHOTO);
        byteFlag(m, OplusConfigPatcher.TAG_10BIT_LIVE_PHOTO);
        byteFlag(m, "com.oplus.camera.livephoto.support");
        byteFlag(m, "com.oplus.camera.livephoto.mastermode.support");
        byteFlag(m, "com.oplus.camera.livephoto.support.fov.optimize");
        put(m, "com.oplus.camera.livephoto.video.bitrate", "Int32", "1", "45");
        byteFlag(m, "com.oplus.camera.preview.hdr.support");

        // Video.
        byteFlag(m, "com.oplus.feature.video.720p.60fps.support");
        byteFlag(m, "com.oplus.feature.slowvideo.ultra.wide.480fps.support");
        byteFlag(m, "com.oplus.video.auto.fps.setting.support");
        byteFlag(m, "com.oplus.video.stop.record.sound.play.immediate");
        byteFlag(m, "com.oplus.feature.video.dv.support");
        byteFlag(m, "com.oplus.feature.video.dv.60fps.support");
        byteFlag(m, "com.oplus.feature.video.dv.sat.support");
        byteFlag(m, "com.oplus.feature.video.front.dv.support");
        byteFlag(m, "com.oplus.video.lock.lens.support");
        byteFlag(m, "com.oplus.video.lock.wb.support");
        byteFlag(m, "com.oplus.feature.mic.status.check.support");
        byteFlag(m, "com.oplus.feature.video.4k.120fps.support");
        byteFlag(m, "com.oplus.feature.video.1080p.120fps.support");
        byteFlag(m, "com.oplus.feature.video.dv.120fps.support");
        byteFlag(m, "com.oplus.video.sound.focus.support");
        byteFlag(m, "com.oplus.feature.front.video.4k.support");
        byteFlag(m, OplusConfigPatcher.TAG_VIDEO_10BIT);

        // Filters / styles / modes shared across OPPO and OnePlus camera bases.
        byteFlag(m, "com.oplus.tol.style.filter.support");
        byteFlag(m, "com.oplus.support.grand.tour.filter");
        byteFlag(m, "com.oplus.desert.filter.type.support");
        byteFlag(m, "com.oplus.vignette.grain.filter.type.support");
        byteFlag(m, "com.oplus.director.filter.upgrade.support");
        byteFlag(m, "com.oplus.director.filter.support");
        byteFlag(m, "com.oplus.support.jzk.movie.filter");
        byteFlag(m, "com.oplus.feature.soft.light.filter.support");
        byteFlag(m, "com.oplus.feature.flash.filter.support");
        byteFlag(m, "com.ocs.camera.ipu.meishe.filter.support");
        byteFlag(m, "com.ocs.camera.ipu.soft.light.photo.mode.support");
        byteFlag(m, "com.ocs.camera.ipu.soft.light.night.mode.support");
        byteFlag(m, "com.ocs.camera.ipu.soft.light.professional.mode.support");
        put(m, "com.oplus.xpan.mode.version", "Int32", "1", "3");
        byteFlag(m, "com.oplus.gr.mode.support");
        byteFlag(m, "com.oplus.feature.os15.new.filter.support");

        // UI / launch / scanner.
        byteFlag(m, "com.oplus.feature.quick.launch.support");
        byteFlag(m, "com.oplus.force.portrait.when.parse.intent");
        byteFlag(m, "com.oplus.feature.face.beauty.custom.menu.version");
        byteFlag(m, "com.oplus.feature.super.text.scanner.support");
        byteFlag(m, "com.oplus.ai.scene.preset.support");

        // Watermarks / guide surfaces.
        byteFlag(m, "com.oplus.hasselblad.watermark.guide.support");
        byteFlag(m, "com.oplus.camera.support.custom.hasselblad.watermark");
        byteFlag(m, "com.oplus.camera.support.custom.hasselblad.watermark.sellmode.default.open");
        byteFlag(m, "com.oplus.video.guide.support");

        // ColorOS 17 AI Composition replaces the old Morpho-only gate.
        byteFlag(m, "com.oplus.ai.capture.guide.support");
        byteFlag(m, "com.oplus.ai.composition.enable");
        byteFlag(m, "com.oplus.feature.aicomposition.realscene.support");
        byteFlag(m, "com.oplus.feature.aicomposition.inspiration.support");

        ALL_OVERRIDES = Collections.unmodifiableMap(m);

        LinkedHashSet<String> compat = new LinkedHashSet<>();
        compat.add("com.oplus.ai.capture.guide.support");
        compat.add("com.oplus.ai.composition.enable");
        compat.add("com.oplus.feature.aicomposition.realscene.support");
        compat.add("com.oplus.feature.aicomposition.inspiration.support");
        COLOROS17_COMPAT_GATES = Collections.unmodifiableSet(compat);
    }

    private OplusFeatureCatalog() {}

    static Map<String, OverrideValue> allOverrides() {
        return ALL_OVERRIDES;
    }

    static OverrideValue overrideFor(String key, ColorOS17CompatResolver resolver) {
        if (key == null || key.isEmpty()) {
            return null;
        }

        if (BuildConfig.ENABLE_ALL_SOFTWARE_CAPABILITIES) {
            OverrideValue value = ALL_OVERRIDES.get(key);
            if (value == null) {
                return null;
            }
            if (key.contains("aicomposition") || key.contains("ai.composition")
                    || key.contains("ai.capture.guide")) {
                return resolver.supportsAnyAiGuide() ? value : null;
            }
            if (key.contains("livephoto")) {
                return resolver.supportsLivePhotoStack() ? value : null;
            }
            return value;
        }

        if (BuildConfig.ENABLE_COLOROS17_COMPAT
                && COLOROS17_COMPAT_GATES.contains(key)
                && resolver.supportsModernAiComposition()) {
            return ALL_OVERRIDES.get(key);
        }

        return null;
    }

    static boolean shouldForceBoolean(String key, ColorOS17CompatResolver resolver) {
        OverrideValue value = overrideFor(key, resolver);
        return value != null && value.asBoolean();
    }

    static boolean isWatched(String key) {
        return ALL_OVERRIDES.containsKey(key);
    }

    private static void byteFlag(Map<String, OverrideValue> map, String key) {
        put(map, key, "Byte", "1", "1");
    }

    private static void put(
            Map<String, OverrideValue> map,
            String key,
            String type,
            String count,
            String value
    ) {
        map.put(key, new OverrideValue(type, count, value));
    }
}
