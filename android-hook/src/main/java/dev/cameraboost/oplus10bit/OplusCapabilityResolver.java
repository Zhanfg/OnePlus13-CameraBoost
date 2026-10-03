package dev.cameraboost.oplus10bit;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

import de.robv.android.xposed.XposedHelpers;

/**
 * Runtime capability resolver for the shared OPlus camera stack.
 *
 * The resolver intentionally prefers implementation evidence over brand/model checks.
 * A OnePlus build may contain OPPO-originated camera features and vice versa; if the
 * implementation is present in the loaded camera APK, the feature can be considered
 * for unlock. Hardware-sensitive features are still kept behind their own guards.
 */
final class OplusCapabilityResolver {
    static final String TAG_AI_CAPTURE_GUIDE = "com.oplus.ai.capture.guide.support";
    static final String TAG_AI_COMPOSITION_ENABLE = "com.oplus.ai.composition.enable";
    static final String TAG_AI_COMPOSITION_STATUS_ON = "com.oplus.ai.composition.status.on";
    static final String TAG_AI_COMPOSITION_REALSCENE =
            "com.oplus.feature.aicomposition.realscene.support";
    static final String TAG_AI_COMPOSITION_INSPIRATION =
            "com.oplus.feature.aicomposition.inspiration.support";

    static final String TAG_VIDEO_LIVE_PHOTO = "com.oplus.camera.video.livephoto.support";
    static final String TAG_HEIF_LIVE_PHOTO = "com.oplus.camera.heif.support.livephoto";
    static final String TAG_10BIT_LIVE_PHOTO = "com.oplus.livephoto.support.10bit";

    static final String FEATURE_HIGH_PIXEL_LIVE_PHOTO = "camera_high_pixel_live_photo";

    private static final String CLASS_MODERN_AI =
            "com.oplus.camera.aicomposition.OplusAIComposition";
    private static final String CLASS_MODERN_AI_HELPER =
            "com.oplus.ocs.camera.OplusAICompositionHelper";
    private static final String CLASS_MODERN_AI_STATE =
            "com.oplus.camera.feature.aicomposition.state.CompositionStateMachine";
    private static final String CLASS_MODERN_AI_PREVIEW =
            "com.oplus.camera.feature.aicomposition.view.AICompositionPreviewView";
    private static final String CLASS_LEGACY_AI =
            "com.morphoinc.anchortracking.sdk.MorphoInitParams";

    private static final String CLASS_LIVE_PHOTO_DATA =
            "com.oplus.camera.feature.livephoto.io.LivePhotoSavedParams";
    private static final String CLASS_LIVE_PHOTO_EVENT =
            "com.oplus.camera.feature.livephoto.event.HandleEventMessage";

    private static final String CLASS_FILTER_MODERN =
            "com.oplus.camera.filter.FilterGroupManager";
    private static final String CLASS_FILTER_LEGACY =
            "com.oplus.ocs.camera.ipusdk.processunit.filter.list.FilterGroupManager";

    private final boolean modernAiComposition;
    private final boolean legacyAiCaptureGuide;
    private final boolean livePhotoPipeline;
    private final boolean modernFilterGroup;
    private final boolean legacyFilterGroup;

    private final OplusFeatureRegistry registry;
    private final OplusAssetResolver assets;
    private final Set<String> watchedTags;

    private OplusCapabilityResolver(
            boolean modernAiComposition,
            boolean legacyAiCaptureGuide,
            boolean livePhotoPipeline,
            boolean modernFilterGroup,
            boolean legacyFilterGroup,
            OplusFeatureRegistry registry
    ) {
        this.modernAiComposition = modernAiComposition;
        this.legacyAiCaptureGuide = legacyAiCaptureGuide;
        this.livePhotoPipeline = livePhotoPipeline;
        this.modernFilterGroup = modernFilterGroup;
        this.legacyFilterGroup = legacyFilterGroup;
        this.registry = registry;
        this.assets = new OplusAssetResolver();

        LinkedHashSet<String> tags = new LinkedHashSet<>();
        tags.add(TAG_AI_CAPTURE_GUIDE);
        tags.add(TAG_AI_COMPOSITION_ENABLE);
        tags.add(TAG_AI_COMPOSITION_STATUS_ON);
        tags.add(TAG_AI_COMPOSITION_REALSCENE);
        tags.add(TAG_AI_COMPOSITION_INSPIRATION);
        tags.add(TAG_VIDEO_LIVE_PHOTO);
        tags.add(TAG_HEIF_LIVE_PHOTO);
        tags.add(TAG_10BIT_LIVE_PHOTO);
        tags.add(FEATURE_HIGH_PIXEL_LIVE_PHOTO);
        tags.add(OplusConfigPatcher.TAG_10BIT_HEIC);
        tags.add(OplusConfigPatcher.TAG_VIDEO_10BIT);
        if (registry != null) tags.addAll(registry.matchedAnchors());
        for (OplusFeatureGateRegistry.GateSpec spec : OplusFeatureGateRegistry.specs()) {
            tags.add(spec.key);
        }
        this.watchedTags = Collections.unmodifiableSet(tags);
    }

    static OplusCapabilityResolver probe(
            ClassLoader classLoader,
            OplusFeatureRegistry registry
    ) {
        boolean modernAi = classExists(classLoader, CLASS_MODERN_AI)
                || classExists(classLoader, CLASS_MODERN_AI_HELPER);
        modernAi &= classExists(classLoader, CLASS_MODERN_AI_STATE)
                || classExists(classLoader, CLASS_MODERN_AI_PREVIEW)
                || (registry != null && registry.has("ai_composition"));

        boolean legacyAi = classExists(classLoader, CLASS_LEGACY_AI);

        boolean livePhoto = classExists(classLoader, CLASS_LIVE_PHOTO_DATA)
                || classExists(classLoader, CLASS_LIVE_PHOTO_EVENT)
                || (registry != null && registry.has("live_photo"));

        boolean modernFilter = classExists(classLoader, CLASS_FILTER_MODERN);
        boolean legacyFilter = classExists(classLoader, CLASS_FILTER_LEGACY);

        return new OplusCapabilityResolver(
                modernAi,
                legacyAi,
                livePhoto,
                modernFilter,
                legacyFilter,
                registry
        );
    }

    Set<String> watchedTags() {
        return watchedTags;
    }

    boolean hasAnchor(String anchor) {
        return registry != null && registry.matched(anchor);
    }

    boolean hasModernAiComposition() {
        return modernAiComposition;
    }

    boolean hasLegacyAiCaptureGuide() {
        return legacyAiCaptureGuide;
    }

    boolean hasAnyAiGuide() {
        return modernAiComposition || legacyAiCaptureGuide;
    }

    boolean hasLivePhotoPipeline() {
        return livePhotoPipeline;
    }

    boolean hasModernFilterGroup() {
        return modernFilterGroup;
    }

    boolean hasLegacyFilterGroup() {
        return legacyFilterGroup;
    }

    boolean shouldForceBoolean(String key) {
        if (key == null || key.isEmpty()) {
            return false;
        }
        if (!BuildConfig.ENABLE_COLOROS17_COMPAT || !FeaturePolicy.isTargetDevice()) {
            return shouldForceExplicitLegacyGate(key);
        }

        if (isAiKey(key)) {
            return hasAnyAiGuide();
        }

        if (isLivePhotoKey(key)) {
            if (TAG_10BIT_LIVE_PHOTO.equals(key)) {
                return BuildConfig.ENABLE_10BIT_LIVE_PHOTO && hasLivePhotoPipeline();
            }
            return hasLivePhotoPipeline();
        }

        if (OplusConfigPatcher.TAG_10BIT_HEIC.equals(key)) {
            return BuildConfig.ENABLE_10BIT_HEIC;
        }

        if (OplusFeatureGateRegistry.shouldForce(key, this)) {
            return true;
        }

        if (BuildConfig.ENABLE_EXPERIMENTAL_ALL
                && registry != null
                && registry.dynamicGateKeys().contains(key)
                && OplusDexGateDiscovery.isPositiveBooleanGate(key)) {
            return true;
        }

        if (registry != null) {
            OplusFeatureRegistry.Feature feature = registry.featureForAnchor(key);
            if (feature != null) {
                if (feature.layer == OplusFeatureRegistry.Layer.SOFTWARE_GATE) {
                    return true;
                }
                if (feature.layer == OplusFeatureRegistry.Layer.ASSET_REQUIRED) {
                    return assets.assetsReady(feature.id);
                }
                // Hardware-sensitive gates are deliberately not blanket-forced here.
                return false;
            }
        }

        return false;
    }

    private boolean shouldForceExplicitLegacyGate(String key) {
        if (OplusConfigPatcher.TAG_10BIT_HEIC.equals(key)) {
            return BuildConfig.ENABLE_10BIT_HEIC && FeaturePolicy.isTargetDevice();
        }
        if (OplusConfigPatcher.TAG_10BIT_LIVE_PHOTO.equals(key)) {
            return BuildConfig.ENABLE_10BIT_LIVE_PHOTO
                    && FeaturePolicy.isTargetDevice()
                    && hasLivePhotoPipeline();
        }
        return false;
    }

    boolean shouldAllowFeatureValue(String key, Object value) {
        if (!BuildConfig.ENABLE_COLOROS17_COMPAT || !FeaturePolicy.isTargetDevice()) {
            return false;
        }

        if (isAiKey(key)) {
            return hasAnyAiGuide();
        }

        if (isLivePhotoKey(key)) {
            return hasLivePhotoPipeline();
        }

        if ("com.oplus.configure.video.fps".equals(key)
                && value != null
                && "video_120fps".equals(String.valueOf(value))) {
            return registry != null
                    && (registry.has("video_4k120")
                    || registry.has("video_1080p120")
                    || registry.has("master_video_120"));
        }

        return false;
    }

    Set<String> experimentalSynthesizableGates() {
        if (!BuildConfig.ENABLE_EXPERIMENTAL_ALL || registry == null) {
            return Collections.emptySet();
        }
        LinkedHashSet<String> out = new LinkedHashSet<>();
        for (String key : registry.dynamicGateKeys()) {
            if (OplusDexGateDiscovery.isSafeToSynthesizeAsByte(key)) {
                out.add(key);
            }
        }
        return Collections.unmodifiableSet(out);
    }

    String assetStatus(String featureId) {
        return assets.describe(featureId);
    }

    String assetRoot(String featureId) {
        return assets.rootFor(featureId);
    }

    String firstReadableLutRoot() {
        return assets.firstReadableLutRoot();
    }

    String assetRoot(String featureId) {
        return assets.rootFor(featureId);
    }

    String firstReadableLutRoot() {
        return assets.firstReadableLutRoot();
    }

    private boolean isAiKey(String key) {
        return TAG_AI_CAPTURE_GUIDE.equals(key)
                || TAG_AI_COMPOSITION_ENABLE.equals(key)
                || TAG_AI_COMPOSITION_STATUS_ON.equals(key)
                || TAG_AI_COMPOSITION_REALSCENE.equals(key)
                || TAG_AI_COMPOSITION_INSPIRATION.equals(key)
                || key.toLowerCase(Locale.ROOT).contains("ai_composition");
    }

    private boolean isLivePhotoKey(String key) {
        return TAG_VIDEO_LIVE_PHOTO.equals(key)
                || TAG_HEIF_LIVE_PHOTO.equals(key)
                || TAG_10BIT_LIVE_PHOTO.equals(key)
                || FEATURE_HIGH_PIXEL_LIVE_PHOTO.equals(key)
                || key.toLowerCase(Locale.ROOT).contains("livephoto");
    }

    String describe() {
        String base = "modernAi=" + modernAiComposition
                + ", legacyAi=" + legacyAiCaptureGuide
                + ", livePhoto=" + livePhotoPipeline
                + ", modernFilter=" + modernFilterGroup
                + ", legacyFilter=" + legacyFilterGroup;
        if (registry == null) return base;
        return base + ", semanticFeatures={" + registry.summarize() + "}"
                + ", grAssets={" + assets.describe("gr_filters") + "}"
                + ", positiveAssets={" + assets.describe("positive_filters") + "}";
    }

    private static boolean classExists(ClassLoader classLoader, String className) {
        try {
            return XposedHelpers.findClass(className, classLoader) != null;
        } catch (Throwable ignored) {
            return false;
        }
    }
}
