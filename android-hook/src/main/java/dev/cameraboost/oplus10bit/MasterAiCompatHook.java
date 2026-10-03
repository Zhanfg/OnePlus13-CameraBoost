package dev.cameraboost.oplus10bit;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * Camera 7.013.30 premium-feature compatibility bridge.
 *
 * These q0 methods are not arbitrary obfuscated names: each was verified against the
 * supplied APK by its stable string anchor. They complement (not replace) OPCameraPro's
 * own MAX/25MP/native hooks.
 */
final class MasterAiCompatHook {
    private static final String CONFIG_CLASS = "ka.q0";

    private MasterAiCompatHook() {}

    static void install(ClassLoader classLoader, OplusCapabilityResolver resolver) {
        if (!BuildConfig.ENABLE_COLOROS17_COMPAT || !FeaturePolicy.isTargetDevice()) {
            return;
        }

        try {
            Class<?> cls = XposedHelpers.findClass(CONFIG_CLASS, classLoader);

            hookBooleanNoArg(cls, "A",
                    resolver.hasAnchor("com.oplus.feature.master.jpg.max.support"),
                    "JPG MAX");

            boolean modernAi = resolver.hasAnyAiGuide();
            hookBooleanNoArg(cls, "I", modernAi, "AI Composition inspiration");
            hookBooleanNoArg(cls, "J", modernAi, "AI Composition real-scene");

            boolean masterV2 = resolver.hasAnchor("com.oplus.feature.master.mode.version");
            for (String method : Arrays.asList("Y", "Z", "a0", "b0", "c0")) {
                hookBooleanNoArg(cls, method, masterV2, "Master mode v2");
            }

            boolean hasselblad = resolver.hasAnchor(
                    "com.oplus.professional.use.hasselblad.style.support")
                    || resolver.hasAnchor("com.oplus.use.hasselblad.style.support");
            hookBooleanNoArg(cls, "z", hasselblad, "Hasselblad style");
        } catch (Throwable t) {
            CameraBoostLog.log("premium q0 bridge unavailable: "
                    + t.getClass().getSimpleName());
        }
    }

    private static void hookBooleanNoArg(
            Class<?> cls,
            String methodName,
            boolean enabled,
            String label
    ) {
        if (!enabled) return;
        try {
            for (Method method : cls.getDeclaredMethods()) {
                if (!methodName.equals(method.getName())
                        || method.getParameterCount() != 0
                        || (method.getReturnType() != boolean.class
                        && method.getReturnType() != Boolean.class)) {
                    continue;
                }
                method.setAccessible(true);
                XposedBridge.hookMethod(method, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        param.setResult(true);
                    }
                });
                CameraBoostLog.log("premium gate enabled: " + label
                        + " via " + CONFIG_CLASS + "#" + methodName);
            }
        } catch (Throwable t) {
            CameraBoostLog.log("premium gate skipped " + label + ": "
                    + t.getClass().getSimpleName());
        }
    }
}
