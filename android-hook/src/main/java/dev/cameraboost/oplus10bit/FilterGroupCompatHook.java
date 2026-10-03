package dev.cameraboost.oplus10bit;

import java.lang.reflect.Field;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

final class FilterGroupCompatHook {
    private static final String MANAGER =
            "com.oplus.ocs.camera.ipusdk.processunit.filter.list.FilterGroupManager";

    private FilterGroupCompatHook() {}

    static void install(ClassLoader loader, RuntimeArchitecture runtime) {
        if (!runtime.filterGroupManager) {
            log("FilterGroupManager unavailable; skipping filter compatibility hook");
            return;
        }

        try {
            Class<?> manager = XposedHelpers.findClass(MANAGER, loader);
            XC_MethodHook syncHook = new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    syncGroups(manager);
                }
            };

            XposedBridge.hookAllMethods(manager, "init", syncHook);
            XposedBridge.hookAllMethods(manager, "initProFilterGroup", syncHook);

            // Apply once in case the class has already initialized before the hook is installed.
            syncGroups(manager);
            log("installed structure-based FilterGroup compatibility hook");
        } catch (Throwable t) {
            log("FilterGroup compatibility unavailable: " + t.getClass().getSimpleName()
                    + ": " + t.getMessage());
        }
    }

    private static void syncGroups(Class<?> manager) {
        try {
            Object normal = getStaticIfPresent(manager, "sFilterGroup");
            if (normal != null && hasField(manager, "sProFilterGroup")) {
                setStatic(manager, "sProFilterGroup", normal);
            }

            // These exist in some pre-ColorOS 17 builds. Keep them opportunistic;
            // never make a missing legacy field fatal.
            setStaticBooleanIfPresent(manager, "sbIsBrandOplusR", false);
            setStaticBooleanIfPresent(manager, "sbIsExport", false);

            logOnce("filter-sync", "synchronized normal/pro filter groups without legacy field assumptions");
        } catch (Throwable t) {
            logOnce("filter-sync-error",
                    "filter-group synchronization failed open: " + t.getClass().getSimpleName());
        }
    }

    private static Object getStaticIfPresent(Class<?> cls, String name) throws IllegalAccessException {
        try {
            Field f = cls.getDeclaredField(name);
            f.setAccessible(true);
            return f.get(null);
        } catch (NoSuchFieldException ignored) {
            return null;
        }
    }

    private static void setStatic(Class<?> cls, String name, Object value)
            throws NoSuchFieldException, IllegalAccessException {
        Field f = cls.getDeclaredField(name);
        f.setAccessible(true);
        f.set(null, value);
    }

    private static boolean hasField(Class<?> cls, String name) {
        try {
            cls.getDeclaredField(name);
            return true;
        } catch (NoSuchFieldException ignored) {
            return false;
        }
    }

    private static void setStaticBooleanIfPresent(Class<?> cls, String name, boolean value)
            throws IllegalAccessException {
        try {
            Field f = cls.getDeclaredField(name);
            f.setAccessible(true);
            f.setBoolean(null, value);
        } catch (NoSuchFieldException ignored) {
            // Newer camera builds legitimately remove old compatibility fields.
        }
    }

    private static final java.util.Set<String> LOGGED =
            java.util.concurrent.ConcurrentHashMap.newKeySet();

    private static void logOnce(String key, String message) {
        if (LOGGED.add(key)) {
            log(message);
        }
    }

    private static void log(String message) {
        XposedBridge.log("CameraBoostFull: " + message);
    }
}
