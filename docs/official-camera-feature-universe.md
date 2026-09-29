# Official OPPO + OnePlus camera feature universe

This index deliberately starts from **official public artifacts only**.

## Evidence hierarchy

1. OPPO / OnePlus official product specifications and imaging technical publications.
2. Official source releases under `oppo-source` and `OnePlusOSS`.
3. Publicly distributed official OTA / ROM camera files after they are materialized by the owner.
4. Third-party reverse engineering is navigation-only and cannot establish a catalog entry.

The machine-readable union is in `catalog/official-camera-features.json`.

## Layers

The catalog does not treat every item as the same kind of feature.

- **User modes**: Photo, Portrait, Night, Master, XPAN, Underwater, Pro Video, etc.
- **Capture formats/color**: RAW, RAW Plus, HEIF, 10-bit HEIF, O-Log.
- **Video**: 8K, 4K120, Dolby Vision, HDR, stabilization.
- **Computational photography**: HyperTone, Lightning Snap, Instant Dual Exposure, AI Telescope Zoom, DetailMax, UltraShot HDR.
- **Optics/hardware**: OIS, periscope/triprism telephoto, ToF, EEPROM, autofocus.
- **Pipeline/compiler capabilities**: official `OPLUS_FEATURE_CAMERA_*`, tuning and event-report symbols.

## High-value official discoveries

### Full-path 10-bit still imaging

OPPO's official Full-path Colour Management material explicitly describes 10-bit color from
capture through storage and display, full DCI-P3 coverage, and 10-bit HEIF image storage.
This is the correct official ancestor for the later OPlus 10-bit HEIC/HEIF path.

### O-Log

The official OPPO O-Log white paper states that the mobile imaging pipeline compresses
14-bit original linear data into 10-bit Log data. It specifies H.265 YUV 4:2:0,
BT.2020 / OPPO Wide Gamut, and post-production-oriented scene-referred encoding.
This is kept separate from 10-bit HEIF still photography.

### Computational capture

Official OPPO publications expose several algorithm/pipeline names that are not merely UI
modes: HyperTone Image Engine, Extra HD / AI RAW Fusion, Lightning Snap, Instant Dual
Exposure, Off-Peak Computing Architecture, near-zero shutter lag, AI Telescope Zoom,
and gapless zoom.

### Kernel/source capabilities

Official OPlus source releases expose a separate historical compile-time camera family,
including:

`OPLUS_FEATURE_CAMERA_IZOOM`
`OPLUS_FEATURE_CAMERA_AIS`
`OPLUS_FEATURE_CAMERA_FB`
`OPLUS_FEATURE_CAMERA_AISCP`
`OPLUS_FEATURE_CAMERA_UPSCALE`
`OPLUS_FEATURE_CAMERA_SUPERNIGHT`
`OPLUS_FEATURE_CAMERA_VIDEO`
`OPLUS_FEATURE_CAMERA_SMVR`
`OPLUS_FEATURE_CAMERA_MSNR`
`OPLUS_FEATURE_CAMERA_SAU`
`OPLUS_FEATURE_CAMERA_OIS`
`OPLUS_FEATURE_VIRTUAL_CAMERA`

Where an acronym has no public official semantic definition, the catalog preserves it
verbatim as `semantics_unresolved`. We do not manufacture expansions.

## Why this is not yet a claim of mathematical exhaustiveness

OPPO/OnePlus do not publish the complete proprietary Camera APK/APS source tree. Public
kernel source proves many hardware/compiler capabilities, while product pages prove many
user-facing modes. Some official OTA camera configs are encrypted or binary.

Accordingly, the repo uses an **incremental official-corpus ingestion model**: every new
official source release, specification, white paper, or user-supplied official OTA file can
be scanned with `tools/extract_official_camera_features.py` and merged into the union.
An absent symbol is never treated as evidence that the product lacks the feature.

## Extracting a newly obtained official artifact

```bash
python tools/extract_official_camera_features.py \
  path/to/oplus_native_features.mk \
  path/to/decrypted-official-camera-config.json \
  -o official-camera-scan.json
```

The extractor currently identifies:

- `OPLUS_FEATURE_CAMERA*`, `OPLUS_ARCH_EXTENDS_CAM*`, `CONFIG_OPLUS_CAM*`
- OPlus `com.oplus.*` VendorTags
- `APS_ALGO_*` identifiers
- explicit camera-mode lists in plaintext official documents

It contains no proprietary decryption key and does not redistribute vendor blobs.


## Evidence audit

The catalog is self-auditing. Its `audit.entries_without_sources` list must remain empty.
This prevents a remembered or community-only capability from silently becoming an
"official" catalog fact.

A second distinction is also enforced:

- **OPPO O-Log** is the named OPPO log encoding defined by the official OPPO white paper.
- **OnePlus LOG Video** is recorded separately from OnePlus' official product release.
  The catalog does not equate the two names without an official statement doing so.

Additional official OnePlus releases establish Dual Exposure Algorithm, Clear Burst,
Action Mode, AI Telephoto, Ultra Clear 26MP, Clear Night Engine, LOG recording and
real-time LUT previews. Official OnePlus 9 Pro specifications also establish 12-bit RAW,
Dual ISO, DOL-HDR, Video Portrait, Focus Tracking, monochrome camera and related focus
features.
