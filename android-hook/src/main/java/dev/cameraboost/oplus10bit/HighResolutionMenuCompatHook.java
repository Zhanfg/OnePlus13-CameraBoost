package dev.cameraboost.oplus10bit;

import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * Restores Camera 7.013.30's native High Resolution menu entry without forcing
 * the capture state itself.
 *
 * CameraSettingsConfig.parseMenuPanel() only adds
 * pref_camera_high_resolution_key when:
 *   !com.oplus.feature.high.definition.support
 *   && com.oplus.pre.high.resolution.support
 *
 * On PJZ110 the HighResolutionPresenter/feature is still present and registered
 * in common mode, so the safe compatibility action is to restore only the menu
 * entry and let the native presenter own standard <-> standard_high.
 */
final class HighResolutionMenuCompatHook {
    private static final String SETTINGS_CONFIG =
            "com.oplus.camera.common.config.CameraSettingsConfig";
    private static final String CAMERA_CONFIG =
            "com.oplus.camera.configure.CameraConfig";
    private static final String HIGH_RES_PRESENTER = "rg.g";

    private static final String MENU_KEY = "pref_camera_high_resolution_key";
    private static final String INSERT_BEFORE = "pref_macro_switch";

    private static final String KEY_HIGH_DEFINITION =
            "com.oplus.feature.high.definition.support";
    private static final String KEY_PRE_HIGH_RESOLUTION =
            "com.oplus.pre.high.resolution.support";

    private HighResolutionMenuCompatHook() {}

    static void install(ClassLoader classLoader) {
        if (!FeaturePolicy.isTargetDevice()) {
            CameraBoostLog.log("HighResolution menu compat skipped: target-device guard");
            return;
        }

        Class<?> settings = XposedHelpers.findClassIfExists(SETTINGS_CONFIG, classLoader);
        Class<?> presenter = XposedHelpers.findClassIfExists(HIGH_RES_PRESENTER, classLoader);
        Class<?> cameraConfig = XposedHelpers.findClassIfExists(CAMERA_CONFIG, classLoader);

        if (settings == null || presenter == null) {
            CameraBoostLog.log("HighResolution menu compat unavailable: settings="
                    + (settings != null) + ", presenter=" + (presenter != null));
            return;
        }

        try {
            final Class<?> configClass = cameraConfig;
            XposedBridge.hookAllMethods(settings, "parseMenuPanel", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    logNativeGates(configClass);
                    restoreMenuEntry(param.thisObject);
                }
            });
            CameraBoostLog.log("HighResolution menu compat installed: "
                    + SETTINGS_CONFIG + "#parseMenuPanel");
        } catch (Throwable t) {
            CameraBoostLog.error("HighResolution menu compat install", t);
        }
    }

    private static void logNativeGates(Class<?> cameraConfig) {
        if (cameraConfig == null) return;

        try {
            Object hd = XposedHelpers.callStaticMethod(
                    cameraConfig, "getConfigBooleanValue", KEY_HIGH_DEFINITION);
            Object pre = XposedHelpers.callStaticMethod(
                    cameraConfig, "getConfigBooleanValue", KEY_PRE_HIGH_RESOLUTION);
            CameraBoostLog.log("HighResolution native gates at parseMenuPanel: "
                    + KEY_HIGH_DEFINITION + "=" + hd + ", "
                    + KEY_PRE_HIGH_RESOLUTION + "=" + pre);
        } catch (Throwable t) {
            CameraBoostLog.log("HighResolution native-gate probe failed: "
                    + t.getClass().getSimpleName());
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void restoreMenuEntry(Object settingsConfig) {
        if (settingsConfig == null) return;

        try {
            Object value = XposedHelpers.getObjectField(settingsConfig, "mMenuPanelList");
            if (!(value instanceof List)) {
                CameraBoostLog.log("HighResolution menu restore skipped: panel list unavailable");
                return;
            }

            List list = (List) value;
            if (list.contains(MENU_KEY)) {
                CameraBoostLog.log("HighResolution native menu already present");
                return;
            }

            int index = list.indexOf(INSERT_BEFORE);
            if (index >= 0) {
                list.add(index, MENU_KEY);
            } else {
                list.add(MENU_KEY);
            }

            CameraBoostLog.log("HighResolution native menu restored: "
                    + MENU_KEY + ", index=" + (index >= 0 ? index : list.size() - 1));
        } catch (Throwable t) {
            CameraBoostLog.error("HighResolution menu restore", t);
        }
    }
}
