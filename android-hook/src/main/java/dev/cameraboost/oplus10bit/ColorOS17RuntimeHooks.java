package dev.cameraboost.oplus10bit;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

final class ColorOS17RuntimeHooks {
    private ColorOS17RuntimeHooks() {}

    static void install(ClassLoader classLoader) {
        if (!BuildConfig.ENABLE_COLOROS17_COMPAT) {
            return;
        }

        installAiCompositionObserver(classLoader);
        installLutPathFallback(classLoader);
        installFilterGroupCompat(classLoader);
        installLivePhotoCompat(classLoader);
    }

    private static void installAiCompositionObserver(ClassLoader classLoader) {
        try {
            Class<?> helper = XposedHelpers.findClass(
                    "com.oplus.ocs.camera.OplusAICompositionHelper",
                    classLoader
            );
            XposedBridge.hookAllMethods(helper, "initComposition", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    log("modern OplusAICompositionHelper initialized");
                }
            });
            log("installed modern AI Composition observer");
        } catch (Throwable t) {
            log("modern AI Composition helper unavailable: " + t.getClass().getSimpleName());
        }
    }

    private static void installLutPathFallback(ClassLoader classLoader) {
        String fallback = ColorOS17CompatResolver.get().getMeisheLutDirectory();
        if (fallback == null) {
            log("Meishe LUT fallback directory not present");
            return;
        }

        try {
            Class<?> render = XposedHelpers.findClass(
                    "com.meicam.effect.oppo.MeisheRender",
                    classLoader
            );
            XposedBridge.hookAllMethods(render, "setStaticLUTPath", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (param.args == null || param.args.length == 0
                            || !(param.args[0] instanceof String)) {
                        return;
                    }

                    String requested = (String) param.args[0];
                    boolean invalid = requested == null || requested.isEmpty();
                    if (!invalid) {
                        try {
                            invalid = !new File(requested).isDirectory();
                        } catch (Throwable ignored) {
                            invalid = true;
                        }
                    }

                    if (invalid) {
                        param.args[0] = fallback;
                        log("redirected missing Meishe LUT path to " + fallback);
                    }
                }
            });
            log("installed Meishe LUT path fallback");
        } catch (Throwable t) {
            log("MeisheRender#setStaticLUTPath unavailable: " + t.getClass().getSimpleName());
        }
    }

    private static void installFilterGroupCompat(ClassLoader classLoader) {
        try {
            Class<?> ocsManager = XposedHelpers.findClass(
                    "com.oplus.ocs.camera.ipusdk.processunit.filter.list.FilterGroupManager",
                    classLoader
            );

            // Older builds exposed brand/export gates. Newer ColorOS 17 builds may remove
            // them entirely, so every field mutation is optional.
            setStaticBooleanIfPresent(ocsManager, "sbIsBrandOplusR", false);
            setStaticBooleanIfPresent(ocsManager, "sbIsExport", false);

            log("OCS FilterGroupManager present; optional legacy brand gates processed");
        } catch (Throwable t) {
            log("OCS FilterGroupManager unavailable: " + t.getClass().getSimpleName());
        }

        try {
            Class<?> modernManager = XposedHelpers.findClass(
                    "com.oplus.camera.filter.FilterGroupManager",
                    classLoader
            );
            XposedBridge.hookAllMethods(modernManager, "initFromIpu", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    log("modern FilterGroupManager initialized from IPU");
                }
            });
            log("installed ColorOS 17 FilterGroupManager observer");
        } catch (Throwable t) {
            log("modern FilterGroupManager unavailable: " + t.getClass().getSimpleName());
        }
    }

    private static void installLivePhotoCompat(ClassLoader classLoader) {
        ColorOS17CompatResolver resolver = ColorOS17CompatResolver.get();
        if (!resolver.supportsLivePhotoStack()) {
            log("Live Photo stack not present; compatibility hook skipped");
            return;
        }

        String gateClass = resolver.resolveLivePhotoGateClass();
        if (gateClass != null) {
            log("Live Photo semantic gate resolved to " + gateClass);
        } else {
            log("Live Photo stack present; semantic gate class not uniquely resolved");
        }

        // The actual enablement is performed through the feature/config gate layer.
        // We intentionally avoid hard-coding ColorOS 16 names (nc.c/hj.l1/sj.i).
    }

    private static void setStaticBooleanIfPresent(
            Class<?> cls,
            String fieldName,
            boolean value
    ) {
        try {
            Field field = cls.getDeclaredField(fieldName);
            if (!Modifier.isStatic(field.getModifiers())
                    || field.getType() != boolean.class) {
                return;
            }
            field.setAccessible(true);
            field.setBoolean(null, value);
            log("set optional " + cls.getName() + "#" + fieldName + "=" + value);
        } catch (NoSuchFieldException ignored) {
            log("legacy filter field absent as expected on ColorOS 17: " + fieldName);
        } catch (Throwable t) {
            log("failed optional filter field " + fieldName + ": "
                    + t.getClass().getSimpleName());
        }
    }

    private static void log(String message) {
        XposedBridge.log("CameraBoost[ColorOS17]: " + message);
    }
}
