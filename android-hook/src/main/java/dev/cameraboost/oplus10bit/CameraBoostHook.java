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
    private static final String GALLERY_PACKAGE = "com.coloros.gallery3d";
    private static final String SCANNER_PACKAGE = "com.coloros.ocrscanner";
    private static final String UPDATE_HELPER =
            "com.oplus.ocs.camera.consumer.apsAdapter.update.UpdateHelper";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (GALLERY_PACKAGE.equals(lpparam.packageName)) {
            GalleryCompatHook.install(lpparam.classLoader);
            return;
        }

        if (SCANNER_PACKAGE.equals(lpparam.packageName)) {
            ScannerCompatHook.install(lpparam.classLoader);
            return;
        }

        if (!TARGET_PACKAGE.equals(lpparam.packageName)) {
            return;
        }

        log("loaded " + TARGET_PACKAGE + "; " + FeaturePolicy.deviceIdentity());
        log("variant: 10bitHEIC=" + BuildConfig.ENABLE_10BIT_HEIC
                + ", 10bitLivePhoto=" + BuildConfig.ENABLE_10BIT_LIVE_PHOTO
                + ", colorOS17Compat=" + BuildConfig.ENABLE_COLOROS17_COMPAT
                + ", experimentalAll=" + BuildConfig.ENABLE_EXPERIMENTAL_ALL);

        if (!FeaturePolicy.isTargetDevice()) {
            log("device guard rejected this device; hook will stay observation-only");
        }

        if (BuildConfig.ENABLE_COLOROS17_COMPAT) {
            CameraBoostLog.log("stage=CAMERA_LOAD_ENTER");

            // hotfix5 keeps the hotfix4 safety baseline, but reintroduces one
            // isolated adapter for the exact HighPixel/TurboRAW keys that were
            // verified on Camera 7.013.30. No global gate/config mutation returns.
            CameraBoostLog.log("stage=CAMERA_SAFE_BASELINE");
            HighPixelCompatHook.install(lpparam.classLoader);
            CameraBoostLog.log("camera mutations disabled except exact HighPixel adapter: "
                    + "config patcher, CameraUnit, universal gates, getSupportFunction, "
                    + "Master/AI q0, OSEE bridge remain disabled");
            CameraBoostLog.log("stage=CAMERA_LOAD_EXIT");
            return;
        }

        // Legacy/standalone 10-bit variants keep the original narrow hook path.
        VendorTagGateHook.install(lpparam.classLoader);
        installLegacyConfigDocumentHook(lpparam.classLoader);
    }

    private static void installLegacyConfigDocumentHook(ClassLoader classLoader) {
        try {
            Class<?> helper = XposedHelpers.findClass(UPDATE_HELPER, classLoader);
            XposedBridge.hookAllMethods(helper, "getValidConfigData", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    handleLegacyConfigResult(param);
                }
            });
            log("hooked " + UPDATE_HELPER + "#getValidConfigData");
        } catch (Throwable t) {
            log("config document path unavailable: " + t.getClass().getSimpleName());
        }
    }

    private static void handleLegacyConfigResult(XC_MethodHook.MethodHookParam param) {
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

        OplusConfigPatcher.PatchResult patched =
                OplusConfigPatcher.inspectAndPatch(original, enable10Bit, enableLive);

        if (!patched.parsed) {
            log("config parse failed for " + configName + ": " + patched.error);
            return;
        }

        log("config=" + configName
                + " before=" + patched.before
                + " after=" + patched.after
                + " changed=" + patched.changed);

        if (patched.changed && (enable10Bit || enableLive)) {
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
        XposedBridge.log("CameraBoost: " + message);
    }
}
