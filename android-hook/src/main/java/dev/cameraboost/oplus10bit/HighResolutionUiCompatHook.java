package dev.cameraboost.oplus10bit;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * Restore the native Camera 7.013.30 High Resolution UI entry only.
 *
 * Important:
 * - does NOT force high-picture state
 * - does NOT change QBC/session/output sizes
 * - does NOT touch zoom/SAT/cameraId
 * - does NOT expose standalone HighPixelMode
 *
 * Camera 7.013.30 already loads:
 *   com.oplus.camera.feature.high_resolution
 *   presenter rg.g
 * into runtime common mode im.j1.
 *
 * The missing UI entry is controlled separately by
 * gm.q2#getSupportFunction("pref_camera_high_resolution_key").
 */
final class HighResolutionUiCompatHook {
    private static final String COMMON_BASE = "gm.q2";
    private static final String COMMON_RUNTIME = "im.j1";
    private static final String PREF_HIGH_RESOLUTION =
            "pref_camera_high_resolution_key";
    private static final String PRESENTER = "rg.g";

    private static final Set<String> LOGGED = ConcurrentHashMap.newKeySet();

    private HighResolutionUiCompatHook() {}

    static void install(ClassLoader classLoader) {
        if (!FeaturePolicy.isTargetDevice()) {
            CameraBoostLog.log("HighResolutionUI skipped: target-device guard");
            return;
        }

        Class<?> base = XposedHelpers.findClassIfExists(COMMON_BASE, classLoader);
        Class<?> runtime = XposedHelpers.findClassIfExists(COMMON_RUNTIME, classLoader);
        Class<?> presenter = XposedHelpers.findClassIfExists(PRESENTER, classLoader);

        if (base == null || runtime == null || presenter == null) {
            CameraBoostLog.log("HighResolutionUI unavailable: base="
                    + (base != null) + ", runtime=" + (runtime != null)
                    + ", presenter=" + (presenter != null));
            return;
        }

        hookPreferenceSupport(base);
        tracePresenter(presenter);

        CameraBoostLog.log("HighResolutionUI installed: exact preference only; "
                + "feature=" + PRESENTER + ", runtime=" + COMMON_RUNTIME);
    }

    private static void hookPreferenceSupport(Class<?> base) {
        try {
            Method method = base.getDeclaredMethod("getSupportFunction", String.class);
            method.setAccessible(true);

            XposedBridge.hookMethod(method, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (!isRuntimeCommon(param.thisObject)
                            || param.args == null
                            || param.args.length == 0
                            || !(param.args[0] instanceof String)) {
                        return;
                    }

                    String key = (String) param.args[0];
                    if (!PREF_HIGH_RESOLUTION.equals(key)) {
                        return;
                    }

                    Object original = param.getResult();
                    param.setResult(Boolean.TRUE);

                    logOnce("pref-support",
                            "HighResolutionUI: "
                                    + PREF_HIGH_RESOLUTION
                                    + " original=" + original
                                    + " -> true");
                }
            });
        } catch (Throwable t) {
            CameraBoostLog.error("HighResolutionUI preference hook", t);
        }
    }

    /**
     * Read-only tracing of the native high-resolution presenter.
     * Obfuscated-name mapping was verified against Camera 6.070.228:
     *
     * rg.g#H2(String,String) = preference/state change handler
     * rg.g#M2()              = internal availability predicate
     * rg.g#N2(String)        = native high-resolution UI update
     */
    private static void tracePresenter(Class<?> presenter) {
        try {
            Method availability = presenter.getDeclaredMethod("M2");
            availability.setAccessible(true);
            XposedBridge.hookMethod(availability, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    logOnce("presenter-M2:" + String.valueOf(param.getResult()),
                            "HighResolutionUI TRACE presenter availability="
                                    + param.getResult());
                }
            });
        } catch (Throwable t) {
            CameraBoostLog.error("HighResolutionUI presenter M2 trace", t);
        }

        try {
            Method change = presenter.getDeclaredMethod(
                    "H2", String.class, String.class);
            change.setAccessible(true);
            XposedBridge.hookMethod(change, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    CameraBoostLog.log("HighResolutionUI TRACE state change: "
                            + String.valueOf(param.args[0])
                            + " -> "
                            + String.valueOf(param.args[1]));
                }
            });
        } catch (Throwable t) {
            CameraBoostLog.error("HighResolutionUI presenter H2 trace", t);
        }

        try {
            Method updateUi = presenter.getDeclaredMethod("N2", String.class);
            updateUi.setAccessible(true);
            XposedBridge.hookMethod(updateUi, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    CameraBoostLog.log("HighResolutionUI TRACE native UI update: "
                            + String.valueOf(param.args[0]));
                }
            });
        } catch (Throwable t) {
            CameraBoostLog.error("HighResolutionUI presenter N2 trace", t);
        }
    }

    private static boolean isRuntimeCommon(Object obj) {
        return obj != null && COMMON_RUNTIME.equals(obj.getClass().getName());
    }

    private static void logOnce(String key, String message) {
        if (LOGGED.add(key)) {
            CameraBoostLog.log(message);
        }
    }
}
