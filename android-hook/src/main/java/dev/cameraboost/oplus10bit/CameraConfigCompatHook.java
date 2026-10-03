package dev.cameraboost.oplus10bit;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * ColorOS 17 moved most feature reads into com.oplus.camera.configure.CameraConfig
 * and obfuscated many accessor names. Resolve accessors by signature instead of
 * relying on method names.
 */
final class CameraConfigCompatHook {
    private static final String CAMERA_CONFIG =
            "com.oplus.camera.configure.CameraConfig";
    private static final Set<String> LOGGED = ConcurrentHashMap.newKeySet();

    private CameraConfigCompatHook() {}

    static void install(ClassLoader loader, RuntimeArchitecture runtime) {
        if (!runtime.modernCameraConfig) {
            log("modern CameraConfig unavailable; skipping signature-based config hook");
            return;
        }

        try {
            Class<?> cls = XposedHelpers.findClass(CAMERA_CONFIG, loader);
            int hooked = 0;

            for (Method method : cls.getDeclaredMethods()) {
                if (method.getReturnType() == Void.TYPE) {
                    continue;
                }

                int keyIndex = firstStringParameter(method.getParameterTypes());
                if (keyIndex < 0) {
                    continue;
                }

                final Class<?> returnType = method.getReturnType();
                final int capturedKeyIndex = keyIndex;
                final String methodName = method.getName();

                XposedBridge.hookMethod(method, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        if (!BuildConfig.ENABLE_FULL_UNLOCK || !FeaturePolicy.isTargetDevice()) {
                            return;
                        }

                        if (param.args == null || capturedKeyIndex >= param.args.length) {
                            return;
                        }

                        Object keyObject = param.args[capturedKeyIndex];
                        if (!(keyObject instanceof String)) {
                            return;
                        }

                        String key = (String) keyObject;
                        CapabilityValuePolicy.OverrideSpec scalar =
                                CapabilityValuePolicy.find(key, runtime);

                        if (scalar != null) {
                            Object converted = convertScalar(scalar.value, returnType);
                            if (converted != null) {
                                param.setResult(converted);
                                logOnce(
                                        "scalar:" + methodName + ":" + key,
                                        "CameraConfig#" + methodName + " " + key
                                                + " -> " + printable(converted)
                                );
                                return;
                            }
                        }

                        if (!CapabilityKeyPolicy.shouldForceBoolean(key, runtime)) {
                            return;
                        }

                        Object enabled = enabledValue(returnType);
                        if (enabled != null) {
                            param.setResult(enabled);
                            logOnce(
                                    "gate:" + methodName + ":" + key,
                                    "CameraConfig#" + methodName + " " + key
                                            + " -> " + printable(enabled)
                            );
                        }
                    }
                });
                hooked++;
            }

            log("installed signature-based ColorOS 17 CameraConfig hook on "
                    + hooked + " accessors");
        } catch (Throwable t) {
            log("CameraConfig compatibility hook unavailable: "
                    + t.getClass().getSimpleName() + ": " + t.getMessage());
        }
    }

    private static int firstStringParameter(Class<?>[] parameterTypes) {
        for (int i = 0; i < parameterTypes.length; i++) {
            if (parameterTypes[i] == String.class) {
                return i;
            }
        }
        return -1;
    }

    private static Object enabledValue(Class<?> type) {
        if (type == Boolean.TYPE || type == Boolean.class) return true;
        if (type == String.class) return "1";
        if (type == Integer.TYPE || type == Integer.class) return 1;
        if (type == Long.TYPE || type == Long.class) return 1L;
        if (type == Float.TYPE || type == Float.class) return 1.0f;
        if (type == Double.TYPE || type == Double.class) return 1.0d;
        return null;
    }

    private static Object convertScalar(String raw, Class<?> type) {
        try {
            if (type == String.class) return raw;
            if (type == Integer.TYPE || type == Integer.class) {
                return Integer.parseInt(first(raw));
            }
            if (type == Long.TYPE || type == Long.class) {
                return Long.parseLong(first(raw));
            }
            if (type == Float.TYPE || type == Float.class) {
                return Float.parseFloat(first(raw));
            }
            if (type == Double.TYPE || type == Double.class) {
                return Double.parseDouble(first(raw));
            }
            if (type == Boolean.TYPE || type == Boolean.class) {
                String v = first(raw);
                return "1".equals(v) || "true".equalsIgnoreCase(v) || "on".equalsIgnoreCase(v);
            }
            if (type == int[].class) {
                String[] parts = split(raw);
                int[] out = new int[parts.length];
                for (int i = 0; i < parts.length; i++) out[i] = Integer.parseInt(parts[i]);
                return out;
            }
            if (type == float[].class) {
                String[] parts = split(raw);
                float[] out = new float[parts.length];
                for (int i = 0; i < parts.length; i++) out[i] = Float.parseFloat(parts[i]);
                return out;
            }
            if (type == String[].class) {
                return split(raw);
            }
        } catch (Throwable ignored) {
            return null;
        }
        return null;
    }

    private static String first(String raw) {
        String[] parts = split(raw);
        return parts.length == 0 ? raw.trim() : parts[0];
    }

    private static String[] split(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return new String[0];
        }
        String[] parts = raw.split(",");
        for (int i = 0; i < parts.length; i++) {
            parts[i] = parts[i].trim();
        }
        return parts;
    }

    private static String printable(Object value) {
        if (value instanceof int[]) {
            return java.util.Arrays.toString((int[]) value);
        }
        if (value instanceof float[]) {
            return java.util.Arrays.toString((float[]) value);
        }
        if (value instanceof Object[]) {
            return java.util.Arrays.toString((Object[]) value);
        }
        return String.valueOf(value);
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
