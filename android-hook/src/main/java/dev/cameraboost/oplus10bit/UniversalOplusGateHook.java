package dev.cameraboost.oplus10bit;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

final class UniversalOplusGateHook {
    private static final String[] CANDIDATE_CLASSES = {
            "com.oplus.camera.configure.CameraConfig",
            "com.oppo.camera.aps.config.CameraConfig",
            "com.oplus.ocs.camera.configure.ConfigFeatureImpl",
            "com.oplus.ocs.camera.appinterface.adapter.CameraAdapterUtils",
            "com.oplus.ocs.camera.consumer.apsAdapter.adapter.ApsUtils"
    };

    private static final Set<String> LOGGED = ConcurrentHashMap.newKeySet();

    private UniversalOplusGateHook() {}

    static void install(ClassLoader classLoader) {
        for (String className : CANDIDATE_CLASSES) {
            try {
                Class<?> cls = XposedHelpers.findClass(className, classLoader);
                installOnClass(cls);
                log("installed semantic gate scan on " + className);
            } catch (Throwable t) {
                log("gate class unavailable: " + className + " (" + t.getClass().getSimpleName() + ")");
            }
        }
    }

    private static void installOnClass(Class<?> cls) {
        for (Method method : cls.getDeclaredMethods()) {
            if (!hasStringParameter(method)) continue;

            Class<?> returnType = method.getReturnType();
            if (returnType == boolean.class || returnType == Boolean.class) {
                XposedBridge.hookMethod(method, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        String key = firstStringArg(param.args);
                        if (OplusUnlockCatalog.shouldForceBoolean(key)) {
                            param.setResult(true);
                            logOnce(cls.getName() + "#" + method.getName() + ":" + key,
                                    "force boolean gate true: " + key + " via "
                                            + cls.getSimpleName() + "#" + method.getName());
                        }
                    }
                });
            } else if (returnType == String.class && looksLikeConfigGetter(method.getName())) {
                XposedBridge.hookMethod(method, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        String key = firstStringArg(param.args);
                        if (OplusUnlockCatalog.shouldForceBoolean(key)) {
                            param.setResult("1");
                            logOnce(cls.getName() + "#" + method.getName() + ":" + key,
                                    "force string gate 1: " + key + " via "
                                            + cls.getSimpleName() + "#" + method.getName());
                        }
                    }
                });
            }
        }
    }

    private static boolean hasStringParameter(Method method) {
        return Arrays.stream(method.getParameterTypes()).anyMatch(type -> type == String.class);
    }

    private static boolean looksLikeConfigGetter(String name) {
        String lower = name.toLowerCase();
        return lower.contains("config") || lower.contains("vendor") || lower.startsWith("get");
    }

    private static String firstStringArg(Object[] args) {
        if (args == null) return "";
        return Arrays.stream(args)
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .findFirst()
                .orElse("");
    }

    private static void logOnce(String id, String message) {
        if (LOGGED.add(id)) log(message);
    }

    private static void log(String message) {
        XposedBridge.log("CameraBoost17[Gate]: " + message);
    }
}
