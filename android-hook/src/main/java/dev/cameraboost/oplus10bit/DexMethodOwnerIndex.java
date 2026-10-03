package dev.cameraboost.oplus10bit;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Tiny DEX method-owner index used to survive obfuscated camera mode class names.
 *
 * We only parse the string/type/method_id tables and never execute code. This is enough
 * to discover every class that declares a stable method name such as getSupportFunction.
 */
final class DexMethodOwnerIndex {
    private DexMethodOwnerIndex() {}

    static Set<String> findOwners(String apkPath, String methodName) {
        if (apkPath == null || apkPath.isEmpty() || methodName == null || methodName.isEmpty()) {
            return Collections.emptySet();
        }

        LinkedHashSet<String> owners = new LinkedHashSet<>();
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(apkPath))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();
                if (name == null || !name.startsWith("classes") || !name.endsWith(".dex")) {
                    continue;
                }
                ByteArrayOutputStream out = new ByteArrayOutputStream(
                        entry.getSize() > 0 && entry.getSize() < Integer.MAX_VALUE
                                ? (int) entry.getSize() : 256 * 1024
                );
                byte[] buffer = new byte[64 * 1024];
                int read;
                while ((read = zis.read(buffer)) > 0) {
                    out.write(buffer, 0, read);
                }
                parseDex(out.toByteArray(), methodName, owners);
            }
        } catch (Throwable t) {
            CameraBoostLog.log("DEX method-owner scan failed: "
                    + t.getClass().getSimpleName() + ": " + t.getMessage());
        }

        CameraBoostLog.log("DEX method-owner scan " + methodName + ": " + owners.size());
        return Collections.unmodifiableSet(owners);
    }

    private static void parseDex(byte[] dex, String methodName, Set<String> owners) {
        if (dex.length < 112 || dex[0] != 'd' || dex[1] != 'e' || dex[2] != 'x') {
            return;
        }

        int stringIdsSize = i32(dex, 56);
        int stringIdsOff = i32(dex, 60);
        int typeIdsSize = i32(dex, 64);
        int typeIdsOff = i32(dex, 68);
        int methodIdsSize = i32(dex, 88);
        int methodIdsOff = i32(dex, 92);

        if (!range(dex, stringIdsOff, stringIdsSize * 4L)
                || !range(dex, typeIdsOff, typeIdsSize * 4L)
                || !range(dex, methodIdsOff, methodIdsSize * 8L)) {
            return;
        }

        String[] strings = new String[stringIdsSize];
        for (int i = 0; i < stringIdsSize; i++) {
            int dataOff = i32(dex, stringIdsOff + i * 4);
            strings[i] = readDexString(dex, dataOff);
        }

        int[] typeStringIndexes = new int[typeIdsSize];
        for (int i = 0; i < typeIdsSize; i++) {
            typeStringIndexes[i] = i32(dex, typeIdsOff + i * 4);
        }

        for (int i = 0; i < methodIdsSize; i++) {
            int off = methodIdsOff + i * 8;
            int classIdx = u16(dex, off);
            int nameIdx = i32(dex, off + 4);
            if (classIdx < 0 || classIdx >= typeIdsSize
                    || nameIdx < 0 || nameIdx >= strings.length) {
                continue;
            }
            if (!methodName.equals(strings[nameIdx])) {
                continue;
            }

            int typeStringIdx = typeStringIndexes[classIdx];
            if (typeStringIdx < 0 || typeStringIdx >= strings.length) {
                continue;
            }
            String javaName = descriptorToJava(strings[typeStringIdx]);
            if (javaName != null) {
                owners.add(javaName);
            }
        }
    }

    private static String readDexString(byte[] dex, int off) {
        if (off < 0 || off >= dex.length) {
            return "";
        }
        int p = off;
        // Skip utf16_size ULEB128.
        for (int i = 0; i < 5 && p < dex.length; i++) {
            int b = dex[p++] & 0xff;
            if ((b & 0x80) == 0) break;
        }
        int end = p;
        while (end < dex.length && dex[end] != 0) {
            end++;
        }
        return new String(dex, p, Math.max(0, end - p), StandardCharsets.UTF_8);
    }

    private static String descriptorToJava(String descriptor) {
        if (descriptor == null || descriptor.length() < 3
                || descriptor.charAt(0) != 'L'
                || descriptor.charAt(descriptor.length() - 1) != ';') {
            return null;
        }
        return descriptor.substring(1, descriptor.length() - 1).replace('/', '.');
    }

    private static int u16(byte[] data, int off) {
        return (data[off] & 0xff) | ((data[off + 1] & 0xff) << 8);
    }

    private static int i32(byte[] data, int off) {
        return (data[off] & 0xff)
                | ((data[off + 1] & 0xff) << 8)
                | ((data[off + 2] & 0xff) << 16)
                | ((data[off + 3] & 0xff) << 24);
    }

    private static boolean range(byte[] data, int off, long length) {
        return off >= 0 && length >= 0 && ((long) off + length) <= data.length;
    }
}
