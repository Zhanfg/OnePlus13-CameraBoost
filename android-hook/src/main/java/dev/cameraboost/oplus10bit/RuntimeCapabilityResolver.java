package dev.cameraboost.oplus10bit;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import de.robv.android.xposed.XposedHelpers;

final class RuntimeCapabilityResolver {
    private final ClassLoader classLoader;
    private final String sourceDir;
    private volatile Set<String> anchors;

    RuntimeCapabilityResolver(ClassLoader classLoader, String sourceDir) {
        this.classLoader = classLoader;
        this.sourceDir = sourceDir;
    }

    boolean hasAnchor(String anchor) {
        ensureIndexed();
        return anchors.contains(anchor);
    }

    boolean hasClass(String className) {
        try {
            return XposedHelpers.findClassIfExists(className, classLoader) != null;
        } catch (Throwable ignored) {
            return false;
        }
    }

    boolean supportsModernAiComposition() {
        return hasClass("com.oplus.camera.aicomposition.OplusAIComposition")
                || hasClass("com.oplus.ocs.camera.OplusAICompositionHelper")
                || (hasAnchor("OplusAIComposition")
                && hasAnchor("CompositionStateMachine"));
    }

    boolean supportsLegacyAiCaptureGuide() {
        return hasAnchor("MorphoInitParams");
    }

    boolean supportsLivePhoto() {
        return hasAnchor("VideoLivePhotoProcessor")
                && (hasAnchor("com.oplus.camera.livephoto.support")
                || hasAnchor("isLivePhotoSupported")
                || hasAnchor("camera_high_pixel_live_photo"));
    }

    boolean supportsFilterCore() {
        return hasClass("com.oplus.ocs.camera.ipusdk.processunit.filter.list.FilterGroupManager")
                || hasAnchor("FilterGroupManager");
    }

    boolean supportsGrLut() {
        return hasAnchor("gr.posi.rgba.bin")
                && hasAnchor("gr.nega.rgba.bin")
                && hasAnchor("gr.bw.rgba.bin")
                && hasAnchor("gr.hi.bw.rgba.bin");
    }

    String summary() {
        return "modernAI=" + supportsModernAiComposition()
                + ", legacyAI=" + supportsLegacyAiCaptureGuide()
                + ", livePhoto=" + supportsLivePhoto()
                + ", filterCore=" + supportsFilterCore()
                + ", grLut=" + supportsGrLut();
    }

    private void ensureIndexed() {
        if (anchors != null) {
            return;
        }
        synchronized (this) {
            if (anchors != null) {
                return;
            }
            DexAnchorIndex index = new DexAnchorIndex(OplusFeatureGateRegistry.anchors());
            Set<String> resolved = new HashSet<>(index.scanApk(sourceDir));
            anchors = Collections.unmodifiableSet(resolved);
            CameraBoostLog.log("runtime capability index ready: anchors=" + resolved.size());
        }
    }
}
