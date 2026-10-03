package dev.cameraboost.oplus10bit;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

final class FilterGroupCompatHook {
    private static final String IPU_MANAGER =
            "com.oplus.ocs.camera.ipusdk.processunit.filter.list.FilterGroupManager";
    private static final String APP_MANAGER =
            "com.oplus.camera.filter.FilterGroupManager";

    private static final Set<String> LOGGED = ConcurrentHashMap.newKeySet();

    private FilterGroupCompatHook() {}

    static void install(ClassLoader loader, RuntimeArchitecture runtime) {
        if (runtime.ipuFilterGroupManager) {
            installManager(loader, IPU_MANAGER, false);
        }
        if (runtime.appFilterGroupManager) {
            installManager(loader, APP_MANAGER, true);
        }
        if (!runtime.filterGroupManager) {
            log("FilterGroupManager unavailable; skipping filter compatibility hook");
        }
    }

    private static void installManager(
            ClassLoader loader,
            String className,
            boolean modern
    ) {
        try {
            Class<?> manager = XposedHelpers.findClass(className, loader);
            int hooks = 0;

            for (Method method : manager.getDeclaredMethods()) {
                if (!method.getName().startsWith("init")) {
                    continue;
                }
                method.setAccessible(true);
                XposedBridge.hookMethod(method, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        syncGroups(manager, modern);
                    }
                });
                hooks++;
            }

            // Apply once in case static initialization happened before module install.
            syncGroups(manager, modern);
            log("installed " + (modern ? "ColorOS17 app" : "OCS/IPU")
                    + " FilterGroup compatibility hook; initHooks=" + hooks);
        } catch (Throwable t) {
            log("FilterGroup compatibility unavailable on " + className + ": "
                    + t.getClass().getSimpleName() + ": " + t.getMessage());
        }
    }

    private static void syncGroups(Class<?> manager, boolean modern) {
        try {
            Object normal = getStaticIfPresent(manager, "sFilterGroup");
            if (normal == null || !hasField(manager, "sProFilterGroup")) {
                return;
            }

            Object pro = getStaticIfPresent(manager, "sProFilterGroup");

            if (modern) {
                if (pro == null) {
                    Object copy = copyGroup(normal);
                    setStatic(manager, "sProFilterGroup", copy != null ? copy : normal);
                } else {
                    mergeModernGroup(pro, normal);
                }
            } else {
                // Legacy OCS FilterGroup does not expose a stable copy API.
                // Sharing the normal group is the same behavior used by the
                // earlier OPCameraPro unlock path.
                setStatic(manager, "sProFilterGroup", normal);
            }

            // Present only on older branches; never make their absence fatal.
            setStaticBooleanIfPresent(manager, "sbIsBrandOplusR", false);
            setStaticBooleanIfPresent(manager, "sbIsExport", false);

            logOnce(
                    "filter-sync:" + manager.getName(),
                    "synchronized normal/pro filter groups on " + manager.getName()
            );
        } catch (Throwable t) {
            logOnce(
                    "filter-sync-error:" + manager.getName(),
                    "filter-group synchronization failed open on " + manager.getName()
                            + ": " + t.getClass().getSimpleName()
            );
        }
    }

    private static Object copyGroup(Object source) {
        try {
            Method copy = source.getClass().getDeclaredMethod("copy");
            copy.setAccessible(true);
            return copy.invoke(source);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void mergeModernGroup(Object target, Object source) {
        try {
            Method copyFrom = target.getClass().getDeclaredMethod(
                    "copyFrom",
                    source.getClass(),
                    boolean.class,
                    boolean.class
            );
            copyFrom.setAccessible(true);
            copyFrom.invoke(target, source, true, true);
        } catch (Throwable ignored) {
            // Keep the host's existing pro group if the copy API changes.
        }
    }

    private static Object getStaticIfPresent(Class<?> cls, String name)
            throws IllegalAccessException {
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
            // Expected on ColorOS 17 where these legacy fields were removed.
        }
    }

    private static void logOnce(String key, String message) {
        if (LOGGED.add(key)) {
            log(message);
        }
    }

    private static void log(String message) {
        XposedBridge.log("CameraBoostFull: " + message);
    }
}
