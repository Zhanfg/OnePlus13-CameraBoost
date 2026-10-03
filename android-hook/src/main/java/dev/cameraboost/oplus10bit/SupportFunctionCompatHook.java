package dev.cameraboost.oplus10bit;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * Hooks every getSupportFunction(String) implementation declared by the loaded Camera APK.
 *
 * Camera 7.x moves mode subclasses aggressively (gm.*, rm.*, im.*, etc.). Discovering
 * owners from method_ids avoids maintaining a fragile hard-coded class list.
 */
final class SupportFunctionCompatHook {
    private static final Set<String> HOOKED = ConcurrentHashMap.newKeySet();

    private SupportFunctionCompatHook() {}

    static void install(
            String apkPath,
            ClassLoader classLoader,
            OplusCapabilityResolver resolver
    ) {
        Set<String> owners = DexMethodOwnerIndex.findOwners(apkPath, "getSupportFunction");
        int installed = 0;

        for (String className : owners) {
            Class<?> cls;
            try {
                cls = XposedHelpers.findClassIfExists(className, classLoader);
            } catch (Throwable t) {
                cls = null;
            }
            if (cls == null) {
                continue;
            }

            for (Method method : cls.getDeclaredMethods()) {
                if (!"getSupportFunction".equals(method.getName())
                        || (method.getReturnType() != boolean.class
                        && method.getReturnType() != Boolean.class)) {
                    continue;
                }

                boolean hasString = false;
                for (Class<?> p : method.getParameterTypes()) {
                    if (p == String.class) {
                        hasString = true;
                        break;
                    }
                }
                if (!hasString) {
                    continue;
                }

                String identity = className + "#" + method.toGenericString();
                if (!HOOKED.add(identity)) {
                    continue;
                }

                try {
                    method.setAccessible(true);
                    XposedBridge.hookMethod(method, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            String key = firstString(param.args);
                            if (resolver.shouldForceBoolean(key)) {
                                param.setResult(true);
                            }
                        }
                    });
                    installed++;
                } catch (Throwable t) {
                    CameraBoostLog.log("getSupportFunction hook skipped "
                            + identity + ": " + t.getClass().getSimpleName());
                }
            }
        }

        CameraBoostLog.log("dynamic getSupportFunction hooks installed=" + installed);
    }

    private static String firstString(Object[] args) {
        if (args == null) return "";
        for (Object arg : args) {
            if (arg instanceof String) return (String) arg;
        }
        return "";
    }
}
