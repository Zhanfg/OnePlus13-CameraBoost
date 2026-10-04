package dev.cameraboost.oplus10bit;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * Restores Camera 7.013.30's native High Resolution UI entry only.
 *
 * Evidence from the supplied APK:
 * - Feature: com.oplus.camera.feature.high_resolution
 * - Presenter: rg.g (HighResolutionPresenter.java)
 * - Model: rg.b (HighResolutionModel.java)
 * - Runtime normal-photo mode: im.j1
 * - Visibility/support key: pref_camera_high_resolution_key
 *
 * This does NOT force the high-resolution state itself and does NOT touch QBC,
 * capture resolution, zoom, SAT, f2(), r9(), or configure parameters.
 */
final class NativeHighResolutionUiCompatHook {
    private static final String COMMON_BASE = "gm.q2";
    private static final String COMMON_RUNTIME = "im.j1";
    private static final String KEY = "pref_camera_high_resolution_key";
    private static final AtomicBoolean LOGGED = new AtomicBoolean(false);

    private NativeHighResolutionUiCompatHook() {}

    static void install(ClassLoader classLoader) {
        if (!FeaturePolicy.isTargetDevice()) {
            CameraBoostLog.log("NativeHighResolutionUI skipped: target-device guard");
            return;
        }

        Class<?> base = XposedHelpers.findClassIfExists(COMMON_BASE, classLoader);
        Class<?> runtime = XposedHelpers.findClassIfExists(COMMON_RUNTIME, classLoader);
        if (base == null || runtime == null) {
            CameraBoostLog.log("NativeHighResolutionUI unavailable: base="
                    + (base != null) + ", runtime=" + (runtime != null));
            return;
        }

        try {
            Method method = base.getDeclaredMethod("getSupportFunction", String.class);
            method.setAccessible(true);
            XposedBridge.hookMethod(method, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (param.thisObject == null
                            || !COMMON_RUNTIME.equals(param.thisObject.getClass().getName())
                            || param.args == null
                            || param.args.length == 0
                            || !(param.args[0] instanceof String)
                            || !KEY.equals(param.args[0])) {
                        return;
                    }

                    Object original = param.getResult();
                    param.setResult(Boolean.TRUE);

                    if (LOGGED.compareAndSet(false, true)) {
                        CameraBoostLog.log("NativeHighResolutionUI: " + KEY
                                + " original=" + original + " -> true");
                    }
                }
            });

            CameraBoostLog.log("NativeHighResolutionUI installed: "
                    + COMMON_RUNTIME + " / " + KEY);
        } catch (Throwable t) {
            CameraBoostLog.error("NativeHighResolutionUI install", t);
        }
    }
}
