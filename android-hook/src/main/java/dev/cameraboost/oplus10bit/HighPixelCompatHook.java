package dev.cameraboost.oplus10bit;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * Narrow ColorOS 17 HighPixel compatibility adapter.
 *
 * This intentionally does NOT restore the old universal gate layer. It only
 * answers four exact HighPixel/TurboRAW feature keys that were observed on the
 * supplied Camera 7.013.30 build and on PJZ110's CameraUnit/APS configuration.
 */
final class HighPixelCompatHook {
    private static final String CAMERA_CONFIG = "com.oplus.camera.configure.CameraConfig";

    private static final Map<String, Object> EXACT_VALUES;
    private static final Set<String> LOGGED = ConcurrentHashMap.newKeySet();

    static {
        Map<String, Object> m = new HashMap<>();
        // Core 25MP/TurboRAW exposure.
        m.put("com.oplus.turboraw.re.support", Boolean.TRUE);

        // HighPixel mode-specific capabilities previously observed as required
        // on Camera 7.013.30. These are deliberately scoped to exact keys.
        m.put("com.oplus.high.pixel.to.support", Integer.valueOf(1));
        m.put("com.oplus.feature.highpixel.merge.support", Integer.valueOf(1));
        m.put("com.oplus.camera.high.pixel.mode.4k.live.dynamic.preview.support",
                Boolean.TRUE);

        EXACT_VALUES = Collections.unmodifiableMap(m);
    }

    private HighPixelCompatHook() {}

    static void install(ClassLoader classLoader) {
        if (!FeaturePolicy.isTargetDevice()) {
            CameraBoostLog.log("HighPixel compat skipped: target-device guard");
            return;
        }

        Class<?> cls;
        try {
            cls = XposedHelpers.findClassIfExists(CAMERA_CONFIG, classLoader);
        } catch (Throwable t) {
            cls = null;
        }
        if (cls == null) {
            CameraBoostLog.log("HighPixel compat unavailable: CameraConfig missing");
            return;
        }

        int installed = 0;
        for (Method method : cls.getDeclaredMethods()) {
            if (!hasStringParameter(method)) {
                continue;
            }
            Class<?> returnType = method.getReturnType();
            if (!isSupportedReturnType(returnType)) {
                continue;
            }

            try {
                method.setAccessible(true);
                XposedBridge.hookMethod(method, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        String key = exactHighPixelKey(param.args);
                        if (key == null) {
                            return;
                        }

                        Object value = valueForReturnType(EXACT_VALUES.get(key), returnType);
                        if (value == null) {
                            return;
                        }

                        param.setResult(value);
                        if (LOGGED.add(key)) {
                            CameraBoostLog.log("HighPixel exact gate: " + key
                                    + " via " + CAMERA_CONFIG + "#" + method.getName()
                                    + " -> " + value);
                        }
                    }
                });
                installed++;
            } catch (Throwable t) {
                CameraBoostLog.log("HighPixel method hook skipped: "
                        + method.getName() + " / " + t.getClass().getSimpleName());
            }
        }

        CameraBoostLog.log("HighPixel compat installed; methods=" + installed
                + ", exactKeys=" + EXACT_VALUES.keySet());
    }

    private static boolean hasStringParameter(Method method) {
        for (Class<?> type : method.getParameterTypes()) {
            if (type == String.class) return true;
        }
        return false;
    }

    private static boolean isSupportedReturnType(Class<?> type) {
        return type == boolean.class || type == Boolean.class
                || type == int.class || type == Integer.class
                || type == long.class || type == Long.class;
    }

    private static String exactHighPixelKey(Object[] args) {
        if (args == null) return null;
        for (Object arg : args) {
            if (arg instanceof String && EXACT_VALUES.containsKey((String) arg)) {
                return (String) arg;
            }
        }
        return null;
    }

    private static Object valueForReturnType(Object configured, Class<?> type) {
        if (type == boolean.class || type == Boolean.class) {
            if (configured instanceof Boolean) return configured;
            if (configured instanceof Number) {
                return ((Number) configured).intValue() != 0;
            }
        }
        if (type == int.class || type == Integer.class) {
            if (configured instanceof Number) return ((Number) configured).intValue();
            if (configured instanceof Boolean) return ((Boolean) configured) ? 1 : 0;
        }
        if (type == long.class || type == Long.class) {
            if (configured instanceof Number) return ((Number) configured).longValue();
            if (configured instanceof Boolean) return ((Boolean) configured) ? 1L : 0L;
        }
        return null;
    }
}
