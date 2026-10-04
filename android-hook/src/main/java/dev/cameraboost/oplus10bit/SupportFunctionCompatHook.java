package dev.cameraboost.oplus10bit;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * Safe ColorOS 17 getSupportFunction hook.
 *
 * IMPORTANT: owner discovery is performed offline from the supplied Camera 7.013.30 APK.
 * No DEX files are read or parsed in the camera process at startup.
 */
final class SupportFunctionCompatHook {
    private static final Set<String> HOOKED = ConcurrentHashMap.newKeySet();

    // High-confidence camera mode owners extracted offline from Camera 7.013.30.
    // Deliberately excludes callbacks, interfaces, test adapters and helper-only classes.
    private static final List<String> OWNERS = Arrays.asList(
            "com.oplus.camera.module.BaseMode",
            "com.oplus.camera.module.a",
            "gm.q2",
            "gm.e3",
            "rm.c",
            "rm.d",
            "rm.f",
            "rm.g",
            "rm.h",
            "rm.m",
            "rm.o",
            "rm.p",
            "rm.q",
            "rm.q0",
            "im.c1",
            "im.d3",
            "im.e0",
            "im.f0",
            "im.f3",
            "im.h0",
            "im.h1",
            "im.h3",
            "im.i2",
            "im.i3",
            "im.j0",
            "im.j1",
            "im.k",
            "im.k0",
            "im.l",
            "im.m",
            "im.m3",
            "im.p",
            "im.p1",
            "im.q",
            "im.q0",
            "im.r",
            "im.r0",
            "im.s2",
            "im.u2",
            "im.v2",
            "im.w2",
            "im.x",
            "im.y"
    );

    private SupportFunctionCompatHook() {}

    static void install(
            ClassLoader classLoader,
            OplusCapabilityResolver resolver
    ) {
        long start = android.os.SystemClock.elapsedRealtime();
        int installed = 0;

        for (String className : OWNERS) {
            Class<?> cls;
            try {
                cls = XposedHelpers.findClassIfExists(className, classLoader);
            } catch (Throwable t) {
                cls = null;
            }
            if (cls == null) {
                continue;
            }

            Method[] methods;
            try {
                methods = cls.getDeclaredMethods();
            } catch (Throwable t) {
                CameraBoostLog.log("support owner inspect skipped " + className + ": "
                        + t.getClass().getSimpleName());
                continue;
            }

            for (Method method : methods) {
                if (!"getSupportFunction".equals(method.getName())
                        || (method.getReturnType() != boolean.class
                        && method.getReturnType() != Boolean.class)) {
                    continue;
                }

                boolean hasString = false;
                for (Class<?> p : method.getParameterTypes()) {
                    if (p == String.class) {
                        hasString = true;
                        break;
                    }
                }
                if (!hasString) {
                    continue;
                }

                String identity = className + "#" + method.toGenericString();
                if (!HOOKED.add(identity)) {
                    continue;
                }

                try {
                    method.setAccessible(true);
                    XposedBridge.hookMethod(method, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            try {
                                String key = firstString(param.args);
                                if (resolver.shouldForceBoolean(key)) {
                                    param.setResult(true);
                                }
                            } catch (Throwable t) {
                                CameraBoostLog.log("getSupportFunction callback failed "
                                        + className + ": " + t.getClass().getSimpleName());
                            }
                        }
                    });
                    installed++;
                } catch (Throwable t) {
                    CameraBoostLog.log("getSupportFunction hook skipped "
                            + identity + ": " + t.getClass().getSimpleName());
                }
            }
        }

        CameraBoostLog.log("safe getSupportFunction hooks installed=" + installed
                + " in " + (android.os.SystemClock.elapsedRealtime() - start) + " ms");
    }

    private static String firstString(Object[] args) {
        if (args == null) return "";
        for (Object arg : args) {
            if (arg instanceof String) return (String) arg;
        }
        return "";
    }
}
