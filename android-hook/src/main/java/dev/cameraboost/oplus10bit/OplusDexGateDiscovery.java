package dev.cameraboost.oplus10bit;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Discovers OPlus feature/config strings directly from the currently loaded camera APK.
 *
 * This is intentionally enabled only for the experimental full-unlock variant. It does
 * not need class names and therefore survives normal R8/ProGuard class renaming.
 */
final class OplusDexGateDiscovery {
    private static final byte[] PREFIX = "com.oplus.".getBytes(StandardCharsets.US_ASCII);
    private static final int MAX_STRING = 240;

    private OplusDexGateDiscovery() {}

    static Set<String> scanApk(String apkPath) {
        if (!BuildConfig.ENABLE_EXPERIMENTAL_ALL || apkPath == null) {
            return Collections.emptySet();
        }

        LinkedHashSet<String> found = new LinkedHashSet<>();
        long start = android.os.SystemClock.elapsedRealtime();

        try (ZipInputStream zis = new ZipInputStream(
                new BufferedInputStream(new FileInputStream(apkPath), 256 * 1024))) {
            ZipEntry entry;
            byte[] buffer = new byte[128 * 1024];

            int prefixState = 0;
            StringBuilder capture = null;

            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();
                if (name == null || !name.startsWith("classes") || !name.endsWith(".dex")) {
                    continue;
                }

                prefixState = 0;
                capture = null;
                int read;
                while ((read = zis.read(buffer)) > 0) {
                    for (int i = 0; i < read; i++) {
                        int b = buffer[i] & 0xff;

                        if (capture != null) {
                            if (isGateChar(b) && capture.length() < MAX_STRING) {
                                capture.append((char) b);
                                continue;
                            }

                            addIfPositive(found, capture.toString());
                            capture = null;
                            prefixState = b == (PREFIX[0] & 0xff) ? 1 : 0;
                            continue;
                        }

                        int expected = PREFIX[prefixState] & 0xff;
                        if (b == expected) {
                            prefixState++;
                            if (prefixState == PREFIX.length) {
                                capture = new StringBuilder("com.oplus.");
                                prefixState = 0;
                            }
                        } else {
                            prefixState = b == (PREFIX[0] & 0xff) ? 1 : 0;
                        }
                    }
                }

                if (capture != null) {
                    addIfPositive(found, capture.toString());
                }
            }
        } catch (Throwable t) {
            CameraBoostLog.log("dynamic OPlus gate scan failed: "
                    + t.getClass().getSimpleName() + ": " + t.getMessage());
        }

        CameraBoostLog.log("dynamic OPlus gate scan: " + found.size() + " positive gates in "
                + (android.os.SystemClock.elapsedRealtime() - start) + " ms");
        return Collections.unmodifiableSet(found);
    }

    static boolean isPositiveBooleanGate(String key) {
        if (key == null) return false;
        String k = key.toLowerCase(Locale.ROOT);
        if (!k.startsWith("com.oplus.")) return false;

        String[] negative = {
                ".not.support", ".unsupported", ".disable", ".disabled",
                ".hide.", ".close.", ".old.", ".preversion", "fallback", "thermal"
        };
        for (String marker : negative) {
            if (k.contains(marker)) return false;
        }

        return k.contains(".support")
                || k.contains(".supported")
                || k.endsWith(".enable")
                || k.contains(".enable.")
                || k.endsWith(".default.open");
    }

    static boolean isSafeToSynthesizeAsByte(String key) {
        if (!isPositiveBooleanGate(key)) return false;
        String k = key.toLowerCase(Locale.ROOT);
        // Feature-table names are not VendorTag rows and must not be invented in APS JSON.
        return !k.startsWith("com.oplus.camera.feature.");
    }

    private static void addIfPositive(Set<String> out, String candidate) {
        if (isPositiveBooleanGate(candidate)) {
            out.add(candidate);
        }
    }

    private static boolean isGateChar(int b) {
        return (b >= 'a' && b <= 'z')
                || (b >= 'A' && b <= 'Z')
                || (b >= '0' && b <= '9')
                || b == '.' || b == '_' || b == '-' || b == '$';
    }
}
