package dev.cameraboost.oplus10bit;

import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * ColorOS 17 "AI 拍一拍" / OSEE scanner compatibility layer.
 *
 * Scanner 17.2.8 maps OBJECT_SCANNER to the "osee" entrance and gates the feature
 * through CameraActivity#w1(). We only relax that exact gate.
 */
final class ScannerCompatHook {
    private static final String CAMERA_ACTIVITY =
            "com.oplus.scanner.ui.main.CameraActivity";

    private ScannerCompatHook() {}

    static void install(ClassLoader classLoader) {
        if (!BuildConfig.ENABLE_COLOROS17_COMPAT || !FeaturePolicy.isTargetDevice()) {
            return;
        }

        try {
            Class<?> cls = XposedHelpers.findClass(CAMERA_ACTIVITY, classLoader);
            XposedBridge.hookAllMethods(cls, "w1", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (param.method instanceof Method) {
                        Class<?> ret = ((Method) param.method).getReturnType();
                        if (ret == boolean.class || ret == Boolean.class) {
                            param.setResult(true);
                        }
                    }
                }
            });
            CameraBoostLog.log("AI 拍一拍 feature_osee gate forced available");
        } catch (Throwable t) {
            CameraBoostLog.log("AI 拍一拍 CameraActivity#w1 unavailable: "
                    + t.getClass().getSimpleName());
        }
    }
}
