package dev.cameraboost.oplus10bit;

import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * ColorOS 17 CameraUnit compatibility layer.
 *
 * CameraUnit is initialized asynchronously on newer OPlus camera builds. Do not
 * snapshot a "camera_unit_config" object during Application.onCreate; resolve the
 * feature at the actual call site so initialization order cannot leave a permanent
 * null cache.
 */
final class CameraUnitCompatHook {
    private static final String CAMERA_UNIT_CLIENT =
            "com.oplus.ocs.camera.CameraUnitClient";
    private static final String CAMERA_UNIT_ALGO_CONFIG =
            "com.oplus.ocs.camera.appinterface.adapter.CameraUnitAlgoSwitchConfig";

    private static final Set<String> LOGGED = ConcurrentHashMap.newKeySet();

    private CameraUnitCompatHook() {}

    static void install(ClassLoader loader, RuntimeArchitecture runtime) {
        installHasFeature(loader, runtime);
        installSupportCameraFeature(loader, runtime);
    }

    private static void installHasFeature(ClassLoader loader, RuntimeArchitecture runtime) {
        try {
            Class<?> cls = XposedHelpers.findClass(CAMERA_UNIT_CLIENT, loader);
            XposedBridge.hookAllMethods(cls, "hasFeature", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (!BuildConfig.ENABLE_FULL_UNLOCK || !FeaturePolicy.isTargetDevice()) {
                        return;
                    }
                    String key = firstString(param.args);
                    if (CapabilityKeyPolicy.shouldForceBoolean(key, runtime)) {
                        param.setResult(true);
                        logOnce("client:" + key,
                                "CameraUnitClient#hasFeature " + key + " -> true");
                    }
                }

                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    String key = firstString(param.args);
                    if (!key.isEmpty() && CapabilityKeyPolicy.shouldForceBoolean(key, runtime)) {
                        logOnce("client-result:" + key,
                                "CameraUnitClient#hasFeature resolved " + key
                                        + " -> " + param.getResult());
                    }
                }
            });
            log("installed CameraUnitClient#hasFeature runtime hook");
        } catch (Throwable t) {
            log("CameraUnitClient#hasFeature unavailable: "
                    + t.getClass().getSimpleName());
        }
    }

    private static void installSupportCameraFeature(
            ClassLoader loader,
            RuntimeArchitecture runtime
    ) {
        try {
            Class<?> cls = XposedHelpers.findClass(CAMERA_UNIT_ALGO_CONFIG, loader);
            XposedBridge.hookAllMethods(cls, "getSupportCameraFeature", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (!BuildConfig.ENABLE_FULL_UNLOCK || !FeaturePolicy.isTargetDevice()) {
                        return;
                    }
                    String key = firstString(param.args);
                    if (CapabilityKeyPolicy.shouldForceBoolean(key, runtime)) {
                        param.setResult(true);
                        logOnce("feature:" + key,
                                "CameraUnitAlgoSwitchConfig#getSupportCameraFeature "
                                        + key + " -> true");
                    }
                }
            });
            log("installed CameraUnitAlgoSwitchConfig#getSupportCameraFeature hook");
        } catch (Throwable t) {
            log("CameraUnit support-feature path unavailable: "
                    + t.getClass().getSimpleName());
        }
    }

    private static String firstString(Object[] args) {
        if (args == null) {
            return "";
        }
        return Arrays.stream(args)
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .findFirst()
                .orElse("");
    }

    private static void logOnce(String key, String message) {
        if (LOGGED.add(key)) {
            log(message);
        }
    }

    private static void log(String message) {
        XposedBridge.log("CameraBoostFull: " + message);
    }
}
