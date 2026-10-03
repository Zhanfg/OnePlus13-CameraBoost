package dev.cameraboost.oplus10bit;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * One-pass Aho-Corasick matcher for stable ASCII feature anchors in classes*.dex.
 *
 * This intentionally avoids depending on obfuscated class names. The matcher scans each
 * dex stream once and answers all registered feature-anchor queries from the resulting set.
 */
final class DexAnchorIndex {
    private static final int ALPHABET = 128;

    private static final class Node {
        final int[] next = new int[ALPHABET];
        int fail;
        final ArrayList<Integer> out = new ArrayList<>();

        Node() {
            Arrays.fill(next, -1);
        }
    }

    private final ArrayList<String> patterns;
    private final ArrayList<Node> nodes = new ArrayList<>();
    private final Set<String> matches = new HashSet<>();

    DexAnchorIndex(Collection<String> anchors) {
        ArrayList<String> unique = new ArrayList<>(new HashSet<>(anchors));
        Collections.sort(unique);
        patterns = unique;
        nodes.add(new Node());
        buildTrie();
        buildFailures();
    }

    Set<String> scanApk(String apkPath) {
        matches.clear();
        long start = android.os.SystemClock.elapsedRealtime();
        try (ZipInputStream zis = new ZipInputStream(
                new BufferedInputStream(new FileInputStream(apkPath), 256 * 1024))) {
            ZipEntry entry;
            byte[] buffer = new byte[128 * 1024];
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();
                if (!isDex(name)) {
                    continue;
                }
                int state = 0;
                int read;
                while ((read = zis.read(buffer)) > 0) {
                    for (int i = 0; i < read; i++) {
                        int b = buffer[i] & 0xff;
                        if (b >= ALPHABET) {
                            state = 0;
                            continue;
                        }
                        while (state != 0 && nodes.get(state).next[b] == -1) {
                            state = nodes.get(state).fail;
                        }
                        int next = nodes.get(state).next[b];
                        state = next == -1 ? 0 : next;
                        Node node = nodes.get(state);
                        for (int id : node.out) {
                            matches.add(patterns.get(id));
                        }
                    }
                }
            }
        } catch (Throwable t) {
            CameraBoostLog.log("DEX anchor scan failed: " + t.getClass().getSimpleName()
                    + ": " + t.getMessage());
        }
        CameraBoostLog.log("DEX anchor scan: " + matches.size() + "/" + patterns.size()
                + " anchors in "
                + (android.os.SystemClock.elapsedRealtime() - start) + " ms");
        return Collections.unmodifiableSet(new HashSet<>(matches));
    }

    private boolean isDex(String name) {
        return name != null && name.startsWith("classes") && name.endsWith(".dex");
    }

    private void buildTrie() {
        for (int id = 0; id < patterns.size(); id++) {
            byte[] bytes = patterns.get(id).getBytes(StandardCharsets.US_ASCII);
            int state = 0;
            for (byte raw : bytes) {
                int b = raw & 0xff;
                if (b >= ALPHABET) {
                    throw new IllegalArgumentException("non-ASCII anchor: " + patterns.get(id));
                }
                int next = nodes.get(state).next[b];
                if (next == -1) {
                    next = nodes.size();
                    nodes.get(state).next[b] = next;
                    nodes.add(new Node());
                }
                state = next;
            }
            nodes.get(state).out.add(id);
        }
    }

    private void buildFailures() {
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        for (int b = 0; b < ALPHABET; b++) {
            int next = nodes.get(0).next[b];
            if (next != -1) {
                nodes.get(next).fail = 0;
                queue.add(next);
            }
        }

        while (!queue.isEmpty()) {
            int state = queue.removeFirst();
            for (int b = 0; b < ALPHABET; b++) {
                int next = nodes.get(state).next[b];
                if (next == -1) continue;
                int failure = nodes.get(state).fail;
                while (failure != 0 && nodes.get(failure).next[b] == -1) {
                    failure = nodes.get(failure).fail;
                }
                int fallback = nodes.get(failure).next[b];
                nodes.get(next).fail = fallback == -1 ? 0 : fallback;
                nodes.get(next).out.addAll(nodes.get(nodes.get(next).fail).out);
                queue.addLast(next);
            }
        }
    }
}
