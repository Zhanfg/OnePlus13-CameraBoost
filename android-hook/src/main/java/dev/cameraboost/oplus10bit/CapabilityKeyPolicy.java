package dev.cameraboost.oplus10bit;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

final class CapabilityKeyPolicy {
    private static final Set<String> EXPLICIT_TRUE = new HashSet<>(Arrays.asList(
            "com.oplus.10bits.heic.encode.support",
            "com.oplus.turboraw.re.support",
            "com.oplus.turboraw.re.open.bydefault",
            "com.oplus.feature.master.hq.raw.support",
            "com.oplus.feature.effect.style.support",
            "com.oplus.feature.face.beauty.custom.menu.version",
            "com.oplus.camera.feature.scale.focus",
            "com.oplus.camera.heif.support.livephoto",
            "com.oplus.livephoto.support.heif",
            "com.oplus.livephoto.support.10bit",
            "com.oplus.camera.livephoto.support",
            "com.oplus.camera.video.livephoto.support",
            "com.oplus.camera.livephoto.mastermode.support",
            "com.oplus.camera.livephoto.enable",
            "com.oplus.camera.livephoto.enable.eis",
            "com.oplus.camera.livephoto.enable.frc",
            "com.oplus.camera.livephoto.reuse.video.codec.support",
            "com.oplus.camera.livephoto.clear.video",
            "com.oplus.feature.retro.camera.livephoto.default.open",
            "com.oplus.ai.capture.guide.support",
            "com.oplus.auto.composition.enable",
            "com.oplus.ai.composition.enable",
            "com.oplus.ai.composition.status.on",
            "com.oplus.feature.aicomposition.realscene.support",
            "com.oplus.feature.aicomposition.inspiration.support",
            "com.oplus.ai.perfect.shot.guide.support",
            "com.oplus.ai.hd.switch.support",
            "com.oplus.tele.sdsr.support",
            "com.oplus.ai.scene.preset.support",
            "com.oplus.camera.preset.scene.detect.support",
            "com.oplus.feature.master.jpg.max.support",
            "com.oplus.master.mode.enable.zsl",
            "com.oplus.high.resolution.support",
            "com.oplus.camera.capture.hdr.support",
            "com.oplus.camera.preview.hdr.support",
            "com.oplus.camera.preview.hdr.video.support",
            "com.oplus.camera.preview.hdr.livephoto.support",
            "com.oplus.preview.ais.dct.support",
            "com.oplus.feature.master.video.eis.support",
            "com.oplus.feature.master.video.camera.mode.support",
            "com.oplus.feature.video.4k.120fps.support",
            "com.oplus.feature.video.1080p.120fps.support",
            "com.oplus.feature.video.dv.120fps.support",
            "com.oplus.feature.video.120fps.ultrawide.support",
            "com.oplus.feature.video.120fps.ultrawide.eis.support",
            "com.oplus.feature.video.8k30fps.ultrawide.eis.support",
            "com.oplus.feature.120fps.guide.support",
            "com.oplus.feature.video.dv.support",
            "com.oplus.feature.video.dv.60fps.support",
            "com.oplus.feature.video.dv.sat.support",
            "com.oplus.feature.video.front.dv.support",
            "com.oplus.feature.front.video.4k.support",
            "com.oplus.video.ultrawide.support",
            "com.oplus.video.auto.fps.setting.support",
            "com.oplus.video.lock.lens.support",
            "com.oplus.video.lock.wb.support",
            "com.oplus.video.sound.focus.support",
            "com.oplus.xpan.all.camera.support",
            "com.oplus.feature.xpan.mode.support",
            "com.oplus.more.tele.zoom.support",
            "com.oplus.super.tele.zoom.support",
            "com.oplus.feature.sat.tele.support",
            "com.oplus.feature.front.camera.wide.zoom.support",
            "com.oplus.rear.portrait.zoom.support",
            "com.oplus.portrait.rear.flash.support",
            "com.oplus.feature.tilt.shift.macro.support",
            "com.oplus.feature.macro.closeup.none.sat.tele.support",
            "com.oplus.feature.macro.depth.of.field.fusion.support",
            "com.oplus.camera.mf.tele.marco.support",
            "com.oplus.feature.filter.preloadfilterresource.enable",
            "com.oplus.feature.qingtou.hupo.filter.support",
            "com.oplus.tol.style.filter.support",
            "com.oplus.director.filter.support",
            "com.oplus.director.filter.upgrade.support",
            "com.oplus.support.grand.tour.filter",
            "com.oplus.support.jzk.movie.filter",
            "com.oplus.feature.soft.light.filter.support",
            "com.oplus.feature.flash.filter.support",
            "com.oplus.feature.os15.new.filter.support",
            "com.oplus.camera.retro.filter.fisheye.enable",
            "com.oplus.feature.multi.video.ultra.wide.support",
            "com.oplus.feature.video.super.eis.none.sat.ultra.wide.support",
            "com.oplus.camera.livephoto.grmode.support",
            "com.oplus.camera.livephoto.support.fov.optimize",
            "com.oplus.livephoto.buffer.copy.first.support",
            "com.oplus.livephoto.buffer.copy.minuv.support",
            "com.oplus.full.size.livephoto.complete.support",
            "com.oplus.1.1.size.livephoto.complete.support",
            "com.oplus.4.3.size.livephoto.complete.support",
            "com.oplus.feature.quick.launch.support",
            "com.oplus.motion.capture.support",
            "com.oplus.switch.lens.focal.length.support",
            "com.oplus.heif.blur.edit.in.gallery.support",
            "com.oplus.feature.super.text.scanner.support",
            "com.oplus.hasselblad.watermark.guide.support",
            "com.oplus.use.hasselblad.style.support",
            "com.oplus.camera.support.custom.hasselblad.watermark",
            "com.oplus.camera.support.custom.hasselblad.watermark.sellmode.default.open",
            "com.oplus.global.ev.support",
            "com.oplus.video.global.ev.support",
            "com.oplus.portrait.global.ev.support",
            "com.oplus.support.multi.frame.burst.shot",
            "com.oplus.support.multi.frame.burst.shot.cluster",
            "com.oplus.video.guide.support",
            "com.oplus.lumo.setting.guide.support",
            "com.ocs.camera.ipu.face.beauty.support",
            "com.ocs.camera.ipu.soft.light.photo.mode.support",
            "com.ocs.camera.ipu.soft.light.night.mode.support",
            "com.ocs.camera.ipu.soft.light.professional.mode.support",
            "com.ocs.camera.ipu.meishe.filter.support",

            // Stable user-facing feature IDs observed in the ColorOS 17 camera.
            // These represent product-tier exposure, not internal state-machine flags.
            "com.oplus.camera.feature.ai_composition",
            "com.oplus.camera.feature.autocomposition",
            "com.oplus.camera.feature.video_live_photo",
            "com.oplus.camera.feature.master_effect",
            "com.oplus.camera.feature.master_video_params",
            "com.oplus.camera.feature.raw",
            "com.oplus.camera.feature.xpan",
            "com.oplus.camera.feature.filter",
            "com.oplus.camera.feature.motion_capture",
            "com.oplus.camera.feature.macro",
            "com.oplus.camera.feature.logvideo",
            "com.oplus.camera.feature.hdr_all_route",
            "com.oplus.camera.feature.high_resolution.enable_by_ai_scene",
            "com.oplus.camera.feature.ai_enhancement_video",
            "com.oplus.camera.feature.portrait.blur",
            "com.oplus.camera.feature.video.blur",
            "com.oplus.camera.feature.multi_video",
            "com.oplus.camera.feature.video_night",
            "com.oplus.camera.feature.quick_video",
            "com.oplus.camera.feature.fast_video"
    ));

    private static final Set<String> DO_NOT_FORCE = new HashSet<>(Arrays.asList(
            "com.oplus.camera.ai.perception.detect.support",
            "com.oplus.camera.preview.hdr.transform.support",
            "com.oplus.camera.preview.merge.hdr.transform.support"
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
            ".ftm.",
            "mutex",
            ".conflict.",
            ".block.",
            "close.reason",
            ".limit.",
            "kill.apps",
            "capture_defer",
            "fallback"
    };

    // Physical capabilities that cannot be created by a software product-tier unlock
    // on OnePlus 13. Do not fabricate hardware that is absent from the target.
    private static final String[] HARDWARE_BLOCK_TOKENS = {
            "200m",
            "200mp",
            "microscope",
            "150.degree",
            "150degree"
    };

    // Generic support/enable keys are only promoted when they describe a user-facing
    // camera capability. This keeps thermal/memory/scheduler/calibration policy intact.
    private static final String[] USER_VISIBLE_TOKENS = {
            "ai.",
            "composition",
            "livephoto",
            "master",
            "raw",
            "heif",
            "hdr",
            "filter",
            "watermark",
            "video",
            "portrait",
            "macro",
            "tele",
            "zoom",
            "xpan",
            "motion",
            "burst",
            "text",
            "scanner",
            "beauty",
            "night",
            "underwater",
            "tilt",
            "street",
            "vibe",
            "lumo",
            "hasselblad",
            "dolby",
            "10bit",
            "4k",
            "8k",
            "120fps",
            "slow",
            "focus",
            "high.resolution",
            "high.pixel",
            "ultra.high",
            "sticker",
            "style",
            "effect",
            "soft.light",
            "quick.launch",
            "global.ev",
            "fisheye",
            "retro",
            "color",
            "logvideo"
    };

    private CapabilityKeyPolicy() {}

    static boolean shouldForceBoolean(String rawKey, RuntimeArchitecture runtime) {
        String key = normalize(rawKey);
        if (!isOplusCameraKey(key)) {
            return false;
        }

        if (DO_NOT_FORCE.contains(key)
                || containsAny(key, SAFETY_OR_NEGATIVE_TOKENS)
                || containsAny(key, HARDWARE_BLOCK_TOKENS)) {
            return false;
        }

        if (key.contains("aicomposition") || key.contains("ai.composition")) {
            return runtime.modernAiComposition || runtime.modernAiCompositionHelper;
        }

        if (key.contains("ai.capture.guide")) {
            // ColorOS 17 replaced the Morpho dependency with native AI Composition.
            // Either architecture is a valid implementation of the feature family.
            return runtime.modernAiComposition
                    || runtime.modernAiCompositionHelper
                    || runtime.legacyAiCaptureGuide;
        }

        if (key.contains("livephoto")) {
            return runtime.livePhotoPipeline && (
                    EXPLICIT_TRUE.contains(key)
                            || key.endsWith(".support")
                            || key.endsWith(".enable")
                            || key.endsWith(".default.open")
            );
        }

        if (EXPLICIT_TRUE.contains(key)) {
            return true;
        }

        // Cross-brand rule for ordinary user-facing OPlus/OCS software gates.
        // Numeric thresholds, scheduler policy, calibration and safety gates stay untouched.
        boolean genericGate = key.endsWith(".support")
                || key.endsWith(".enable")
                || key.endsWith(".default.open")
                || key.endsWith(".status.on")
                || key.startsWith("com.oplus.support.")
                || key.contains(".support.");

        return genericGate && containsAny(key, USER_VISIBLE_TOKENS);
    }

    static Set<String> explicitTrueKeys(RuntimeArchitecture runtime) {
        Set<String> result = new HashSet<>();
        for (String key : EXPLICIT_TRUE) {
            if (shouldForceBoolean(key, runtime)) {
                result.add(key);
            }
        }
        return result;
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

    private static boolean isOplusCameraKey(String key) {
        return key.startsWith("com.oplus.")
                || key.startsWith("com.ocs.camera.")
                || key.startsWith("com.oppo.camera.");
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
