# Android Folder Chunker

A no-terminal Android utility for splitting a large file into upload-friendly chunks.

## Behavior

1. First launch opens Android's system folder picker.
2. Grant the folder that contains the large file.
3. The app automatically selects the **largest ordinary file >= 32 MiB** in that folder.
4. It splits the file into **96 MiB** chunks.
5. Output is created in the same selected folder as:

```text
<original-name>.parts/
  manifest.json
  parts.sha256
  UPLOAD_ORDER.txt
  part-000000.bin
  part-000001.bin
  ...
```

The original file is never modified or deleted.

## Android storage model

An installed APK cannot safely infer the folder it was downloaded from on modern Android.
The first folder selection is therefore required by Android's Storage Access Framework.
The app persists that folder grant and automatically scans it on later launches.

No broad `MANAGE_EXTERNAL_STORAGE` permission and no Internet permission are requested.

## Output integrity

`manifest.json` contains:

- original file name/size/last-modified timestamp;
- complete original SHA-256;
- chunk size and count;
- every chunk's name, size and SHA-256.

This makes cross-chat or multi-batch uploading safe to verify and reassemble.
