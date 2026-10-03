package dev.cameraboost.oplus10bit;

import de.robv.android.xposed.callbacks.XC_LoadPackage;

final class ColorOS17CompatHook {
    private static volatile RuntimeCapabilityResolver resolver;

    private ColorOS17CompatHook() {}

    static void install(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!BuildConfig.ENABLE_COLOROS17_COMPAT) {
            return;
        }
        RuntimeCapabilityResolver local = new RuntimeCapabilityResolver(
                lpparam.classLoader, lpparam.appInfo.sourceDir);
        resolver = local;

        CameraBoostLog.log("ColorOS17 compatibility layer active; source="
                + lpparam.appInfo.sourceDir);
        CameraBoostLog.log("capabilities: " + local.summary());

        OplusUniversalGateHook.install(lpparam.classLoader, local);
        FilterCompatHook.install(lpparam.classLoader, local);

        if (local.supportsModernAiComposition()) {
            CameraBoostLog.log("AI route=modern OplusAIComposition; legacy Morpho gate ignored");
        } else if (local.supportsLegacyAiCaptureGuide()) {
            CameraBoostLog.log("AI route=legacy Morpho Capture Guide");
        } else {
            CameraBoostLog.log("AI route=unresolved");
        }

        if (local.supportsLivePhoto()) {
            CameraBoostLog.log("Live Photo modern implementation detected");
        }
    }

    static ColorOS17ConfigPatcher.Result patchConfig(String input) {
        RuntimeCapabilityResolver local = resolver;
        if (!BuildConfig.ENABLE_COLOROS17_COMPAT || local == null) {
            return new ColorOS17ConfigPatcher.Result(
                    false, false, 0, 0, input, "compat resolver unavailable");
        }
        return ColorOS17ConfigPatcher.patch(input, local);
    }
}
