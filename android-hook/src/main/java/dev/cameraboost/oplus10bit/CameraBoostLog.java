package dev.cameraboost.oplus10bit;

import android.util.Log;

import de.robv.android.xposed.XposedBridge;

final class CameraBoostLog {
    private static final String TAG = "CameraBoost17";

    private CameraBoostLog() {}

    static void log(String message) {
        try {
            Log.e(TAG, message);
        } catch (Throwable ignored) {
        }
        try {
            XposedBridge.log(TAG + ": " + message);
        } catch (Throwable ignored) {
        }
    }

    static void error(String stage, Throwable t) {
        String message = stage + " failed: "
                + t.getClass().getName()
                + (t.getMessage() == null ? "" : ": " + t.getMessage());
        try {
            Log.e(TAG, message, t);
        } catch (Throwable ignored) {
        }
        try {
            XposedBridge.log(TAG + ": " + message);
            XposedBridge.log(t);
        } catch (Throwable ignored) {
        }
    }
}
