package dev.cameraboost.oplus10bit;

import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

final class VendorTagGateHook {
    private static final String[] STRING_CONFIG_CLASSES = {
            "com.oplus.ocs.camera.appinterface.adapter.CameraAdapterUtils",
            "com.oplus.ocs.camera.consumer.apsAdapter.adapter.ApsUtils"
    };

    private static final String[] BOOLEAN_CONFIG_CLASSES = {
            "com.oplus.camera.configure.CameraConfig",
            "com.oppo.camera.aps.config.CameraConfig"
    };

    private static final Set<String> WATCHED_TAGS = ConcurrentHashMap.newKeySet();
    private static final Set<String> LOGGED = ConcurrentHashMap.newKeySet();

    static {
        WATCHED_TAGS.addAll(OplusFeatureCatalog.allOverrides().keySet());
    }

    private VendorTagGateHook() {}

    static void install(ClassLoader classLoader) {
        for (String className : STRING_CONFIG_CLASSES) {
            installStringGetter(classLoader, className);
        }
        for (String className : BOOLEAN_CONFIG_CLASSES) {
            installBooleanGetter(classLoader, className);
        }
        installFeatureValueLegalityHook(classLoader);
    }

    private static void installStringGetter(ClassLoader classLoader, String className) {
        try {
            Class<?> cls = XposedHelpers.findClass(className, classLoader);
            XposedBridge.hookAllMethods(cls, "getVendorTagConfig", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    String key = firstStringArg(param.args);
                    String forced = forcedStringValue(key);
                    if (forced != null) {
                        param.setResult(forced);
                    }
                }

                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    String key = firstStringArg(param.args);
                    if (!WATCHED_TAGS.contains(key)) {
                        return;
                    }
                    logOnce(className + "#getVendorTagConfig:" + key,
                            className + "#getVendorTagConfig " + key + " -> " + param.getResult());
                }
            });
            log("installed string vendor-tag getter hook on " + className);
        } catch (Throwable t) {
            log("string vendor-tag getter unavailable on " + className + ": "
                    + t.getClass().getSimpleName());
        }
    }

    private static void installBooleanGetter(ClassLoader classLoader, String className) {
        try {
            Class<?> cls = XposedHelpers.findClass(className, classLoader);
            XposedBridge.hookAllMethods(cls, "getConfigBooleanValue", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    String key = firstStringArg(param.args);
                    if (forcedBooleanValue(key)) {
                        param.setResult(true);
                    }
                }

                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    String key = firstStringArg(param.args);
                    if (!WATCHED_TAGS.contains(key)) {
                        return;
                    }
                    logOnce(className + "#getConfigBooleanValue:" + key,
                            className + "#getConfigBooleanValue " + key + " -> " + param.getResult());
                }
            });
            log("installed boolean camera-config hook on " + className);
        } catch (Throwable t) {
            log("boolean camera-config getter unavailable on " + className + ": "
                    + t.getClass().getSimpleName());
        }
    }

    private static void installFeatureValueLegalityHook(ClassLoader classLoader) {
        try {
            Class<?> cls = XposedHelpers.findClass(
                    "com.oplus.ocs.camera.configure.ConfigFeatureImpl",
                    classLoader
            );
            XposedBridge.hookAllMethods(cls, "isFeatureValueLegal", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (param.args == null || param.args.length < 2) {
                        return;
                    }
                    String key = param.args[0] instanceof String ? (String) param.args[0] : "";
                    Object value = param.args[1];

                    // OPCameraPro's existing 120 FPS compatibility path, now shared by
                    // both OnePlus and OPPO camera bases.
                    if (BuildConfig.ENABLE_ALL_SOFTWARE_CAPABILITIES
                            && "com.oplus.configure.video.fps".equals(key)
                            && "video_120fps".equals(String.valueOf(value))) {
                        param.setResult(true);
                    }
                }
            });
            log("installed ConfigFeatureImpl#isFeatureValueLegal compatibility hook");
        } catch (Throwable t) {
            log("ConfigFeatureImpl legality hook unavailable: " + t.getClass().getSimpleName());
        }
    }

    private static String forcedStringValue(String key) {
        if (key == null || key.isEmpty()) {
            return null;
        }

        if (OplusConfigPatcher.TAG_10BIT_HEIC.equals(key)
                && BuildConfig.ENABLE_10BIT_HEIC
                && FeaturePolicy.isTargetDevice()) {
            return "1";
        }
        if ((OplusConfigPatcher.TAG_HEIF_LIVE_PHOTO.equals(key)
                || OplusConfigPatcher.TAG_10BIT_LIVE_PHOTO.equals(key))
                && BuildConfig.ENABLE_10BIT_LIVE_PHOTO
                && FeaturePolicy.isTargetDevice()
                && ColorOS17CompatResolver.get().supportsLivePhotoStack()) {
            return "1";
        }

        OplusFeatureCatalog.OverrideValue override =
                OplusFeatureCatalog.overrideFor(key, ColorOS17CompatResolver.get());
        return override == null ? null : override.value;
    }

    private static boolean forcedBooleanValue(String key) {
        if (key == null || key.isEmpty()) {
            return false;
        }

        if (OplusConfigPatcher.TAG_10BIT_HEIC.equals(key)
                && BuildConfig.ENABLE_10BIT_HEIC
                && FeaturePolicy.isTargetDevice()) {
            return true;
        }
        if ((OplusConfigPatcher.TAG_HEIF_LIVE_PHOTO.equals(key)
                || OplusConfigPatcher.TAG_10BIT_LIVE_PHOTO.equals(key))
                && BuildConfig.ENABLE_10BIT_LIVE_PHOTO
                && FeaturePolicy.isTargetDevice()
                && ColorOS17CompatResolver.get().supportsLivePhotoStack()) {
            return true;
        }

        return OplusFeatureCatalog.shouldForceBoolean(
                key,
                ColorOS17CompatResolver.get()
        );
    }

    private static String firstStringArg(Object[] args) {
        if (args == null) {
            return "";
        }
        return Arrays.stream(args)
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .findFirst()
                .orElse("");
    }

    private static void logOnce(String identity, String message) {
        if (LOGGED.add(identity)) {
            log(message);
        }
    }

    private static void log(String message) {
        XposedBridge.log("CameraBoost: " + message);
    }
}
