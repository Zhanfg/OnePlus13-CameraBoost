package dev.cameraboost.oplus10bit;

import java.lang.reflect.Field;
import java.util.Arrays;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * ColorOS 17 compatibility layer.
 *
 * It removes software/brand gates only when a matching implementation is present.
 * Hardware-sensitive capture paths remain guarded by capability evidence.
 */
final class ColorOs17CompatHook {
    private static final String CONFIG_FEATURE_IMPL =
            "com.oplus.ocs.camera.configure.ConfigFeatureImpl";

    private static final String[] FILTER_GROUP_CLASSES = {
            "com.oplus.camera.filter.FilterGroupManager",
            "com.oplus.ocs.camera.ipusdk.processunit.filter.list.FilterGroupManager"
    };

    private ColorOs17CompatHook() {}

    static void install(ClassLoader classLoader, OplusCapabilityResolver resolver) {
        installFeatureValueLegalHook(classLoader, resolver);
        installFilterGroupCompat(classLoader, resolver);
        log("resolver: " + resolver.describe());
    }

    private static void installFeatureValueLegalHook(
            ClassLoader classLoader,
            OplusCapabilityResolver resolver
    ) {
        try {
            Class<?> cls = XposedHelpers.findClass(CONFIG_FEATURE_IMPL, classLoader);
            XposedBridge.hookAllMethods(cls, "isFeatureValueLegal", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    String key = firstStringArg(param.args);
                    Object value = firstNonStringArg(param.args);
                    if (resolver.shouldAllowFeatureValue(key, value)) {
                        param.setResult(true);
                    }
                }
            });
            log("installed ConfigFeatureImpl#isFeatureValueLegal compatibility hook");
        } catch (Throwable t) {
            log("ConfigFeatureImpl path unavailable: " + t.getClass().getSimpleName());
        }
    }

    private static void installFilterGroupCompat(
            ClassLoader classLoader,
            OplusCapabilityResolver resolver
    ) {
        if (!resolver.hasModernFilterGroup() && !resolver.hasLegacyFilterGroup()) {
            log("no FilterGroupManager implementation detected");
            return;
        }

        for (String className : FILTER_GROUP_CLASSES) {
            Class<?> cls;
            try {
                cls = XposedHelpers.findClass(className, classLoader);
            } catch (Throwable ignored) {
                continue;
            }

            // Old OCS implementation used explicit brand/export gates. Newer builds may
            // remove them; missing fields are treated as normal migration, not failure.
            setStaticBooleanIfPresent(cls, "sbIsBrandOplusR", false);
            setStaticBooleanIfPresent(cls, "sbIsExport", false);

            XC_MethodHook refresh = new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    mirrorFilterGroupIfPresent(cls);
                }
            };

            for (String method : Arrays.asList(
                    "init",
                    "initProFilterGroup",
                    "initHasselbladXpanFilterGroup"
            )) {
                try {
                    XposedBridge.hookAllMethods(cls, method, refresh);
                } catch (Throwable ignored) {
                    // Method names legitimately differ between camera generations.
                }
            }

            // Some 7.x builds initialize static groups before our hook gets a callback.
            mirrorFilterGroupIfPresent(cls);
            log("installed FilterGroup compatibility on " + className);
        }
    }

    private static void mirrorFilterGroupIfPresent(Class<?> cls) {
        try {
            Field base = findFieldOrNull(cls, "sFilterGroup");
            Field pro = findFieldOrNull(cls, "sProFilterGroup");
            if (base == null || pro == null) {
                return;
            }

            Object value = base.get(null);
            if (value != null) {
                pro.set(null, value);
            }
        } catch (Throwable t) {
            log("FilterGroup mirror skipped on " + cls.getName() + ": "
                    + t.getClass().getSimpleName());
        }
    }

    private static void setStaticBooleanIfPresent(Class<?> cls, String fieldName, boolean value) {
        try {
            Field field = findFieldOrNull(cls, fieldName);
            if (field != null && (field.getType() == boolean.class || field.getType() == Boolean.class)) {
                field.set(null, value);
            }
        } catch (Throwable t) {
            log("field " + cls.getSimpleName() + "#" + fieldName + " skipped: "
                    + t.getClass().getSimpleName());
        }
    }

    private static Field findFieldOrNull(Class<?> cls, String fieldName) {
        try {
            Field field = cls.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static String firstStringArg(Object[] args) {
        if (args == null) {
            return "";
        }
        for (Object arg : args) {
            if (arg instanceof String) {
                return (String) arg;
            }
        }
        return "";
    }

    private static Object firstNonStringArg(Object[] args) {
        if (args == null) {
            return null;
        }
        for (Object arg : args) {
            if (arg != null && !(arg instanceof String)) {
                return arg;
            }
        }
        return null;
    }

    private static void log(String message) {
        XposedBridge.log("CameraBoostCompat17: " + message);
    }
}
