package dev.cameraboost.oplus10bit;

import java.lang.reflect.Field;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

final class FilterCompatHook {
    private static final String MANAGER =
            "com.oplus.ocs.camera.ipusdk.processunit.filter.list.FilterGroupManager";

    private FilterCompatHook() {}

    static void install(ClassLoader classLoader, RuntimeCapabilityResolver resolver) {
        if (!resolver.supportsFilterCore()) {
            return;
        }

        Class<?> cls = XposedHelpers.findClassIfExists(MANAGER, classLoader);
        if (cls == null) {
            return;
        }

        setBooleanIfPresent(cls, "sbIsBrandOplusR", false);
        setBooleanIfPresent(cls, "sbIsExport", false);

        try {
            XposedBridge.hookAllMethods(cls, "init", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    mirrorMainFilterGroupIntoProIfPossible(cls);
                }
            });
            CameraBoostLog.log("ColorOS17 FilterGroup init compatibility hook installed");
        } catch (Throwable t) {
            CameraBoostLog.log("FilterGroup init hook unavailable: "
                    + t.getClass().getSimpleName());
        }
    }

    private static void mirrorMainFilterGroupIntoProIfPossible(Class<?> cls) {
        try {
            Field main = XposedHelpers.findFieldIfExists(cls, "sFilterGroup");
            Field pro = XposedHelpers.findFieldIfExists(cls, "sProFilterGroup");
            if (main == null || pro == null) {
                return;
            }
            main.setAccessible(true);
            pro.setAccessible(true);
            Object value = main.get(null);
            if (value != null) {
                pro.set(null, value);
                CameraBoostLog.log("FilterGroup modern path: sProFilterGroup <- sFilterGroup");
            }
        } catch (Throwable t) {
            CameraBoostLog.log("FilterGroup modern mirror failed: " + t.getClass().getSimpleName());
        }
    }

    private static void setBooleanIfPresent(Class<?> cls, String fieldName, boolean value) {
        try {
            Field field = XposedHelpers.findFieldIfExists(cls, fieldName);
            if (field == null) {
                return;
            }
            field.setAccessible(true);
            field.setBoolean(null, value);
            CameraBoostLog.log("FilterGroup legacy gate patched: " + fieldName + "=" + value);
        } catch (Throwable t) {
            CameraBoostLog.log("FilterGroup gate " + fieldName + " skipped: "
                    + t.getClass().getSimpleName());
        }
    }
}
