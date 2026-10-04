package dev.cameraboost.oplus10bit;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * Read-only tracer for Camera 7.013.30's native HighResolution feature.
 *
 * Native semantic mapping recovered from the supplied APK:
 *   rg.a = HighResolutionKeys
 *   rg.b = HighResolutionModel
 *   rg.g = HighResolutionPresenter
 *
 * This hook never changes a result/argument/DataManager value.
 */
final class NativeHighResolutionTraceHook {
    private static final String CAMERA_CONFIG =
            "com.oplus.camera.configure.CameraConfig";
    private static final String PRESENTER = "rg.g";

    private static final Set<String> KEYS = new HashSet<>(Arrays.asList(
            "com.oplus.super.resolution.picturesize",
            "com.oplus.feature.high.definition.support",
            "com.oplus.pre.high.resolution.support",
            "com.oplus.turboraw.re.support",
            "com.oplus.high.picturesize.name",
            "com.oplus.high.picturesize",
            "com.oplus.ultra.wide.high.supported.picturesize",
            "com.oplus.tele.high.supported.picturesize",
            "com.oplus.camera.hasselblad.super.definition.support"
    ));

    private static final Set<String> ONCE = ConcurrentHashMap.newKeySet();
    private static final AtomicInteger PRESENTER_EVENTS = new AtomicInteger();

    private NativeHighResolutionTraceHook() {}

    static void install(ClassLoader cl) {
        if (!FeaturePolicy.isTargetDevice()) return;
        hookCameraConfig(cl);
        hookPresenter(cl);
        CameraBoostLog.log("NativeHighRes TRACE installed; mutation=false");
    }

    private static void hookCameraConfig(ClassLoader cl) {
        Class<?> cls = XposedHelpers.findClassIfExists(CAMERA_CONFIG, cl);
        if (cls == null) {
            CameraBoostLog.log("NativeHighRes TRACE CameraConfig missing");
            return;
        }

        for (Method m : cls.getDeclaredMethods()) {
            String name = m.getName();
            if (!("w".equals(name) || "d".equals(name)
                    || "o".equals(name) || "x".equals(name)
                    || "getConfigBooleanValue".equals(name))) {
                continue;
            }
            Class<?>[] ps = m.getParameterTypes();
            if (ps.length == 0 || ps[0] != String.class) continue;

            try {
                m.setAccessible(true);
                XposedBridge.hookMethod(m, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        if (param.args == null || param.args.length == 0
                                || !(param.args[0] instanceof String)) return;
                        String key = (String) param.args[0];
                        if (!KEYS.contains(key)) return;

                        String token = name + ":" + key + ":" + String.valueOf(param.getResult());
                        if (ONCE.add(token)) {
                            CameraBoostLog.log("NativeHighRes TRACE CameraConfig."
                                    + name + " " + key + " -> " + param.getResult());
                        }
                    }
                });
            } catch (Throwable t) {
                CameraBoostLog.error("NativeHighRes TRACE CameraConfig#" + name, t);
            }
        }
    }

    private static void hookPresenter(ClassLoader cl) {
        Class<?> cls = XposedHelpers.findClassIfExists(PRESENTER, cl);
        if (cls == null) {
            CameraBoostLog.log("NativeHighRes TRACE presenter rg.g missing");
            return;
        }

        hookNamed(cls, "N2", "menuEvent");
        hookNamed(cls, "G2", "stateChange");
        hookNamed(cls, "H2", "stateWrite");
        hookNamed(cls, "J2", "pictureSize");
        hookNamed(cls, "I2", "initState");
    }

    private static void hookNamed(Class<?> cls, String methodName, String semantic) {
        for (Method m : cls.getDeclaredMethods()) {
            if (!methodName.equals(m.getName())) continue;
            try {
                m.setAccessible(true);
                XposedBridge.hookMethod(m, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        int n = PRESENTER_EVENTS.incrementAndGet();
                        if (n <= 80) {
                            CameraBoostLog.log("NativeHighRes TRACE presenter."
                                    + methodName + "(" + semantic + ") before args="
                                    + Arrays.toString(param.args));
                        }
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        int n = PRESENTER_EVENTS.get();
                        if (n <= 80) {
                            CameraBoostLog.log("NativeHighRes TRACE presenter."
                                    + methodName + "(" + semantic + ") result="
                                    + param.getResult());
                        }
                    }
                });
            } catch (Throwable t) {
                CameraBoostLog.error("NativeHighRes TRACE presenter#" + methodName, t);
            }
        }
    }
}
