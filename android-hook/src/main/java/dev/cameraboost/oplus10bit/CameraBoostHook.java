package dev.cameraboost.oplus10bit;

import java.util.Arrays;
import java.util.Locale;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public final class CameraBoostHook implements IXposedHookLoadPackage {
    private static final String TARGET_PACKAGE = "com.oplus.camera";
    private static final String UPDATE_HELPER =
            "com.oplus.ocs.camera.consumer.apsAdapter.update.UpdateHelper";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!TARGET_PACKAGE.equals(lpparam.packageName)) {
            return;
        }

        log("loaded " + TARGET_PACKAGE + "; " + FeaturePolicy.deviceIdentity());
        log("variant: 10bitHEIC=" + BuildConfig.ENABLE_10BIT_HEIC
                + ", 10bitLivePhoto=" + BuildConfig.ENABLE_10BIT_LIVE_PHOTO
                + ", fullUnlock=" + BuildConfig.ENABLE_FULL_UNLOCK);

        if (!FeaturePolicy.isTargetDevice()) {
            log("device guard rejected this device; hook will stay observation-only");
        }

        RuntimeArchitecture runtime = RuntimeArchitecture.detect(lpparam.classLoader);
        log(runtime.summary());

        // Observe/override all known OPlus feature-gate paths.
        VendorTagGateHook.install(lpparam.classLoader, runtime);
        CameraConfigCompatHook.install(lpparam.classLoader, runtime);
        CameraUnitCompatHook.install(lpparam.classLoader, runtime);
        installConfigDocumentHook(lpparam.classLoader, runtime);

        if (BuildConfig.ENABLE_FULL_UNLOCK && FeaturePolicy.isTargetDevice()) {
            // Older camera builds still expose ConfigFeatureImpl; ColorOS 17 does not.
            FeatureValueLegalHook.install(lpparam.classLoader, runtime);

            // Resolve both app-level and OCS/IPU filter managers.
            FilterGroupCompatHook.install(lpparam.classLoader, runtime);
        }
    }

    private static void installConfigDocumentHook(
            ClassLoader classLoader,
            RuntimeArchitecture runtime
    ) {
        try {
            Class<?> helper = XposedHelpers.findClass(UPDATE_HELPER, classLoader);
            XposedBridge.hookAllMethods(helper, "getValidConfigData", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    handleConfigResult(param, runtime);
                }
            });
            log("hooked " + UPDATE_HELPER + "#getValidConfigData");
        } catch (Throwable t) {
            log("config document path unavailable: " + t.getClass().getSimpleName());
        }
    }

    private static void handleConfigResult(
            XC_MethodHook.MethodHookParam param,
            RuntimeArchitecture runtime
    ) {
        Object result = param.getResult();
        if (!(result instanceof String)) {
            return;
        }

        String original = (String) result;
        String configName = findConfigName(param.args);

        boolean looksLikeCameraConfig =
                configName.toLowerCase(Locale.ROOT).contains("oplus_camera_config")
                        || original.contains("\"VendorTag\"");

        if (!looksLikeCameraConfig) {
            return;
        }

        boolean canMutate = FeaturePolicy.isTargetDevice();
        boolean enable10Bit = canMutate && BuildConfig.ENABLE_10BIT_HEIC;
        boolean enableLive = canMutate && BuildConfig.ENABLE_10BIT_LIVE_PHOTO;
        boolean fullUnlock = canMutate && BuildConfig.ENABLE_FULL_UNLOCK;

        OplusConfigPatcher.PatchResult patched =
                OplusConfigPatcher.inspectAndPatch(
                        original,
                        enable10Bit,
                        enableLive,
                        fullUnlock,
                        runtime
                );

        if (!patched.parsed) {
            log("config parse failed for " + configName + ": " + patched.error);
            return;
        }

        log("config=" + configName
                + " before=" + patched.before
                + " after=" + patched.after
                + " changedKeys=" + patched.changedKeys);

        if (patched.changed && (enable10Bit || enableLive || fullUnlock)) {
            param.setResult(patched.output);
            log("applied guarded OPlus camera feature-gate patch");
        }
    }

    private static String findConfigName(Object[] args) {
        if (args == null) {
            return "<unknown>";
        }

        return Arrays.stream(args)
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .filter(value -> value.toLowerCase(Locale.ROOT).contains("camera"))
                .findFirst()
                .orElse("<unknown>");
    }

    private static void log(String message) {
        XposedBridge.log("CameraBoostFull: " + message);
    }
}
