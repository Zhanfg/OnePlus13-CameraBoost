package dev.cameraboost.oplus10bit;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * Stable ColorOS 17 CameraUnit/IPU compatibility surfaces.
 *
 * This deliberately avoids private UpdateHelper fields and obfuscated mode classes.
 */
final class OplusCameraUnitCompatHook {
    private static final String IPU_FEATURES =
            "com.oplus.ocs.camera.ipusdk.IPUFeatures";
    private static final String CAMERA_UNIT_CLIENT =
            "com.oplus.ocs.camera.CameraUnitClient";

    private static final Set<String> LOGGED = ConcurrentHashMap.newKeySet();

    private OplusCameraUnitCompatHook() {}

    static void install(ClassLoader classLoader, OplusCapabilityResolver resolver) {
        installIpuConfigHook(classLoader, resolver);
        installCameraUnitFeatureHook(classLoader, resolver);
        installCameraUnitInventoryObservers(classLoader);
    }

    private static void installIpuConfigHook(
            ClassLoader classLoader,
            OplusCapabilityResolver resolver
    ) {
        try {
            Class<?> cls = XposedHelpers.findClass(IPU_FEATURES, classLoader);
            XposedBridge.hookAllMethods(cls, "getValidConfigData", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    String configName = stringArg(param.args);
                    Object result = param.getResult();

                    if (configName != null && configName.contains("camera_unit_config")) {
                        String status = result instanceof String
                                ? "len=" + ((String) result).length()
                                : "null/type=" + (result == null
                                ? "null" : result.getClass().getName());
                        logOnce("unit-config:" + status,
                                "IPU camera_unit_config result " + status);
                    }

                    if (!(result instanceof String)) {
                        return;
                    }
                    String original = (String) result;
                    if (!original.contains("\"VendorTag\"")) {
                        return;
                    }

                    ColorOS17ConfigPatcher.Result patched =
                            ColorOS17ConfigPatcher.patch(original, resolver);
                    if (patched.parsed && patched.changed) {
                        param.setResult(patched.output);
                        log("IPU config patched: " + configName
                                + ", existing=" + patched.enabledExisting
                                + ", synthesized=" + patched.synthesized);
                    }
                }
            });
            log("installed IPUFeatures#getValidConfigData compatibility hook");
        } catch (Throwable t) {
            log("IPUFeatures config path unavailable: " + t.getClass().getSimpleName());
        }
    }

    private static void installCameraUnitFeatureHook(
            ClassLoader classLoader,
            OplusCapabilityResolver resolver
    ) {
        try {
            Class<?> cls = XposedHelpers.findClass(CAMERA_UNIT_CLIENT, classLoader);
            XposedBridge.hookAllMethods(cls, "hasFeature", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    String key = stringArg(param.args);
                    if (key != null && resolver.shouldForceBoolean(key)) {
                        param.setResult(true);
                        logOnce("cameraunit-force:" + key,
                                "CameraUnit hasFeature forced: " + key);
                    }
                }

                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    String key = stringArg(param.args);
                    if (key == null) return;
                    if (resolver.watchedTags().contains(key)
                            || OplusFeatureGateRegistry.knows(key)) {
                        logOnce("cameraunit-observe:" + key,
                                "CameraUnit hasFeature " + key + " -> " + param.getResult());
                    }
                }
            });
            log("installed CameraUnitClient#hasFeature compatibility hook");
        } catch (Throwable t) {
            log("CameraUnit hasFeature path unavailable: " + t.getClass().getSimpleName());
        }
    }

    private static void installCameraUnitInventoryObservers(ClassLoader classLoader) {
        try {
            Class<?> cls = XposedHelpers.findClass(CAMERA_UNIT_CLIENT, classLoader);

            XposedBridge.hookAllMethods(cls, "isDeviceSupportCameraUnit", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    logOnce("cameraunit-supported",
                            "CameraUnit device support -> " + param.getResult());
                }
            });

            XposedBridge.hookAllMethods(cls, "getAllSupportCameraMode", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    Object result = param.getResult();
                    int size = result instanceof Map ? ((Map<?, ?>) result).size() : -1;
                    logOnce("cameraunit-modes:" + size,
                            "CameraUnit support modes size=" + size);
                }
            });

            XposedBridge.hookAllMethods(cls, "getAllSupportCameraType", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    Object result = param.getResult();
                    int size = result instanceof java.util.List
                            ? ((java.util.List<?>) result).size() : -1;
                    logOnce("cameraunit-types:" + size,
                            "CameraUnit support camera types size=" + size);
                }
            });

            log("installed CameraUnit inventory observers");
        } catch (Throwable t) {
            log("CameraUnit inventory path unavailable: " + t.getClass().getSimpleName());
        }
    }

    private static String stringArg(Object[] args) {
        if (args == null) return null;
        for (Object arg : args) {
            if (arg instanceof String) return (String) arg;
        }
        return null;
    }

    private static void logOnce(String identity, String message) {
        if (LOGGED.add(identity)) log(message);
    }

    private static void log(String message) {
        XposedBridge.log("CameraBoost[CameraUnit]: " + message);
    }
}
