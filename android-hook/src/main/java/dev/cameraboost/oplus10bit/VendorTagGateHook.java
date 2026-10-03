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

    private static final Set<String> LOGGED = ConcurrentHashMap.newKeySet();

    private VendorTagGateHook() {}

    static void install(ClassLoader classLoader, RuntimeArchitecture runtime) {
        for (String className : STRING_CONFIG_CLASSES) {
            installStringGetter(classLoader, className, runtime);
        }
        for (String className : BOOLEAN_CONFIG_CLASSES) {
            installBooleanGetter(classLoader, className, runtime);
        }
    }

    private static void installStringGetter(
            ClassLoader classLoader,
            String className,
            RuntimeArchitecture runtime
    ) {
        try {
            Class<?> cls = XposedHelpers.findClass(className, classLoader);
            XposedBridge.hookAllMethods(cls, "getVendorTagConfig", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    String key = firstStringArg(param.args);

                    if (BuildConfig.ENABLE_FULL_UNLOCK && FeaturePolicy.isTargetDevice()) {
                        CapabilityValuePolicy.OverrideSpec value =
                                CapabilityValuePolicy.find(key, runtime);
                        if (value != null) {
                            param.setResult(value.value);
                            logOnce("force-value:" + key,
                                    "forced scalar feature value " + key + " -> "
                                            + value.value + " via " + className);
                            return;
                        }
                    }

                    if (shouldForce(key, runtime)) {
                        param.setResult("1");
                        logOnce("force-string:" + key,
                                "forced string feature gate " + key + " -> 1 via " + className);
                    }
                }

                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    String key = firstStringArg(param.args);
                    if (isInteresting(key, runtime)) {
                        logOnce(className + "#getVendorTagConfig:" + key,
                                className + "#getVendorTagConfig " + key + " -> " + param.getResult());
                    }
                }
            });
            log("installed string vendor-tag getter hook on " + className);
        } catch (Throwable t) {
            log("string vendor-tag getter unavailable on " + className + ": "
                    + t.getClass().getSimpleName());
        }
    }

    private static void installBooleanGetter(
            ClassLoader classLoader,
            String className,
            RuntimeArchitecture runtime
    ) {
        try {
            Class<?> cls = XposedHelpers.findClass(className, classLoader);
            XposedBridge.hookAllMethods(cls, "getConfigBooleanValue", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    String key = firstStringArg(param.args);
                    if (shouldForce(key, runtime)) {
                        param.setResult(true);
                        logOnce("force-bool:" + key,
                                "forced boolean feature gate " + key + " -> true via " + className);
                    }
                }

                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    String key = firstStringArg(param.args);
                    if (isInteresting(key, runtime)) {
                        logOnce(className + "#getConfigBooleanValue:" + key,
                                className + "#getConfigBooleanValue " + key + " -> " + param.getResult());
                    }
                }
            });
            log("installed boolean camera-config hook on " + className);
        } catch (Throwable t) {
            log("boolean camera-config getter unavailable on " + className + ": "
                    + t.getClass().getSimpleName());
        }
    }

    private static boolean shouldForce(String key, RuntimeArchitecture runtime) {
        if (!FeaturePolicy.isTargetDevice()) {
            return false;
        }

        if (BuildConfig.ENABLE_FULL_UNLOCK
                && CapabilityKeyPolicy.shouldForceBoolean(key, runtime)) {
            return true;
        }

        if (BuildConfig.ENABLE_10BIT_HEIC
                && OplusConfigPatcher.TAG_10BIT_HEIC.equals(key)) {
            return true;
        }

        return BuildConfig.ENABLE_10BIT_LIVE_PHOTO
                && (OplusConfigPatcher.TAG_HEIF_LIVE_PHOTO.equals(key)
                || OplusConfigPatcher.TAG_10BIT_LIVE_PHOTO.equals(key));
    }

    private static boolean isInteresting(String key, RuntimeArchitecture runtime) {
        return OplusConfigPatcher.TAG_10BIT_HEIC.equals(key)
                || OplusConfigPatcher.TAG_HEIF_LIVE_PHOTO.equals(key)
                || OplusConfigPatcher.TAG_10BIT_LIVE_PHOTO.equals(key)
                || OplusConfigPatcher.TAG_VIDEO_10BIT.equals(key)
                || CapabilityKeyPolicy.shouldForceBoolean(key, runtime)
                || CapabilityValuePolicy.find(key, runtime) != null;
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
        XposedBridge.log("CameraBoostFull: " + message);
    }
}
