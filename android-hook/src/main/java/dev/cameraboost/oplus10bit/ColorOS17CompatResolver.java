package dev.cameraboost.oplus10bit;

import java.io.File;
import java.util.concurrent.ConcurrentHashMap;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.MethodMatcher;

import de.robv.android.xposed.XposedBridge;

final class ColorOS17CompatResolver {
    private static volatile ColorOS17CompatResolver INSTANCE;

    private final ClassLoader classLoader;
    private final String apkPath;
    private final ConcurrentHashMap<String, Boolean> classCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> semanticClassCache = new ConcurrentHashMap<>();
    private volatile boolean dexKitLoadAttempted;
    private volatile boolean dexKitAvailable;

    private ColorOS17CompatResolver(ClassLoader classLoader, String apkPath) {
        this.classLoader = classLoader;
        this.apkPath = apkPath;
    }

    static ColorOS17CompatResolver init(ClassLoader classLoader, String apkPath) {
        ColorOS17CompatResolver resolver = new ColorOS17CompatResolver(classLoader, apkPath);
        INSTANCE = resolver;
        resolver.logSummary();
        return resolver;
    }

    static ColorOS17CompatResolver get() {
        ColorOS17CompatResolver resolver = INSTANCE;
        if (resolver == null) {
            throw new IllegalStateException("ColorOS17CompatResolver not initialized");
        }
        return resolver;
    }

    boolean supportsModernAiComposition() {
        return hasClass("com.oplus.ocs.camera.OplusAICompositionHelper")
                && hasClass("com.oplus.camera.feature.aicomposition.state.CompositionStateMachine")
                && hasClass("com.oplus.camera.feature.aicomposition.view.AICompositionPreviewView");
    }

    boolean supportsLegacyAiCaptureGuide() {
        return hasClass("com.morphoinc.anchortracking.sdk.MorphoInitParams");
    }

    boolean supportsAnyAiGuide() {
        return supportsModernAiComposition() || supportsLegacyAiCaptureGuide();
    }

    boolean supportsLivePhotoStack() {
        return hasClass("com.oplus.camera.feature.livephoto.io.LivePhotoSavedParams")
                || hasClass("com.oplus.camera.feature.livephoto.data.LivePhotoVideoContract")
                || hasClass("com.oplus.camera.feature.video.livephoto.data.VideoSavedParams");
    }

    boolean supportsModernFilterStack() {
        return hasClass("com.oplus.camera.filter.FilterGroupManager")
                || hasClass("com.oplus.ocs.camera.ipusdk.processunit.filter.list.FilterGroupManager");
    }

    boolean hasMeisheLutDirectory() {
        File dir = new File("/odm/etc/camera/meishe_lut");
        return dir.isDirectory() && dir.canRead();
    }

    String getMeisheLutDirectory() {
        return hasMeisheLutDirectory() ? "/odm/etc/camera/meishe_lut/" : null;
    }

    String resolveLivePhotoGateClass() {
        return resolveDeclaredClassUsingString(
                "livephoto-gate",
                "camera_high_pixel_live_photo"
        );
    }

    String resolveAiCompositionGateClass() {
        String found = resolveDeclaredClassUsingString(
                "ai-composition-realscene",
                "com.oplus.feature.aicomposition.realscene.support"
        );
        if (found != null) {
            return found;
        }
        return resolveDeclaredClassUsingString(
                "ai-composition-enable",
                "com.oplus.ai.composition.enable"
        );
    }

    private boolean hasClass(String name) {
        return classCache.computeIfAbsent(name, key -> {
            try {
                Class.forName(key, false, classLoader);
                return true;
            } catch (Throwable ignored) {
                return false;
            }
        });
    }

    private String resolveDeclaredClassUsingString(String cacheKey, String anchor) {
        if (!BuildConfig.ENABLE_COLOROS17_COMPAT) {
            return null;
        }
        if (semanticClassCache.containsKey(cacheKey)) {
            String cached = semanticClassCache.get(cacheKey);
            return cached == null || cached.isEmpty() ? null : cached;
        }

        String result = null;
        try {
            if (ensureDexKit() && apkPath != null && !apkPath.isEmpty()) {
                try (DexKitBridge bridge = DexKitBridge.create(apkPath)) {
                    var methods = bridge.findMethod(
                            FindMethod.create().matcher(
                                    MethodMatcher.create().usingStrings(anchor)
                            )
                    );
                    for (var method : methods) {
                        String declared = method.getDeclaredClassName();
                        if (declared != null && declared.startsWith("com.oplus.")) {
                            result = declared;
                            break;
                        }
                    }
                }
            }
        } catch (Throwable t) {
            log("DexKit resolve failed for " + anchor + ": " + t.getClass().getSimpleName());
        }

        semanticClassCache.put(cacheKey, result == null ? "" : result);
        if (result != null) {
            log("semantic anchor " + anchor + " -> " + result);
        }
        return result;
    }

    private synchronized boolean ensureDexKit() {
        if (dexKitLoadAttempted) {
            return dexKitAvailable;
        }
        dexKitLoadAttempted = true;
        try {
            System.loadLibrary("dexkit");
            dexKitAvailable = true;
        } catch (Throwable t) {
            dexKitAvailable = false;
            log("DexKit native library unavailable: " + t.getClass().getSimpleName());
        }
        return dexKitAvailable;
    }

    private void logSummary() {
        log("compat resolver: modernAI=" + supportsModernAiComposition()
                + ", legacyAI=" + supportsLegacyAiCaptureGuide()
                + ", livePhoto=" + supportsLivePhotoStack()
                + ", filterStack=" + supportsModernFilterStack()
                + ", meisheLut=" + hasMeisheLutDirectory());
        if (BuildConfig.ENABLE_COLOROS17_COMPAT) {
            resolveAiCompositionGateClass();
            resolveLivePhotoGateClass();
        }
    }

    private static void log(String message) {
        XposedBridge.log("CameraBoost[ColorOS17]: " + message);
    }
}
