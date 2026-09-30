package dev.cameraboost.folderchunker;

import java.util.Locale;

final class ChunkRules {
    static final long DEFAULT_CHUNK_BYTES = 96L * 1024L * 1024L;
    static final long MIN_AUTO_FILE_BYTES = 32L * 1024L * 1024L;

    private ChunkRules() {}

    static String partName(int index) {
        return String.format(Locale.ROOT, "part-%06d.bin", index);
    }

    static long expectedPartCount(long size, long chunkSize) {
        if (size <= 0 || chunkSize <= 0) return 0;
        return (size + chunkSize - 1L) / chunkSize;
    }

    static boolean ignoredFileName(String name) {
        if (name == null) return true;
        String lower = name.toLowerCase(Locale.ROOT);
        return lower.startsWith("part-")
                || lower.equals("manifest.json")
                || lower.equals("parts.sha256")
                || lower.equals("upload_order.txt")
                || lower.endsWith(".tmp");
    }

    static String humanBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        double kib = bytes / 1024.0;
        if (kib < 1024) return String.format(Locale.ROOT, "%.1f KiB", kib);
        double mib = kib / 1024.0;
        if (mib < 1024) return String.format(Locale.ROOT, "%.1f MiB", mib);
        return String.format(Locale.ROOT, "%.2f GiB", mib / 1024.0);
    }
}
