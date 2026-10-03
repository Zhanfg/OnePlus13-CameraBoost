package dev.cameraboost.oplus10bit;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * Hooks stable OPlus configuration surfaces rather than obfuscated camera mode classes.
 * A method is only altered when one of its String arguments is an exact registered gate.
 */
final class OplusUniversalGateHook {
    private static final String[] CONFIG_CLASSES = {
            "com.oplus.ocs.camera.appinterface.adapter.CameraAdapterUtils",
            "com.oplus.ocs.camera.consumer.apsAdapter.adapter.ApsUtils",
            "com.oplus.camera.configure.CameraConfig",
            "com.oppo.camera.aps.config.CameraConfig",
            "com.oplus.ocs.camera.configure.ConfigFeatureImpl"
    };

    private static final Set<String> HOOKED = ConcurrentHashMap.newKeySet();
    private static final Set<String> LOGGED_KEYS = ConcurrentHashMap.newKeySet();

    private OplusUniversalGateHook() {}

    static void install(ClassLoader classLoader, OplusCapabilityResolver resolver) {
        for (String className : CONFIG_CLASSES) {
            hookConfigClass(classLoader, className, resolver);
        }
    }

    private static void hookConfigClass(
            ClassLoader classLoader, String className, OplusCapabilityResolver resolver) {
        Class<?> cls;
        try {
            cls = XposedHelpers.findClassIfExists(className, classLoader);
        } catch (Throwable t) {
            cls = null;
        }
        if (cls == null) {
            return;
        }

        int installed = 0;
        for (Method method : cls.getDeclaredMethods()) {
            if ("getVendorTagConfig".equals(method.getName())
                    || "getConfigBooleanValue".equals(method.getName())) {
                continue;
            }
            if (!hasStringParameter(method) || !isSupportedReturnType(method.getReturnType())) {
                continue;
            }
            String identity = className + "#" + method.toGenericString();
            if (!HOOKED.add(identity)) {
                continue;
            }
            try {
                XposedBridge.hookMethod(method, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        String key = findRegisteredKey(param.args);
                        if (key == null || !OplusFeatureGateRegistry.shouldForce(key, resolver)) {
                            return;
                        }
                        OplusFeatureGateRegistry.GateSpec spec = OplusFeatureGateRegistry.get(key);
                        Object forced = OplusFeatureGateRegistry.forcedValueForReturnType(
                                spec, method.getReturnType());
                        if (forced != null) {
                            param.setResult(forced);
                            if (LOGGED_KEYS.add(key)) {
                                CameraBoostLog.log("gate forced: " + key + " via "
                                        + className + "#" + method.getName()
                                        + " -> " + forced);
                            }
                        }
                    }
                });
                installed++;
            } catch (Throwable t) {
                CameraBoostLog.log("gate hook skipped " + className + "#"
                        + method.getName() + ": " + t.getClass().getSimpleName());
            }
        }
        if (installed > 0) {
            CameraBoostLog.log("universal gate hooks installed: " + className
                    + " methods=" + installed);
        }
    }

    private static boolean hasStringParameter(Method method) {
        for (Class<?> type : method.getParameterTypes()) {
            if (type == String.class) {
                return true;
            }
        }
        return false;
    }

    private static boolean isSupportedReturnType(Class<?> type) {
        return type == boolean.class || type == Boolean.class
                || type == int.class || type == Integer.class
                || type == long.class || type == Long.class
                || type == float.class || type == Float.class
                || type == double.class || type == Double.class
                || type == String.class;
    }

    private static String findRegisteredKey(Object[] args) {
        if (args == null) {
            return null;
        }
        for (Object arg : args) {
            if (arg instanceof String) {
                String value = (String) arg;
                if (OplusFeatureGateRegistry.knows(value)) {
                    return value;
                }
            }
        }
        return null;
    }
}
