package dev.cameraboost.oplus10bit;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

final class FeatureValueLegalHook {
    private static final String CONFIG_FEATURE_IMPL =
            "com.oplus.ocs.camera.configure.ConfigFeatureImpl";

    private FeatureValueLegalHook() {}

    static void install(ClassLoader loader, RuntimeArchitecture runtime) {
        if (!runtime.configFeatureImpl) {
            log("ConfigFeatureImpl unavailable; skipping value-legal hook");
            return;
        }

        try {
            Class<?> cls = XposedHelpers.findClass(CONFIG_FEATURE_IMPL, loader);
            XposedBridge.hookAllMethods(cls, "isFeatureValueLegal", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (!BuildConfig.ENABLE_FULL_UNLOCK || !FeaturePolicy.isTargetDevice()) {
                        return;
                    }

                    String key = firstString(param.args);
                    Object value = secondValue(param.args, key);

                    if (CapabilityKeyPolicy.shouldForceValueLegal(key, value)) {
                        param.setResult(true);
                        logOnce(key + "=" + value,
                                "accepted feature value " + key + "=" + value);
                    }
                }
            });
            log("installed ConfigFeatureImpl#isFeatureValueLegal compatibility hook");
        } catch (Throwable t) {
            log("value-legal hook unavailable: " + t.getClass().getSimpleName()
                    + ": " + t.getMessage());
        }
    }

    private static String firstString(Object[] args) {
        if (args == null) {
            return "";
        }
        for (Object arg : args) {
            if (arg instanceof String) {
                return (String) arg;
            }
        }
        return "";
    }

    private static Object secondValue(Object[] args, String key) {
        if (args == null) {
            return null;
        }

        boolean seenKey = false;
        for (Object arg : args) {
            if (!seenKey && arg instanceof String && key.equals(arg)) {
                seenKey = true;
                continue;
            }
            if (seenKey) {
                return arg;
            }
        }
        return args.length > 1 ? args[1] : null;
    }

    private static final java.util.Set<String> LOGGED =
            java.util.concurrent.ConcurrentHashMap.newKeySet();

    private static void logOnce(String key, String message) {
        if (LOGGED.add(key)) {
            log(message);
        }
    }

    private static void log(String message) {
        XposedBridge.log("CameraBoostFull: " + message);
    }
}
