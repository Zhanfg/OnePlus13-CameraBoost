package dev.cameraboost.oplus10bit;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * OPlus camera capability union used by the ColorOS 17 compatibility layer.
 *
 * A gate can be forced when the host advertises it, or synthesized when the
 * implementation anchor is present in the current camera APK and the spec is
 * explicitly marked synthesize=true.
 */
final class OplusFeatureGateRegistry {
    enum Risk {
        SAFE,
        PIPELINE_SENSITIVE,
        HARDWARE_SENSITIVE
    }

    static final class GateSpec {
        final String key;
        final String type;
        final String count;
        final String value;
        final Risk risk;
        final boolean synthesize;
        final String[] secondaryAnchors;

        GateSpec(String key, String type, String count, String value,
                 Risk risk, boolean synthesize, String... secondaryAnchors) {
            this.key = key;
            this.type = type;
            this.count = count;
            this.value = value;
            this.risk = risk;
            this.synthesize = synthesize;
            this.secondaryAnchors = secondaryAnchors == null
                    ? new String[0] : secondaryAnchors;
        }
    }

    private static final LinkedHashMap<String, GateSpec> SPECS = new LinkedHashMap<>();
    private static final Set<String> HOST_ADVERTISED = ConcurrentHashMap.newKeySet();

    static {
        // Core still / Master / RAW.
        safe("com.oplus.turboraw.re.support");
        safe("com.oplus.feature.effect.style.support");
        value("com.oplus.feature.master.mode.version", "Float", "1", "2.0",
                Risk.SAFE, true);
        safe("com.oplus.professional.use.hasselblad.style.support");
        safe("com.oplus.use.hasselblad.style.support");
        pipeline("com.oplus.feature.master.hq.raw.support", true);
        safe("com.oplus.rear.portrait.zoom.support");
        safe("com.oplus.heif.blur.edit.in.gallery.support");
        safe("com.oplus.camera.feature.scale.focus");
        safe("com.oplus.portrait.global.ev.support");
        safe("com.oplus.motion.capture.support");
        safe("com.oplus.camera.hasselblad.super.definition.support");

        // Macro / computational photography.
        value("com.oplus.feature.macro.closeup.max.zoom.value", "Float", "1", "30.0",
                Risk.PIPELINE_SENSITIVE, false);
        safe("com.oplus.feature.macro.closeup.none.sat.tele.support");
        safe("com.oplus.feature.macro.depth.of.field.fusion.support");
        safe("com.oplus.ai.hd.switch.support");
        value("com.oplus.ai.hd.zoom.value.default", "Float", "1", "60.0",
                Risk.PIPELINE_SENSITIVE, false);
        safe("com.oplus.tele.sdsr.support");
        value("com.oplus.tele.sdsr.zoom.value.default", "Float", "1", "20.0",
                Risk.PIPELINE_SENSITIVE, false);
        safe("com.oplus.ai.scene.preset.support");
        safe("com.oplus.support.multi.frame.burst.shot");
        safe("com.oplus.feature.super.text.scanner.support");

        // Live Photo.
        safe("com.oplus.camera.livephoto.support", "VideoLivePhotoProcessor");
        safe("com.oplus.camera.livephoto.mastermode.support", "VideoLivePhotoProcessor");
        safe("com.oplus.camera.livephoto.support.fov.optimize", "VideoLivePhotoProcessor");
        pipeline("com.oplus.camera.heif.support.livephoto", true, "VideoLivePhotoProcessor");
        pipeline("com.oplus.livephoto.support.10bit", false, "VideoLivePhotoProcessor");
        value("com.oplus.camera.livephoto.video.bitrate", "Int32", "1", "45",
                Risk.PIPELINE_SENSITIVE, true, "VideoLivePhotoProcessor");
        value("com.oplus.camera.livephoto.video.max.duration", "Int32", "1", "3200",
                Risk.SAFE, true, "VideoLivePhotoProcessor");
        value("com.oplus.camera.livephoto.video.min.duration", "Int32", "1", "500",
                Risk.SAFE, true, "VideoLivePhotoProcessor");

        // 10-bit / HDR / ProXDR.
        pipeline("com.oplus.10bits.heic.encode.support", true, "HEIF");
        safe("com.oplus.camera.preview.hdr.support", "DATASPACE_DISPLAY_P3_HLG");
        safe("com.oplus.feature.video.dv.support");
        hardware("com.oplus.feature.video.dv.60fps.support");
        hardware("com.oplus.feature.video.dv.sat.support");
        hardware("com.oplus.feature.video.front.dv.support");
        hardware("com.oplus.feature.video.dv.120fps.support", "video_120fps");

        // Video controls.
        safe("com.oplus.video.auto.fps.setting.support");
        safe("com.oplus.video.stop.record.sound.play.immediate");
        safe("com.oplus.video.lock.lens.support");
        safe("com.oplus.video.lock.wb.support");
        safe("com.oplus.feature.mic.status.check.support");
        safe("com.oplus.video.sound.focus.support");
        hardware("com.oplus.feature.front.video.4k.support");
        hardware("com.oplus.feature.video.720p.60fps.support");
        hardware("com.oplus.feature.slowvideo.ultra.wide.480fps.support");
        hardware("com.oplus.feature.video.1080p.120fps.support", "video_120fps");
        hardware("com.oplus.feature.video.4k.120fps.support", "video_120fps");

        // Filters / styles / XPAN / GR / soft light.
        safe("com.oplus.tol.style.filter.support");
        safe("com.oplus.support.grand.tour.filter");
        safe("com.oplus.desert.filter.type.support");
        safe("com.oplus.vignette.grain.filter.type.support");
        safe("com.oplus.director.filter.upgrade.support");
        safe("com.oplus.director.filter.support");
        safe("com.oplus.support.jzk.movie.filter");
        safe("com.ocs.camera.ipu.soft.light.photo.mode.support");
        safe("com.ocs.camera.ipu.soft.light.night.mode.support");
        safe("com.ocs.camera.ipu.soft.light.professional.mode.support");
        safe("com.ocs.camera.ipu.meishe.filter.support");
        safe("com.oplus.feature.soft.light.filter.support");
        safe("com.oplus.feature.flash.filter.support");
        safe("com.oplus.feature.os15.new.filter.support");
        value("com.oplus.xpan.mode.version", "Int32", "1", "3",
                Risk.SAFE, true, "XPAN");
        safe("com.oplus.xpan.legacy.ui.style", "XPAN");
        safe("com.oplus.gr.mode.support", "gr.posi.rgba.bin");

        // UI / mode exposure / controls.
        value("com.oplus.feature.face.beauty.custom.menu.version", "Int32", "1", "1",
                Risk.SAFE, true);
        safe("com.oplus.feature.quick.launch.support");
        safe("com.oplus.force.portrait.when.parse.intent");
        safe("com.oplus.feature.front.camera.wide.zoom.support");
        safe("com.oplus.portrait.rear.flash.support");
        safe("com.oplus.switch.lens.focal.length.support");

        // Watermark / brand-shared features.
        safe("com.oplus.hasselblad.watermark.guide.support");
        safe("com.oplus.camera.support.custom.hasselblad.watermark");
        safe("com.oplus.camera.support.custom.hasselblad.watermark.sellmode.default.open");
        safe("com.oplus.video.guide.support");

        // ColorOS 17 / Camera 7.x user-facing additions observed in 7.013.30.
        safe("com.oplus.feature.color.palette.support");
        safe("com.oplus.palette.capture.enable");
        pipeline("com.oplus.feature.master.jpg.max.support", true);
        safe("com.oplus.feature.master.video.camera.mode.support");
        safe("com.oplus.feature.master.video.1080.support");
        safe("com.oplus.feature.master.video.24fps.support");
        safe("com.oplus.feature.master.video.4k.support");
        safe("com.oplus.feature.master.video.4k.tele.support");
        safe("com.oplus.feature.master.video.4k.wide.support");
        safe("com.oplus.feature.master.video.60fps.support");
        safe("com.oplus.feature.master.video.8k.support");
        safe("com.oplus.feature.master.video.eis.support");
        safe("com.oplus.feature.master.video.eis.8k.support");
        safe("com.oplus.feature.master.video.focus.peaking.histogram.oplus.r.support");
        safe("com.oplus.feature.master.video.hdr.support");
        safe("com.oplus.feature.master.video.none.sat.tele.eis.support");
        safe("com.oplus.feature.master.video.none.sat.ultratele.eis.support");
        safe("com.oplus.feature.master.video.none.sat.ultratele.support");
        safe("com.oplus.feature.master.video.ratio.support");
        safe("com.oplus.feature.master.video.salient.object.detection.enabled");
        safe("com.oplus.master.video.color.tone.support");
        safe("com.oplus.master.video.lock.wb.support");
        safe("com.oplus.master.video.three.state.stabilization");
        safe("com.oplus.master.video.top.menu.support");
        safe("com.oplus.feature.movie.mode.log.support");
        safe("com.oplus.feature.photo.10bit.enable");
        safe("com.oplus.feature.video.10bit.enable");
        safe("com.oplus.feature.video.10bit.support");
        safe("com.oplus.feature.video.3hdr.10bit.support");
        safe("com.oplus.lumo.setting.guide.support");
        safe("com.oplus.telephotoVideo.sound_stage_pickup.support");
        safe("com.oplus.three.stage.animation.support");
        safe("com.oplus.ai.hd.gan.zoom.support");
        safe("com.oplus.ai.hd.icon.support");
        safe("com.oplus.feature.aihd.sdsr.enable");
        safe("com.oplus.feature.retro.camera.support");
        safe("com.oplus.feature.retro.camera.livephoto.default.open", "VideoLivePhotoProcessor");
        safe("com.oplus.camera.retro.filter.fisheye.enable");
        safe("com.oplus.feature.multi.video.ultra.wide.support");
        safe("com.oplus.camera.multi.video.back.sat.support");
        safe("com.oplus.camera.multi.video.v2.support");
        safe("com.oplus.feature.qingtou.hupo.filter.support");
        safe("com.oplus.feature.filter.preloadfilterresource.enable");
        safe("com.oplus.camera.video.livephoto.default.value.is.still", "VideoLivePhotoProcessor");
        pipeline("com.oplus.camera.high.pixel.mode.4k.live.dynamic.preview.support",
                false, "camera_high_pixel_live_photo");
        hardware("com.oplus.feature.video.120fps.ultrawide.support", "video_120fps");
        hardware("com.oplus.feature.video.120fps.ultrawide.eis.support", "video_120fps");
        hardware("com.oplus.feature.video.8k30fps.ultrawide.eis.support");
        safe("com.oplus.feature.video.super.eis.none.sat.ultra.wide.support");
        safe("com.oplus.high.pixel.zoom.nonarc.support");
        safe("com.oplus.ultra.wide.display.zoom.value.support");
        safe("com.oplus.camera.volume.zoom.aidl.enable");
        safe("com.oplus.feature.front.zoom.anim.in.hal");
        safe("com.oplus.feature.assist.center.indicator.support");
        safe("com.oplus.feature.monitor.assist.support");
        safe("com.oplus.feature.rack.screen.mode.support");
        safe("com.oplus.camera.direct.launcher.support");

        // ColorOS 17: modern AI Composition replaces the legacy Morpho gate.
        safe("com.oplus.ai.capture.guide.support", "OplusAIComposition");
        safe("com.oplus.ai.composition.enable", "OplusAIComposition");
        safe("com.oplus.feature.aicomposition.realscene.support", "CompositionStateMachine");
        safe("com.oplus.feature.aicomposition.inspiration.support", "DeepThinkManager");
        safe("com.oplus.ai.composition.status.on", "OplusAIComposition");
    }

    private OplusFeatureGateRegistry() {}

    static Collection<GateSpec> specs() {
        return Collections.unmodifiableCollection(SPECS.values());
    }

    static Collection<String> anchors() {
        ArrayList<String> out = new ArrayList<>();
        for (GateSpec spec : SPECS.values()) {
            out.add(spec.key);
            Collections.addAll(out, spec.secondaryAnchors);
        }
        Collections.addAll(out,
                "OplusAIComposition",
                "CompositionStateMachine",
                "AICompositionPreviewView",
                "DeepThinkManager",
                "MorphoInitParams",
                "VideoLivePhotoProcessor",
                "camera_high_pixel_live_photo",
                "isLivePhotoSupported",
                "FilterGroupManager",
                "gr.posi.rgba.bin",
                "gr.nega.rgba.bin",
                "gr.bw.rgba.bin",
                "gr.hi.bw.rgba.bin",
                "DATASPACE_DISPLAY_P3_HLG",
                "com.oplus.isRawMax",
                "com.oplus.feature.master.jpg.max.support",
                "HEIF",
                "XPAN",
                "video_120fps");
        return out;
    }

    static GateSpec get(String key) {
        return SPECS.get(key);
    }

    static boolean knows(String key) {
        return key != null && SPECS.containsKey(key);
    }

    static void markHostAdvertised(String key) {
        if (knows(key)) {
            HOST_ADVERTISED.add(key);
        }
    }

    static boolean shouldForce(String key, OplusCapabilityResolver resolver) {
        GateSpec spec = get(key);
        if (spec == null || !FeaturePolicy.isTargetDevice()) {
            return false;
        }
        if (HOST_ADVERTISED.contains(key)) {
            return secondaryAnchorsSatisfied(spec, resolver);
        }

        boolean maySynthesize = spec.synthesize || BuildConfig.ENABLE_EXPERIMENTAL_ALL;
        if (!maySynthesize || !resolver.hasAnchor(key)) {
            return false;
        }

        // The all-unlock variant is deliberately aggressive, but still refuses to
        // invent a gate that is absent from the current Camera APK or whose secondary
        // implementation anchors are missing.
        return secondaryAnchorsSatisfied(spec, resolver);
    }

    static boolean shouldSynthesize(GateSpec spec, OplusCapabilityResolver resolver) {
        boolean maySynthesize = spec != null
                && (spec.synthesize || BuildConfig.ENABLE_EXPERIMENTAL_ALL);
        return maySynthesize
                && !HOST_ADVERTISED.contains(spec.key)
                && resolver.hasAnchor(spec.key)
                && secondaryAnchorsSatisfied(spec, resolver)
                && FeaturePolicy.isTargetDevice();
    }

    static Object forcedValueForReturnType(GateSpec spec, Class<?> returnType) {
        if (spec == null || returnType == null) {
            return null;
        }
        String value = spec.value;
        if (returnType == boolean.class || returnType == Boolean.class) {
            return true;
        }
        if (returnType == int.class || returnType == Integer.class) {
            try {
                return (int) Math.round(Double.parseDouble(value));
            } catch (NumberFormatException ignored) {
                return 1;
            }
        }
        if (returnType == long.class || returnType == Long.class) {
            try {
                return Math.round(Double.parseDouble(value));
            } catch (NumberFormatException ignored) {
                return 1L;
            }
        }
        if (returnType == float.class || returnType == Float.class) {
            try {
                return Float.parseFloat(value);
            } catch (NumberFormatException ignored) {
                return 1.0f;
            }
        }
        if (returnType == double.class || returnType == Double.class) {
            try {
                return Double.parseDouble(value);
            } catch (NumberFormatException ignored) {
                return 1.0d;
            }
        }
        if (returnType == String.class) {
            return value;
        }
        return null;
    }

    static String enabledValueForDeclaredType(GateSpec spec, String declaredType) {
        if (spec == null) {
            return "1";
        }
        return spec.value;
    }

    private static boolean secondaryAnchorsSatisfied(
            GateSpec spec, OplusCapabilityResolver resolver) {
        for (String anchor : spec.secondaryAnchors) {
            if (!resolver.hasAnchor(anchor)) {
                return false;
            }
        }
        return true;
    }

    private static void safe(String key, String... anchors) {
        value(key, "Byte", "1", "1", Risk.SAFE, true, anchors);
    }

    private static void pipeline(String key, boolean synthesize, String... anchors) {
        value(key, "Byte", "1", "1", Risk.PIPELINE_SENSITIVE, synthesize, anchors);
    }

    private static void hardware(String key, String... anchors) {
        // Do not invent a hardware-sensitive feature if the target config never advertised it.
        value(key, "Byte", "1", "1", Risk.HARDWARE_SENSITIVE, false, anchors);
    }

    private static void value(String key, String type, String count, String value,
                              Risk risk, boolean synthesize, String... anchors) {
        SPECS.put(key, new GateSpec(key, type, count, value, risk, synthesize, anchors));
    }
}
