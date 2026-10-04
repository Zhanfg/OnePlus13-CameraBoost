package dev.cameraboost.oplus10bit;

import android.util.Size;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * ColorOS 17 direct-25MP compatibility for Camera 7.013.30.
 *
 * Scope is intentionally narrow: only CommonCapMode (gm.q2) is modified.
 * It does not expose the standalone HighPixelMode, alter ModeSwitcher, or
 * globally force CameraConfig feature gates.
 */
final class Direct25MpCompatHook {
    private static final String COMMON_CAP_MODE = "gm.q2";
    private static final String CAMERA_PARAMETER =
            "com.oplus.ocs.camera.CameraParameter";

    private static final Set<String> SUPPORT_KEYS = new HashSet<>(Arrays.asList(
            "key_high_picture_size",
            "func_switch_capture_resolution_support",
            "com.oplus.turboraw.re.support",
            "com.oplus.feature.highpixel.merge.support",
            "pref_ultra_wide_high_picture_size_key",
            "pref_tele_high_picture_size_key",
            "pref_more_tele_high_picture_size_key",
            "pref_ultra_tele_high_picture_size_key",
            "pref_dual_high_picture_size_key"
    ));

    private static final Set<String> LOGGED = ConcurrentHashMap.newKeySet();

    private static final Size SIZE_4_3 = new Size(5888, 4416);
    private static final Size SIZE_16_9 = new Size(5888, 3312);
    private static final Size SIZE_WIDE = new Size(5888, 2704);
    private static final Size SIZE_ULTRAWIDE = new Size(5888, 2174);
    private static final Size SIZE_1_1 = new Size(4416, 4416);

    private Direct25MpCompatHook() {}

    static void install(ClassLoader classLoader) {
        if (!FeaturePolicy.isTargetDevice()) {
            CameraBoostLog.log("Direct25MP skipped: target-device guard");
            return;
        }

        Class<?> common = XposedHelpers.findClassIfExists(COMMON_CAP_MODE, classLoader);
        Class<?> cameraParameter =
                XposedHelpers.findClassIfExists(CAMERA_PARAMETER, classLoader);
        if (common == null || cameraParameter == null) {
            CameraBoostLog.log("Direct25MP unavailable: common="
                    + (common != null) + ", CameraParameter=" + (cameraParameter != null));
            return;
        }

        hookCurrentHighPictureState(common);
        hookSupportFunction(common);
        hookConfigureParameters(common, cameraParameter);
        hookQbcOutputSize(common);

        CameraBoostLog.log("Direct25MP installed for CommonCapMode only");
    }

    /**
     * CommonCapMode#f2 is Camera 7.013.30's current high-picture-size state.
     * HighPixelMode overrides this to true; direct-25MP needs the same capture
     * state while staying inside the normal Photo mode.
     */
    private static void hookCurrentHighPictureState(Class<?> common) {
        try {
            Method method = common.getDeclaredMethod("f2");
            method.setAccessible(true);
            XposedBridge.hookMethod(method, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (!isExactCommonMode(param.thisObject)) return;
                    param.setResult(Boolean.TRUE);
                    logOnce("f2", "Direct25MP: CommonCapMode#f2 -> true");
                }
            });
        } catch (Throwable t) {
            CameraBoostLog.error("Direct25MP f2 hook", t);
        }
    }

    private static void hookSupportFunction(Class<?> common) {
        try {
            Method method = common.getDeclaredMethod("getSupportFunction", String.class);
            method.setAccessible(true);
            XposedBridge.hookMethod(method, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (!isExactCommonMode(param.thisObject)) return;
                    String key = param.args != null && param.args.length > 0
                            && param.args[0] instanceof String
                            ? (String) param.args[0] : null;
                    if (key == null || !SUPPORT_KEYS.contains(key)) return;

                    param.setResult(Boolean.TRUE);
                    logOnce("support:" + key,
                            "Direct25MP support: " + key + " -> true");
                }
            });
        } catch (Throwable t) {
            CameraBoostLog.error("Direct25MP getSupportFunction hook", t);
        }
    }

    /**
     * Patch only the configure-parameter object passed to CommonCapMode#Z7.
     * No global CameraConfig document is modified.
     */
    private static void hookConfigureParameters(Class<?> common, Class<?> cameraParameter) {
        Method target = null;
        for (Method method : common.getDeclaredMethods()) {
            if (!"Z7".equals(method.getName()) || method.getParameterTypes().length != 1) {
                continue;
            }
            target = method;
            break;
        }
        if (target == null) {
            CameraBoostLog.log("Direct25MP: CommonCapMode#Z7 not found");
            return;
        }

        final Object highPictureSizeEnable =
                XposedHelpers.getStaticObjectField(cameraParameter, "HIGH_PICTURE_SIZE_ENABLE");
        final Object qbcSessionEnable =
                XposedHelpers.getStaticObjectField(
                        cameraParameter, "CONFIGURE_KEY_FULL_BINING_QBC_SESSION_ENABLE");
        final Object qbcEnable =
                XposedHelpers.getStaticObjectField(
                        cameraParameter, "CONFIGURE_KEY_FULL_BINING_QBC_ENABLE");
        final Object captureResolutionFormat =
                XposedHelpers.getStaticObjectField(
                        cameraParameter, "KEY_CAPTURE_RESOLUTION_FORMAT");

        target.setAccessible(true);
        XposedBridge.hookMethod(target, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                if (!isExactCommonMode(param.thisObject)
                        || param.args == null || param.args.length == 0
                        || param.args[0] == null) {
                    return;
                }

                Object configure = param.args[0];
                trySet(configure, highPictureSizeEnable, Boolean.TRUE,
                        "HIGH_PICTURE_SIZE_ENABLE");
                trySet(configure, qbcSessionEnable, Boolean.TRUE,
                        "QBC_SESSION_ENABLE");
                trySet(configure, qbcEnable, Boolean.TRUE,
                        "QBC_ENABLE");
                trySet(configure, captureResolutionFormat, "high",
                        "CAPTURE_RESOLUTION_FORMAT=high");
            }
        });

        CameraBoostLog.log("Direct25MP: hooked CommonCapMode#Z7");
    }

    /**
     * CommonCapMode#r9(int) chooses the QBC output Size. Preserve its aspect
     * ratio decision but raise 12MP-class outputs to the known OnePlus 13
     * 25MP QBC sizes from OPCameraPro's original direct-25MP configuration.
     */
    private static void hookQbcOutputSize(Class<?> common) {
        try {
            Method method = common.getDeclaredMethod("r9", int.class);
            method.setAccessible(true);
            XposedBridge.hookMethod(method, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (!isExactCommonMode(param.thisObject)) return;
                    if (isFrontCamera(param.thisObject)) return;

                    Object result = param.getResult();
                    if (!(result instanceof Size)) return;

                    Size original = (Size) result;
                    long area = (long) original.getWidth() * original.getHeight();
                    // Do not downscale an already high-resolution result.
                    if (area >= 20_000_000L) {
                        logOnce("r9-high",
                                "Direct25MP r9 already high: " + original);
                        return;
                    }

                    Size target = selectByAspect(original);
                    param.setResult(target);
                    logOnce("r9:" + original,
                            "Direct25MP r9: " + original + " -> " + target);
                }
            });
        } catch (Throwable t) {
            CameraBoostLog.error("Direct25MP r9 hook", t);
        }
    }

    private static Size selectByAspect(Size original) {
        if (original.getWidth() <= 0 || original.getHeight() <= 0) return SIZE_4_3;
        double ratio = Math.max(original.getWidth(), original.getHeight())
                / (double) Math.min(original.getWidth(), original.getHeight());

        Size[] choices = {
                SIZE_1_1, SIZE_4_3, SIZE_16_9, SIZE_WIDE, SIZE_ULTRAWIDE
        };
        Size best = SIZE_4_3;
        double bestError = Double.MAX_VALUE;
        for (Size candidate : choices) {
            double candidateRatio = Math.max(candidate.getWidth(), candidate.getHeight())
                    / (double) Math.min(candidate.getWidth(), candidate.getHeight());
            double error = Math.abs(candidateRatio - ratio);
            if (error < bestError) {
                bestError = error;
                best = candidate;
            }
        }
        return best;
    }

    private static void trySet(Object configure, Object key, Object value, String label) {
        if (key == null) return;
        try {
            XposedHelpers.callMethod(configure, "a", key, value);
            logOnce("cfg:" + label, "Direct25MP configure: " + label);
        } catch (Throwable t) {
            CameraBoostLog.log("Direct25MP configure failed: " + label
                    + " / " + t.getClass().getSimpleName());
        }
    }

    private static boolean isFrontCamera(Object mode) {
        try {
            Object result = XposedHelpers.callMethod(mode, "s");
            return result instanceof Boolean && (Boolean) result;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean isExactCommonMode(Object obj) {
        return obj != null && COMMON_CAP_MODE.equals(obj.getClass().getName());
    }

    private static void logOnce(String key, String message) {
        if (LOGGED.add(key)) {
            CameraBoostLog.log(message);
        }
    }
}
