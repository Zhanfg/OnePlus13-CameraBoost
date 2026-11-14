package dev.cameraboost.folderchunker;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public final class MainActivity extends Activity {
    private static final int REQ_TREE = 2001;
    private static final String PREFS = "folder_chunker";
    private static final String PREF_TREE_URI = "tree_uri";

    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final AtomicBoolean busy = new AtomicBoolean(false);
    private final AtomicBoolean cancelRequested = new AtomicBoolean(false);

    private TextView status;
    private TextView details;
    private ProgressBar progress;
    private Button chooseButton;
    private Button rescanButton;
    private Button cancelButton;
    private Button copyButton;

    private volatile String lastReport = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildUi();

        chooseButton.setOnClickListener(v -> chooseFolder());
        rescanButton.setOnClickListener(v -> {
            Uri uri = savedTreeUri();
            if (uri == null) {
                chooseFolder();
            } else {
                startAutoSplit(uri, true);
            }
        });
        cancelButton.setOnClickListener(v -> {
            if (busy.get()) {
                cancelRequested.set(true);
                setStatus("正在请求停止……当前分片写完后会停下。");
            }
        });
        copyButton.setOnClickListener(v -> copyReport());

        Uri saved = savedTreeUri();
        if (saved == null) {
            setStatus("首次使用：请选择包含大文件的文件夹。");
            chooseFolder();
        } else {
            setStatus("已记住文件夹，正在自动扫描……");
            startAutoSplit(saved, false);
        }
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(18), dp(18), dp(24));

        TextView title = new TextView(this);
        title.setText("Folder Chunker");
        title.setTextSize(24f);
        root.addView(title, matchWrap());

        TextView subtitle = new TextView(this);
        subtitle.setText(
                "选择一次文件夹后自动识别其中最大的文件，并在同目录生成 .parts 分片目录。\n"
                        + "默认 96 MiB/片；原文件不会被修改或删除。"
        );
        subtitle.setTextSize(15f);
        subtitle.setPadding(0, dp(8), 0, dp(12));
        root.addView(subtitle, matchWrap());

        status = new TextView(this);
        status.setTextSize(17f);
        status.setPadding(0, dp(8), 0, dp(8));
        root.addView(status, matchWrap());

        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(1000);
        progress.setProgress(0);
        root.addView(progress, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(20)
        ));

        details = new TextView(this);
        details.setTextIsSelectable(true);
        details.setTypeface(android.graphics.Typeface.MONOSPACE);
        details.setTextSize(13f);
        details.setPadding(0, dp(12), 0, dp(12));
        root.addView(details, matchWrap());

        chooseButton = button("选择 / 更换文件夹");
        rescanButton = button("重新扫描并分片");
        cancelButton = button("停止");
        copyButton = button("复制报告");

        root.addView(chooseButton, matchWrap());
        root.addView(rescanButton, matchWrap());
        root.addView(cancelButton, matchWrap());
        root.addView(copyButton, matchWrap());

        ScrollView scroll = new ScrollView(this);
        scroll.addView(root);
        setContentView(scroll);
    }

    private Button button(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        return b;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
    }

    private void chooseFolder() {
        if (busy.get()) {
            toast("正在分片，请先停止。");
            return;
        }
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION
                        | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                        | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
        );
        startActivityForResult(intent, REQ_TREE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQ_TREE || resultCode != RESULT_OK || data == null) {
            return;
        }

        Uri tree = data.getData();
        if (tree == null) return;

        int takeFlags = data.getFlags()
                & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        try {
            getContentResolver().takePersistableUriPermission(tree, takeFlags);
        } catch (SecurityException ignored) {
            // Some providers grant usable transient permission but reject persistable flags.
        }

        getSharedPreferences(PREFS, MODE_PRIVATE)
                .edit()
                .putString(PREF_TREE_URI, tree.toString())
                .apply();

        startAutoSplit(tree, false);
    }

    private Uri savedTreeUri() {
        String value = getSharedPreferences(PREFS, MODE_PRIVATE)
                .getString(PREF_TREE_URI, null);
        if (value == null || value.isEmpty()) return null;
        return Uri.parse(value);
    }

    private void startAutoSplit(Uri treeUri, boolean forceNewRun) {
        if (!busy.compareAndSet(false, true)) {
            toast("已经在处理。");
            return;
        }

        cancelRequested.set(false);
        setUiBusy(true);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        worker.execute(() -> {
            try {
                runAutoSplit(treeUri, forceNewRun);
            } catch (Throwable t) {
                publishFailure(t);
            } finally {
                busy.set(false);
                cancelRequested.set(false);
                runOnUiThread(() -> {
                    setUiBusy(false);
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                });
            }
        });
    }

    private void runAutoSplit(Uri treeUri, boolean forceNewRun) throws Exception {
        ContentResolver resolver = getContentResolver();
        Uri rootUri = rootDocumentUri(treeUri);

        List<DocEntry> children = listChildren(treeUri, rootUri);
        DocEntry target = chooseLargestFile(children, resolver);

        if (target == null) {
            throw new IllegalStateException(
                    "这个文件夹里没有找到可分片的大文件（至少 32 MiB）。"
            );
        }

        publishStatus(
                "已识别文件，准备分片",
                "文件: " + target.name + "\n"
                        + "大小: " + ChunkRules.humanBytes(target.size) + "\n"
                        + "默认分片: " + ChunkRules.humanBytes(ChunkRules.DEFAULT_CHUNK_BYTES)
        );

        String basePartsName = target.name + ".parts";
        ExistingParts existing = findCompletedParts(treeUri, rootUri, basePartsName, target);
        if (existing != null && !forceNewRun) {
            String report = "检测到已经完成的分片目录：\n"
                    + existing.name + "\n\n"
                    + "原文件: " + target.name + "\n"
                    + "大小: " + target.size + " bytes\n"
                    + "无需重复分片。";
            publishComplete("已经分片完成", report);
            return;
        }

        String outputDirName = uniquePartsDirectoryName(
                treeUri, rootUri, basePartsName, forceNewRun || existing != null
        );
        Uri outputDir = DocumentsContract.createDocument(
                resolver,
                rootUri,
                DocumentsContract.Document.MIME_TYPE_DIR,
                outputDirName
        );
        if (outputDir == null) {
            throw new IllegalStateException("无法在目标文件夹创建 " + outputDirName);
        }

        SplitResult result = splitToDirectory(treeUri, target, outputDir, outputDirName);

        if (cancelRequested.get()) {
            publishStatus(
                    "已停止",
                    "已写出的分片保留在 " + outputDirName + " 中。\n"
                            + "因为没有完整 manifest，请不要上传这一批；可重新运行生成新目录。"
            );
            return;
        }

        writeMetadata(outputDir, target, result);

        String report = buildReport(target, outputDirName, result);
        publishComplete("分片完成", report);
    }

    private SplitResult splitToDirectory(
            Uri treeUri,
            DocEntry target,
            Uri outputDir,
            String outputDirName
    ) throws Exception {
        ContentResolver resolver = getContentResolver();
        MessageDigest fullDigest = MessageDigest.getInstance("SHA-256");
        List<PartInfo> parts = new ArrayList<>();

        long totalRead = 0L;
        int partIndex = 0;

        try (InputStream raw = resolver.openInputStream(target.uri);
             BufferedInputStream input = new BufferedInputStream(raw, 1024 * 1024)) {

            if (raw == null) throw new IllegalStateException("无法读取原文件。");

            byte[] buffer = new byte[1024 * 1024];

            while (totalRead < target.size) {
                if (cancelRequested.get()) break;

                String partName = ChunkRules.partName(partIndex);
                Uri partUri = DocumentsContract.createDocument(
                        resolver,
                        outputDir,
                        "application/octet-stream",
                        partName
                );
                if (partUri == null) {
                    throw new IllegalStateException("无法创建分片 " + partName);
                }

                MessageDigest partDigest = MessageDigest.getInstance("SHA-256");
                long partBytes = 0L;

                try (OutputStream rawOut = resolver.openOutputStream(partUri, "w");
                     BufferedOutputStream output = new BufferedOutputStream(rawOut, 1024 * 1024)) {
                    if (rawOut == null) throw new IllegalStateException("无法写入 " + partName);

                    while (partBytes < ChunkRules.DEFAULT_CHUNK_BYTES
                            && totalRead < target.size
                            && !cancelRequested.get()) {
                        int wanted = (int) Math.min(
                                buffer.length,
                                Math.min(
                                        ChunkRules.DEFAULT_CHUNK_BYTES - partBytes,
                                        target.size - totalRead
                                )
                        );
                        int n = input.read(buffer, 0, wanted);
                        if (n < 0) break;

                        output.write(buffer, 0, n);
                        partDigest.update(buffer, 0, n);
                        fullDigest.update(buffer, 0, n);
                        partBytes += n;
                        totalRead += n;

                        publishProgress(target, totalRead, partIndex);
                    }
                    output.flush();
                }

                if (partBytes > 0) {
                    parts.add(new PartInfo(
                            partIndex,
                            partName,
                            partBytes,
                            hex(partDigest.digest())
                    ));
                    partIndex++;
                }

                if (partBytes == 0) break;
            }
        }

        if (!cancelRequested.get() && totalRead != target.size) {
            throw new IllegalStateException(
                    "读取长度不一致：预期 " + target.size + "，实际 " + totalRead
            );
        }

        return new SplitResult(
                totalRead,
                cancelRequested.get() ? null : hex(fullDigest.digest()),
                parts
        );
    }

    private void writeMetadata(Uri outputDir, DocEntry target, SplitResult result) throws Exception {
        JSONObject manifest = new JSONObject();
        manifest.put("format", "folder-chunker-v1");
        manifest.put("status", "complete");
        manifest.put("original_name", target.name);
        manifest.put("original_size", target.size);
        manifest.put("original_last_modified", target.lastModified);
        manifest.put("original_sha256", result.fullSha256);
        manifest.put("chunk_size_bytes", ChunkRules.DEFAULT_CHUNK_BYTES);
        manifest.put("chunk_count", result.parts.size());
        manifest.put("created_at_utc", utcNow());

        JSONArray array = new JSONArray();
        for (PartInfo part : result.parts) {
            JSONObject p = new JSONObject();
            p.put("index", part.index);
            p.put("name", part.name);
            p.put("size", part.size);
            p.put("sha256", part.sha256);
            array.put(p);
        }
        manifest.put("parts", array);

        StringBuilder hashes = new StringBuilder();
        for (PartInfo part : result.parts) {
            hashes.append(part.sha256)
                    .append("  ")
                    .append(part.size)
                    .append("  ")
                    .append(part.name)
                    .append('\n');
        }

        String order = "请先上传：\n"
                + "1. manifest.json\n"
                + "2. parts.sha256\n"
                + "然后按顺序上传：\n"
                + "3. part-000000.bin\n"
                + "4. part-000001.bin\n"
                + "5. ...直到最后一片\n\n"
                + "Original SHA-256:\n"
                + result.fullSha256
                + "\n";

        writeTextFile(outputDir, "manifest.json", manifest.toString(2));
        writeTextFile(outputDir, "parts.sha256", hashes.toString());
        writeTextFile(outputDir, "UPLOAD_ORDER.txt", order);
    }

    private void writeTextFile(Uri parent, String name, String text) throws Exception {
        Uri uri = DocumentsContract.createDocument(
                getContentResolver(),
                parent,
                "text/plain",
                name
        );
        if (uri == null) throw new IllegalStateException("无法创建 " + name);

        try (OutputStream output = getContentResolver().openOutputStream(uri, "w")) {
            if (output == null) throw new IllegalStateException("无法写入 " + name);
            output.write(text.getBytes(StandardCharsets.UTF_8));
        }
    }

    private ExistingParts findCompletedParts(
            Uri treeUri,
            Uri rootUri,
            String dirName,
            DocEntry target
    ) {
        try {
            DocEntry dir = findChild(treeUri, rootUri, dirName, true);
            if (dir == null) return null;

            DocEntry manifest = findChild(treeUri, dir.uri, "manifest.json", false);
            if (manifest == null) return null;

            String text = readSmallText(manifest.uri, 1024 * 1024);
            JSONObject json = new JSONObject(text);

            if (!"complete".equals(json.optString("status"))) return null;
            if (!target.name.equals(json.optString("original_name"))) return null;
            if (target.size != json.optLong("original_size", -1L)) return null;

            long expectedModified = json.optLong("original_last_modified", -1L);
            if (expectedModified > 0L && target.lastModified > 0L
                    && expectedModified != target.lastModified) {
                return null;
            }
            return new ExistingParts(dir.name, dir.uri);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private String uniquePartsDirectoryName(
            Uri treeUri,
            Uri rootUri,
            String base,
            boolean forceSuffix
    ) throws Exception {
        if (!forceSuffix && findChild(treeUri, rootUri, base, true) == null) {
            return base;
        }

        if (!forceSuffix && findChild(treeUri, rootUri, base, true) != null) {
            forceSuffix = true;
        }

        for (int i = 2; i < 10_000; i++) {
            String candidate = base + "-" + i;
            if (findChild(treeUri, rootUri, candidate, true) == null) {
                return candidate;
            }
        }
        throw new IllegalStateException("无法生成新的分片目录名称。");
    }

    private DocEntry chooseLargestFile(List<DocEntry> entries, ContentResolver resolver) {
        DocEntry best = null;
        for (DocEntry entry : entries) {
            if (entry.directory || ChunkRules.ignoredFileName(entry.name)) continue;

            long size = entry.size;
            if (size < 0L) size = contentLength(resolver, entry.uri);
            if (size < ChunkRules.MIN_AUTO_FILE_BYTES) continue;

            DocEntry normalized = entry.withSize(size);
            if (best == null || normalized.size > best.size) {
                best = normalized;
            }
        }
        return best;
    }

    private long contentLength(ContentResolver resolver, Uri uri) {
        try (android.content.res.AssetFileDescriptor afd =
                     resolver.openAssetFileDescriptor(uri, "r")) {
            if (afd == null) return -1L;
            return afd.getLength();
        } catch (Throwable ignored) {
            return -1L;
        }
    }

    private List<DocEntry> listChildren(Uri treeUri, Uri parentUri) throws Exception {
        String parentId = DocumentsContract.getDocumentId(parentUri);
        Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentId);

        String[] projection = {
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_SIZE,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_LAST_MODIFIED,
        };

        List<DocEntry> result = new ArrayList<>();
        try (Cursor cursor = getContentResolver().query(
                childrenUri, projection, null, null, null
        )) {
            if (cursor == null) return result;

            int idCol = cursor.getColumnIndexOrThrow(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID
            );
            int nameCol = cursor.getColumnIndexOrThrow(
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME
            );
            int sizeCol = cursor.getColumnIndexOrThrow(
                    DocumentsContract.Document.COLUMN_SIZE
            );
            int mimeCol = cursor.getColumnIndexOrThrow(
                    DocumentsContract.Document.COLUMN_MIME_TYPE
            );
            int modifiedCol = cursor.getColumnIndexOrThrow(
                    DocumentsContract.Document.COLUMN_LAST_MODIFIED
            );

            while (cursor.moveToNext()) {
                String id = cursor.getString(idCol);
                String name = cursor.getString(nameCol);
                long size = cursor.isNull(sizeCol) ? -1L : cursor.getLong(sizeCol);
                String mime = cursor.getString(mimeCol);
                long modified = cursor.isNull(modifiedCol) ? -1L : cursor.getLong(modifiedCol);
                Uri uri = DocumentsContract.buildDocumentUriUsingTree(treeUri, id);
                boolean directory = DocumentsContract.Document.MIME_TYPE_DIR.equals(mime);

                result.add(new DocEntry(
                        id, name, size, mime, modified, directory, uri
                ));
            }
        }
        return result;
    }

    private DocEntry findChild(
            Uri treeUri,
            Uri parentUri,
            String wantedName,
            boolean wantDirectory
    ) throws Exception {
        for (DocEntry entry : listChildren(treeUri, parentUri)) {
            if (wantedName.equals(entry.name) && entry.directory == wantDirectory) {
                return entry;
            }
        }
        return null;
    }

    private String readSmallText(Uri uri, int maxBytes) throws Exception {
        try (InputStream input = getContentResolver().openInputStream(uri);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            if (input == null) throw new IllegalStateException("无法读取文件。");

            byte[] buffer = new byte[8192];
            int total = 0;
            while (true) {
                int n = input.read(buffer);
                if (n < 0) break;
                total += n;
                if (total > maxBytes) {
                    throw new IllegalStateException("文本文件超过读取限制。");
                }
                output.write(buffer, 0, n);
            }
            return output.toString(StandardCharsets.UTF_8.name());
        }
    }

    private Uri rootDocumentUri(Uri treeUri) {
        return DocumentsContract.buildDocumentUriUsingTree(
                treeUri,
                DocumentsContract.getTreeDocumentId(treeUri)
        );
    }

    private void publishProgress(DocEntry target, long totalRead, int partIndex) {
        int value = target.size <= 0L
                ? 0
                : (int) Math.min(1000L, totalRead * 1000L / target.size);

        String detail = "文件: " + target.name + "\n"
                + "已处理: " + ChunkRules.humanBytes(totalRead)
                + " / " + ChunkRules.humanBytes(target.size) + "\n"
                + "当前: " + ChunkRules.partName(partIndex);

        runOnUiThread(() -> {
            progress.setProgress(value);
            status.setText("正在分片……");
            details.setText(detail);
        });
    }

    private void publishStatus(String headline, String detail) {
        runOnUiThread(() -> {
            status.setText(headline);
            details.setText(detail);
        });
    }

    private void publishFailure(Throwable t) {
        String report = "失败："
                + t.getClass().getSimpleName()
                + "\n"
                + (t.getMessage() == null ? "" : t.getMessage());

        lastReport = report;
        runOnUiThread(() -> {
            progress.setProgress(0);
            status.setText("处理失败");
            details.setText(report);
        });
    }

    private void publishComplete(String headline, String report) {
        lastReport = report;
        runOnUiThread(() -> {
            progress.setProgress(1000);
            status.setText(headline);
            details.setText(report);
        });
    }

    private String buildReport(DocEntry target, String outputDirName, SplitResult result) {
        return "原文件: " + target.name + "\n"
                + "原大小: " + target.size + " bytes\n"
                + "原 SHA-256: " + result.fullSha256 + "\n"
                + "分片大小: " + ChunkRules.DEFAULT_CHUNK_BYTES + " bytes\n"
                + "分片数量: " + result.parts.size() + "\n"
                + "输出目录: " + outputDirName + "\n\n"
                + "上传时先发 manifest.json 和 parts.sha256，"
                + "再按 part-000000.bin、part-000001.bin……顺序上传。";
    }

    private void setStatus(String text) {
        status.setText(text);
    }

    private void setUiBusy(boolean isBusy) {
        chooseButton.setEnabled(!isBusy);
        rescanButton.setEnabled(!isBusy);
        cancelButton.setEnabled(isBusy);
    }

    private void copyReport() {
        String text = lastReport;
        if (text == null || text.isEmpty()) text = details.getText().toString();

        ClipboardManager clipboard =
                (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText("Folder Chunker report", text));
        toast("已复制");
    }

    private void toast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static String hex(byte[] bytes) {
        StringBuilder out = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            out.append(String.format(Locale.ROOT, "%02x", b & 0xff));
        }
        return out.toString();
    }

    private static String utcNow() {
        SimpleDateFormat fmt = new SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ROOT
        );
        fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
        return fmt.format(new Date());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isFinishing()) {
            cancelRequested.set(true);
            worker.shutdownNow();
        }
    }

    private static final class DocEntry {
        final String id;
        final String name;
        final long size;
        final String mime;
        final long lastModified;
        final boolean directory;
        final Uri uri;

        DocEntry(
                String id,
                String name,
                long size,
                String mime,
                long lastModified,
                boolean directory,
                Uri uri
        ) {
            this.id = id;
            this.name = name;
            this.size = size;
            this.mime = mime;
            this.lastModified = lastModified;
            this.directory = directory;
            this.uri = uri;
        }

        DocEntry withSize(long newSize) {
            return new DocEntry(
                    id, name, newSize, mime, lastModified, directory, uri
            );
        }
    }

    private static final class ExistingParts {
        final String name;
        final Uri uri;

        ExistingParts(String name, Uri uri) {
            this.name = name;
            this.uri = uri;
        }
    }

    private static final class PartInfo {
        final int index;
        final String name;
        final long size;
        final String sha256;

        PartInfo(int index, String name, long size, String sha256) {
            this.index = index;
            this.name = name;
            this.size = size;
            this.sha256 = sha256;
        }
    }

    private static final class SplitResult {
        final long bytesRead;
        final String fullSha256;
        final List<PartInfo> parts;

        SplitResult(long bytesRead, String fullSha256, List<PartInfo> parts) {
            this.bytesRead = bytesRead;
            this.fullSha256 = fullSha256;
            this.parts = parts;
        }
    }
}
