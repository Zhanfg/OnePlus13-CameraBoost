# Official OTA / ROM camera-stack ingestion

This tool is for **officially distributed** OnePlus/OPPO firmware only. It creates a
derived index; it does not add proprietary firmware contents to the repository.

## Why this exists

The remaining OnePlus 13 P0 10-bit still gap is primarily in userspace/vendor material:

```text
OPlus feature/build config
  -> Camera HAL / VendorTags
  -> P010 or PRIVATE 10-bit stream
  -> HEIF/HEIC encoder
  -> CICP / ICC / wide-gamut metadata
  -> OPlus Camera / APS feature exposure
  -> Gallery/Photos handling
```

The public SM8750 kernel cannot answer these questions by itself.

## Supported inputs

### Extracted firmware directory

```bash
python tools/index_official_ota.py extracted_ota/ -o ota-camera-index.json
```

### ZIP archive

```bash
python tools/index_official_ota.py official_rollback.zip -o ota-camera-index.json
```

A normal Android full OTA often contains `payload.bin` instead of directly visible
partitions. The indexer records this as a container artifact and marks
`needs_partition_materialization=true`; it does not pretend the payload has already been
searched.

## What gets classified

- **system_features** — OPlus native-feature/build properties and permissions;
- **camera_hal** — camera provider/HAL services and declarations;
- **camera_aps_config** — OPlus Camera, APS, CamX, CHI, Chromatix and sensor-mode config;
- **heif_codec** — HEIF/HEIC, MediaCodec/Codec2 and HEVC still-image surfaces;
- **color_metadata** — Rec.2020/BT.2020/P3, HDR, dataspace, CICP and ICC assets;
- **gallery_media** — Gallery/Photos/MediaProvider surfaces;
- **camera_ml_model** — camera-related model files are inventoried, not copied.

## Safe default behavior

For relevant files the index records:

- relative path;
- file size;
- classification layers;
- SHA-256 for files up to the configured hash limit;
- derived text signals for small text configuration files.

Large proprietary binaries are **not** read merely to hash them by default. Camera models,
calibration data, libraries and full configurations are never committed automatically.

## Chaining into the 10-bit path analyzer

After materializing the relevant text files, use:

```bash
python tools/extract_official_camera_features.py \
  extracted/vendor/etc/camera/config/... \
  extracted/vendor/etc/media_codecs*.xml \
  -o official-camera-scan.json

cameraboost color-path official-camera-scan.json
```

This separates two tasks:

1. **inventory** — find the official files that matter;
2. **evidence scan** — determine which required 10-bit/HEIF/color signals are actually
   present.

## Official distribution channels

OnePlus support documents both OTA updates and local installation of downloaded official
update ZIPs. OnePlus Community software-team release threads also publish exact OnePlus 13
build identifiers and, when rollback is offered, local-OTA rollback packages.

Do not substitute a third-party firmware mirror when first-party packages are available.
