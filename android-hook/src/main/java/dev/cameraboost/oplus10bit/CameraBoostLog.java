package dev.cameraboost.oplus10bit;

import de.robv.android.xposed.XposedBridge;

final class CameraBoostLog {
    private CameraBoostLog() {}

    static void log(String message) {
        XposedBridge.log("CameraBoost17: " + message);
    }
}
