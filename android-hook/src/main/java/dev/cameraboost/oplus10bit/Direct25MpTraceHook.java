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
 * Read-only Camera 7.013.30 direct-25MP tracer.
 *
 * No result, argument, CameraConfig, CameraParameter, zoom state, mode state,
 * or output Size is modified here.
 */
final class Direct25MpTraceHook {
    private static final String COMMON_BASE = "gm.q2";
    private static final String COMMON_RUNTIME = "im.j1";

    private static final Set<String> WATCH_KEYS = new HashSet<>(Arrays.asList(
            "key_high_picture_size",
            "func_switch_capture_resolution_support",
            "pref_camera_high_resolution_key",
            "pref_ultra_wide_high_picture_size_key",
            "pref_tele_high_picture_size_key",
            "pref_more_tele_high_picture_size_key",
            "pref_ultra_tele_high_picture_size_key",
            "pref_dual_high_picture_size_key",
            "com.oplus.turboraw.re.support",
            "com.oplus.feature.highpixel.merge.support"
    ));

    private static final Set<String> ONCE = ConcurrentHashMap.newKeySet();
    private static final AtomicInteger R9_COUNT = new AtomicInteger();
    private static final AtomicInteger Z7_COUNT = new AtomicInteger();

    private Direct25MpTraceHook() {}

    static void install(ClassLoader classLoader) {
        if (!FeaturePolicy.isTargetDevice()) {
            CameraBoostLog.log("Direct25MP trace skipped: target-device guard");
            return;
        }

        Class<?> base = XposedHelpers.findClassIfExists(COMMON_BASE, classLoader);
        Class<?> runtime = XposedHelpers.findClassIfExists(COMMON_RUNTIME, classLoader);
        if (base == null || runtime == null) {
            CameraBoostLog.log("Direct25MP trace unavailable: base="
                    + (base != null) + ", runtime=" + (runtime != null));
            return;
        }

        hookF2(base);
        hookSupport(base);
        hookZ7(base);
        hookR9(base);

        CameraBoostLog.log("Direct25MP trace-only installed: runtime "
                + COMMON_RUNTIME + " -> base " + COMMON_BASE);
    }

    private static boolean isCommon(Object obj) {
        return obj != null && COMMON_RUNTIME.equals(obj.getClass().getName());
    }

    private static void hookF2(Class<?> base) {
        try {
            Method m = base.getDeclaredMethod("f2");
            m.setAccessible(true);
            XposedBridge.hookMethod(m, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (!isCommon(param.thisObject)) return;
                    logOnce("f2:" + String.valueOf(param.getResult()),
                            "Direct25MP TRACE f2 original=" + param.getResult());
                }
            });
        } catch (Throwable t) {
            CameraBoostLog.error("Direct25MP TRACE f2", t);
        }
    }

    private static void hookSupport(Class<?> base) {
        try {
            Method m = base.getDeclaredMethod("getSupportFunction", String.class);
            m.setAccessible(true);
            XposedBridge.hookMethod(m, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (!isCommon(param.thisObject)
                            || param.args == null || param.args.length == 0
                            || !(param.args[0] instanceof String)) return;

                    String key = (String) param.args[0];
                    if (!WATCH_KEYS.contains(key)) return;

                    logOnce("support:" + key + ":" + String.valueOf(param.getResult()),
                            "Direct25MP TRACE support " + key
                                    + " original=" + param.getResult());
                }
            });
        } catch (Throwable t) {
            CameraBoostLog.error("Direct25MP TRACE getSupportFunction", t);
        }
    }

    private static void hookZ7(Class<?> base) {
        try {
            for (Method m : base.getDeclaredMethods()) {
                if (!"Z7".equals(m.getName()) || m.getParameterTypes().length != 1) {
                    continue;
                }
                m.setAccessible(true);
                XposedBridge.hookMethod(m, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        if (!isCommon(param.thisObject)) return;
                        int n = Z7_COUNT.incrementAndGet();
                        if (n <= 8) {
                            Object arg = param.args != null && param.args.length > 0
                                    ? param.args[0] : null;
                            CameraBoostLog.log("Direct25MP TRACE Z7 original completed"
                                    + ", call=" + n
                                    + ", argClass=" + (arg == null ? "null"
                                    : arg.getClass().getName()));
                        }
                    }
                });
                return;
            }
            CameraBoostLog.log("Direct25MP TRACE Z7 not found");
        } catch (Throwable t) {
            CameraBoostLog.error("Direct25MP TRACE Z7", t);
        }
    }

    private static void hookR9(Class<?> base) {
        try {
            Method m = base.getDeclaredMethod("r9", int.class);
            m.setAccessible(true);
            XposedBridge.hookMethod(m, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (!isCommon(param.thisObject)) return;
                    int n = R9_COUNT.incrementAndGet();
                    if (n <= 12) {
                        CameraBoostLog.log("Direct25MP TRACE r9 original"
                                + ", call=" + n
                                + ", arg=" + (param.args == null || param.args.length == 0
                                ? "<none>" : param.args[0])
                                + ", result=" + param.getResult());
                    }
                }
            });
        } catch (Throwable t) {
            CameraBoostLog.error("Direct25MP TRACE r9", t);
        }
    }

    private static void logOnce(String key, String message) {
        if (ONCE.add(key)) {
            CameraBoostLog.log(message);
        }
    }
}
