package dev.cameraboost.oplus10bit;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * Gallery 17.x watermark compatibility/unlock layer.
 *
 * The OPPO and OnePlus Gallery packages share the same implementation base and most
 * watermark assets. Brand filtering is only relaxed while watermark UI/model code is
 * executing, so the rest of Gallery keeps its real product identity.
 */
final class GalleryCompatHook {
    private static final String OTHER_SYSTEM_STORAGE =
            "com.oplus.gallery.framework.abilities.config.storage.OtherSystemStorage";
    private static final String WATERMARK_MASTER_VM =
            "com.oplus.gallery.photoeditor.editingvvm.watermarkmaster.WatermarkMasterVM";
    private static final String WATERMARK_CAMERA_SECTION =
            "com.oplus.gallery.photoeditor.editingvvm.watermarkCamera.WatermarkCameraSection";
    private static final String AI_WATERMARK_VIEW =
            "com.oplus.gallery.photoeditor.widget.layout.AIWatermarkView";

    private static final ThreadLocal<Integer> WATERMARK_SCOPE =
            ThreadLocal.withInitial(() -> 0);

    private static final Set<String> FORCE_TRUE = Collections.unmodifiableSet(
            new LinkedHashSet<>(java.util.Arrays.asList(
                    "feature_is_support_watermark_master",
                    "feature_is_support_lumo_watermark",
                    "feature_is_support_hassel_device",
                    "feature_is_support_hassel_watermark",
                    "feature_is_support_photo_editor_watermark",
                    "feature_is_support_photo_editor_frame_watermark",
                    "feature_is_support_color_watermark",
                    "feature_is_support_spring_festival_watermark",
                    "feature_is_support_lonely_planet_watermark",
                    "feature_is_support_street_watermark",
                    "feature_is_support_privacy_watermark",
                    "feature_is_support_ipu_watermark",
                    "feature_is_support_show_ai_logo",
                    "feature_is_support_ai_composition",
                    "feature_is_support_color_palette",
                    "feature_is_support_ipu_color_palette"
            ))
    );

    private static final Set<String> BRAND_KEYS = Collections.unmodifiableSet(
            new LinkedHashSet<>(java.util.Arrays.asList(
                    "is_oppo_brand",
                    "is_oneplus_brand"
            ))
    );

    private static final ConcurrentHashMap<Class<?>, Field[]> STRING_FIELDS =
            new ConcurrentHashMap<>();
    private static final Set<String> LOGGED = ConcurrentHashMap.newKeySet();

    private GalleryCompatHook() {}

    static void install(ClassLoader classLoader) {
        if (!BuildConfig.ENABLE_COLOROS17_COMPAT || !FeaturePolicy.isTargetDevice()) {
            return;
        }

        hookWatermarkScope(classLoader);
        hookFeatureStorage(classLoader);
        hookStyleBlacklist(classLoader);
        CameraBoostLog.log("Gallery watermark compatibility layer installed");
    }

    private static void hookWatermarkScope(ClassLoader classLoader) {
        hookScopeMethods(classLoader, WATERMARK_MASTER_VM, "F", "K");

        try {
            Class<?> cls = XposedHelpers.findClass(AI_WATERMARK_VIEW, classLoader);
            XposedBridge.hookAllConstructors(cls, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    enterScope();
                }

                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    exitScope();
                }
            });
            CameraBoostLog.log("Gallery AIWatermarkView brand scope installed");
        } catch (Throwable t) {
            CameraBoostLog.log("Gallery AIWatermarkView unavailable: "
                    + t.getClass().getSimpleName());
        }
    }

    private static void hookScopeMethods(
            ClassLoader classLoader, String className, String... methodNames) {
        try {
            Class<?> cls = XposedHelpers.findClass(className, classLoader);
            for (String methodName : methodNames) {
                XposedBridge.hookAllMethods(cls, methodName, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        enterScope();
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        if ("K".equals(methodName) && param.getResult() instanceof Collection) {
                            logOnce("watermark-master-list-size",
                                    "WatermarkMasterVM#K styles="
                                            + ((Collection<?>) param.getResult()).size());
                        }
                        exitScope();
                    }
                });
            }
            CameraBoostLog.log("Gallery watermark brand scope installed on " + className);
        } catch (Throwable t) {
            CameraBoostLog.log("Gallery watermark scope unavailable on " + className + ": "
                    + t.getClass().getSimpleName());
        }
    }

    private static void hookFeatureStorage(ClassLoader classLoader) {
        try {
            Class<?> cls = XposedHelpers.findClass(OTHER_SYSTEM_STORAGE, classLoader);

            XposedBridge.hookAllMethods(cls, "h", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (!(param.method instanceof Method)) {
                        return;
                    }
                    Method method = (Method) param.method;
                    if (method.getReturnType() != Boolean.class
                            && method.getReturnType() != boolean.class) {
                        return;
                    }

                    String key = findConfigKey(param.args);
                    if (key == null) {
                        return;
                    }

                    if (FORCE_TRUE.contains(key)
                            || (inWatermarkScope() && BRAND_KEYS.contains(key))) {
                        param.setResult(true);
                        logOnce("gallery-true:" + key, "Gallery gate forced true: " + key);
                    }
                }
            });

            XposedBridge.hookAllMethods(cls, "d", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    String key = findConfigKey(param.args);
                    if (key == null || !key.startsWith("watermark_style_id_blacklist")) {
                        return;
                    }

                    Object cleared = emptyValueLike(param.getResult());
                    if (cleared != null) {
                        param.setResult(cleared);
                        logOnce("gallery-blacklist:" + key,
                                "Gallery watermark blacklist cleared: " + key);
                    }
                }
            });

            CameraBoostLog.log("Gallery OtherSystemStorage hooks installed");
        } catch (Throwable t) {
            CameraBoostLog.log("Gallery OtherSystemStorage unavailable: "
                    + t.getClass().getSimpleName());
        }
    }

    /**
     * 17.10.7 (OnePlus) and 17.10.16 (OPPO) share this predicate and signature.
     * Returning false means the style id is not blocked by the device/OS blacklist.
     */
    private static void hookStyleBlacklist(ClassLoader classLoader) {
        try {
            Class<?> cls = XposedHelpers.findClass(WATERMARK_CAMERA_SECTION, classLoader);
            XposedBridge.hookAllMethods(cls, "p0", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (param.method instanceof Method
                            && ((Method) param.method).getReturnType() == boolean.class) {
                        param.setResult(false);
                    }
                }
            });
            CameraBoostLog.log("Gallery WatermarkCameraSection blacklist bypass installed");
        } catch (Throwable t) {
            CameraBoostLog.log("Gallery WatermarkCameraSection unavailable: "
                    + t.getClass().getSimpleName());
        }
    }

    private static String findConfigKey(Object[] args) {
        if (args == null) {
            return null;
        }

        for (Object arg : args) {
            String key = extractConfigKey(arg);
            if (key != null) {
                return key;
            }
        }
        return null;
    }

    private static String extractConfigKey(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof String) {
            return normalizeKey((String) value);
        }

        String direct = normalizeKey(String.valueOf(value));
        if (direct != null) {
            return direct;
        }

        Field[] fields = STRING_FIELDS.computeIfAbsent(
                value.getClass(),
                GalleryCompatHook::collectStringFields
        );
        for (Field field : fields) {
            try {
                Object v = field.get(value);
                if (v instanceof String) {
                    String key = normalizeKey((String) v);
                    if (key != null) {
                        return key;
                    }
                }
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private static Field[] collectStringFields(Class<?> type) {
        ArrayList<Field> out = new ArrayList<>();
        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field field : c.getDeclaredFields()) {
                if (field.getType() == String.class) {
                    try {
                        field.setAccessible(true);
                        out.add(field);
                    } catch (Throwable ignored) {
                    }
                }
            }
        }
        return out.toArray(new Field[0]);
    }

    private static String normalizeKey(String text) {
        if (text == null) {
            return null;
        }

        for (String key : FORCE_TRUE) {
            if (text.equals(key) || text.contains(key)) {
                return key;
            }
        }
        for (String key : BRAND_KEYS) {
            if (text.equals(key) || text.contains(key)) {
                return key;
            }
        }
        if (text.contains("watermark_style_id_blacklist_os17")) {
            return "watermark_style_id_blacklist_os17";
        }
        if (text.contains("watermark_style_id_blacklist")) {
            return "watermark_style_id_blacklist";
        }
        return null;
    }

    private static Object emptyValueLike(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof String) {
            return "";
        }
        if (value instanceof Set) {
            return Collections.emptySet();
        }
        if (value instanceof java.util.List) {
            return Collections.emptyList();
        }
        if (value instanceof Map) {
            return Collections.emptyMap();
        }
        if (value instanceof Collection) {
            return Collections.emptyList();
        }
        Class<?> type = value.getClass();
        if (type.isArray()) {
            return Array.newInstance(type.getComponentType(), 0);
        }
        return null;
    }

    private static void enterScope() {
        WATERMARK_SCOPE.set(WATERMARK_SCOPE.get() + 1);
    }

    private static void exitScope() {
        int depth = WATERMARK_SCOPE.get() - 1;
        if (depth <= 0) {
            WATERMARK_SCOPE.remove();
        } else {
            WATERMARK_SCOPE.set(depth);
        }
    }

    private static boolean inWatermarkScope() {
        return WATERMARK_SCOPE.get() > 0;
    }

    private static void logOnce(String id, String message) {
        if (LOGGED.add(id)) {
            CameraBoostLog.log(message);
        }
    }
}
