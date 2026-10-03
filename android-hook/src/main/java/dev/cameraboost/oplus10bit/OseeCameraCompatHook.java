package dev.cameraboost.oplus10bit;

import android.content.Context;
import android.content.pm.PackageManager;

import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * Camera -> AI 拍一拍 bridge.
 *
 * SupportStatus values recovered from Camera 7.013.30:
 * 1000 = unsupported
 * 1001 = installed below 17
 * 1002 = supported but incomplete
 * 1003 = supported and installed
 */
final class OseeCameraCompatHook {
    private static final int OSEE_SUPPORTED_AND_INSTALLED = 1003;
    private static final String SCANNER_PACKAGE = "com.coloros.ocrscanner";

    private static final String[] OSEE_CLASSES = {
            "com.oplus.base.ocrscanner.oseesdk.OseeSdk",
            "com.oplus.base.ocrscanner.oseesdk.OseeSdkHandler"
    };

    private OseeCameraCompatHook() {}

    static void install(ClassLoader classLoader) {
        if (!BuildConfig.ENABLE_COLOROS17_COMPAT || !FeaturePolicy.isTargetDevice()) {
            return;
        }

        for (String className : OSEE_CLASSES) {
            try {
                Class<?> cls = XposedHelpers.findClass(className, classLoader);
                XposedBridge.hookAllMethods(cls, "isScannerSupport", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        if (!(param.method instanceof Method)
                                || ((Method) param.method).getReturnType() != int.class) {
                            return;
                        }

                        Context context = firstContext(param.args);
                        if (context != null && isScannerInstalled(context)) {
                            param.setResult(OSEE_SUPPORTED_AND_INSTALLED);
                        }
                    }
                });
                CameraBoostLog.log("Camera OSEE support bridge installed on " + className);
            } catch (Throwable t) {
                CameraBoostLog.log("Camera OSEE bridge unavailable on " + className + ": "
                        + t.getClass().getSimpleName());
            }
        }
    }

    private static Context firstContext(Object[] args) {
        if (args == null) {
            return null;
        }
        for (Object arg : args) {
            if (arg instanceof Context) {
                return (Context) arg;
            }
        }
        return null;
    }

    private static boolean isScannerInstalled(Context context) {
        try {
            context.getPackageManager().getPackageInfo(
                    SCANNER_PACKAGE,
                    PackageManager.PackageInfoFlags.of(0)
            );
            return true;
        } catch (Throwable ignored) {
            try {
                // Fallback for pre-33 compatible package manager path.
                context.getPackageManager().getPackageInfo(SCANNER_PACKAGE, 0);
                return true;
            } catch (Throwable ignoredAgain) {
                return false;
            }
        }
    }
}
