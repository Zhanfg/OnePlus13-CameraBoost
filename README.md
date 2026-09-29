# OnePlus13-CameraBoost

Clean-room research toolkit for mapping, validating and selectively unlocking
OPlus camera capabilities on OnePlus 13-class devices.

## Current research tracks

### 1. Official OPPO + OnePlus camera capability universe

Before device testing, the project is building a source-grounded union of camera
capabilities from **official public OPPO/OnePlus artifacts only**:

- official product specifications and imaging/technical publications;
- `oppo-source` and `OnePlusOSS` source releases;
- official OTA/ROM camera files when publicly distributed and materialized by the owner.

The machine-readable catalog is `catalog/official-camera-features.json`; see
`docs/official-camera-feature-universe.md`.

Third-party modules and community ROM dumps are not accepted as evidence for this
catalog. Unexplained official acronyms are kept verbatim rather than guessed.

### 2. Real 10-bit still photography

The repository contains an installable LSPosed-compatible Android module with two CI
variants:

- **probe** — observes OPlus camera gates and reports Camera2 10-bit/P010/HEIC
  capabilities without modifying the camera configuration.
- **enable10bit** — on guarded OnePlus 13 targets, enables the native OPlus
  `com.oplus.10bits.heic.encode.support` feature gate while keeping 10-bit
  Live Photo disabled until normal 10-bit still capture is validated.

See `docs/10bit-still.md` for the validation procedure.

## Goals

1. Maintain an auditable official-source camera feature universe.
2. Collect reproducible device/camera capability snapshots.
3. Diff decrypted OPlus APS JSON dumps without redistributing proprietary camera blobs.
4. Build a feature matrix that separates UI gates, HAL/VendorTags, APS algorithms,
   sensor/ISP/encoder constraints, and actual media validation.
5. Keep kernel-side work separate from LSPosed/user-space hooks.

## Safety rule

A visible camera option is **not** proof that the underlying sensor/ISP/video pipeline
supports it.

Every feature moves through:

`discovered -> advertised -> callable -> streaming -> stable -> validated`

## First target

- OnePlus 13
- Model IDs currently guarded: `PJZ110`, `CPH2653`, `CPH2655`
- Platform: SM8750
- Codename: `dodge`

## Clean-room / license boundary

Original CameraBoost code is MIT licensed. Third-party projects referenced by research
notes retain their own licenses. No proprietary OPPO/OnePlus camera blobs are redistributed.
