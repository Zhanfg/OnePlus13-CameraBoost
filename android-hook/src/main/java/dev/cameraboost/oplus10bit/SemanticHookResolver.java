package dev.cameraboost.oplus10bit;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.MethodData;
import org.luckypray.dexkit.result.MethodDataList;

final class SemanticHookResolver {
    static {
        System.loadLibrary("dexkit");
    }

    private SemanticHookResolver() {}

    static void install(String apkPath, ClassLoader classLoader) {
        try (DexKitBridge bridge = DexKitBridge.create(apkPath)) {
            hookBooleanAnchorGroup(
                    bridge,
                    classLoader,
                    "AIComposition",
                    new String[] {
                            "com.oplus.feature.aicomposition.realscene.support",
                            "com.oplus.feature.aicomposition.inspiration.support",
                            "com.oplus.ai.capture.guide.support"
                    }
            );

            hookNamedBooleanMethods(
                    bridge,
                    classLoader,
                    "AIComposition support",
                    new String[] {"isSupportAIComposition"}
            );

            hookBooleanAnchorGroup(
                    bridge,
                    classLoader,
                    "LivePhoto",
                    new String[] {
                            "com.oplus.camera.livephoto.support",
                            "com.oplus.camera.feature.live_photo",
                            "com.oplus.livephoto.support.heif",
                            "camera_high_pixel_live_photo"
                    }
            );

            hookNamedBooleanMethods(
                    bridge,
                    classLoader,
                    "LivePhoto support",
                    new String[] {"isLivePhotoSupported"}
            );

            hookBooleanAnchorGroup(
                    bridge,
                    classLoader,
                    "Filters/XPAN",
                    new String[] {
                            "com.oplus.feature.xpan.mode.support",
                            "com.oplus.xpan.all.camera.support",
                            "com.oplus.support.grand.tour.filter",
                            "com.oplus.camera.feature.gr_photo"
                    }
            );

            hookBooleanAnchorGroup(
                    bridge,
                    classLoader,
                    "HDR/MAX",
                    new String[] {
                            "com.oplus.feature.ultra.hdr.enable",
                            "com.oplus.highpicture.pro.support",
                            "com.oplus.feature.high.pixel.support",
                            "com.oplus.high.resolution.support"
                    }
            );
        } catch (Throwable t) {
            log("resolver failed: " + t);
        }
    }

    private static void hookBooleanAnchorGroup(
            DexKitBridge bridge,
            ClassLoader classLoader,
            String label,
            String[] anchors
    ) {
        Set<String> hooked = new LinkedHashSet<>();
        for (String anchor : anchors) {
            try {
                MethodDataList list = bridge.findMethod(
                        FindMethod.create().matcher(
                                MethodMatcher.create()
                                        .returnType("boolean")
                                        .usingStrings(anchor)
                        )
                );
                for (MethodData data : list) {
                    Method method = data.getMethodInstance(classLoader);
                    String id = method.getDeclaringClass().getName() + "#" + method.getName()
                            + Arrays.toString(method.getParameterTypes());
                    if (!hooked.add(id)) continue;
                    XposedBridge.hookMethod(method, XC_MethodReplacement.returnConstant(true));
                    log(label + " semantic gate -> " + id + " [anchor=" + anchor + "]");
                }
            } catch (Throwable t) {
                log(label + " anchor unresolved: " + anchor + " (" + t.getClass().getSimpleName() + ")");
            }
        }
    }

    private static void hookNamedBooleanMethods(
            DexKitBridge bridge,
            ClassLoader classLoader,
            String label,
            String[] methodNames
    ) {
        Set<String> hooked = new LinkedHashSet<>();
        for (String name : methodNames) {
            try {
                MethodDataList list = bridge.findMethod(
                        FindMethod.create().matcher(
                                MethodMatcher.create()
                                        .name(name)
                                        .returnType("boolean")
                        )
                );
                for (MethodData data : list) {
                    Method method = data.getMethodInstance(classLoader);
                    String id = method.getDeclaringClass().getName() + "#" + method.getName()
                            + Arrays.toString(method.getParameterTypes());
                    if (!hooked.add(id)) continue;
                    XposedBridge.hookMethod(method, XC_MethodReplacement.returnConstant(true));
                    log(label + " named gate -> " + id);
                }
            } catch (Throwable t) {
                log(label + " method unresolved: " + name + " (" + t.getClass().getSimpleName() + ")");
            }
        }
    }

    private static void log(String message) {
        XposedBridge.log("CameraBoost17[Semantic]: " + message);
    }
}
