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
        WATCHED_TAGS.add(OplusConfigPatcher.TAG_10BIT_HEIC);
        WATCHED_TAGS.add(OplusConfigPatcher.TAG_HEIF_LIVE_PHOTO);
        WATCHED_TAGS.add(OplusConfigPatcher.TAG_10BIT_LIVE_PHOTO);
        WATCHED_TAGS.add(OplusConfigPatcher.TAG_VIDEO_10BIT);
    }

    private VendorTagGateHook() {}

    static void install(ClassLoader classLoader) {
        for (String className : STRING_CONFIG_CLASSES) {
            installStringGetter(classLoader, className);
        }
        for (String className : BOOLEAN_CONFIG_CLASSES) {
            installBooleanGetter(classLoader, className);
        }
    }

    private static void installStringGetter(ClassLoader classLoader, String className) {
        try {
            Class<?> cls = XposedHelpers.findClass(className, classLoader);
            XposedBridge.hookAllMethods(cls, "getVendorTagConfig", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    String key = firstStringArg(param.args);
                    if (!OplusConfigPatcher.TAG_10BIT_HEIC.equals(key)) {
                        return;
                    }

                    if (canEnable10BitStill()) {
                        param.setResult("1");
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
                    if (!OplusConfigPatcher.TAG_10BIT_HEIC.equals(key)) {
                        return;
                    }

                    if (canEnable10BitStill()) {
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

    private static boolean canEnable10BitStill() {
        return FeaturePolicy.canEnable10BitStill();
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
        XposedBridge.log("CameraBoost10Bit: " + message);
    }
}
