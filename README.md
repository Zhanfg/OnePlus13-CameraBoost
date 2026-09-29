# OnePlus13-CameraBoost

Clean-room research toolkit for mapping, validating and selectively unlocking
OPlus camera capabilities on OnePlus 13-class devices.

## Current focus: real 10-bit still photography

The repository now contains an installable LSPosed-compatible Android module
with two CI variants:

- **probe** — observes OPlus camera gates and reports Camera2 10-bit/P010/HEIC
  capabilities without modifying the camera configuration.
- **enable10bit** — on guarded OnePlus 13 targets, enables the native OPlus
  `com.oplus.10bits.heic.encode.support` feature gate while keeping 10-bit
  Live Photo disabled until normal 10-bit still capture is validated.

See `docs/10bit-still.md` for the validation procedure.

## Goals

1. Collect a reproducible device/camera capability snapshot.
2. Diff decrypted OPlus APS JSON dumps without redistributing proprietary camera blobs.
3. Build a feature matrix that separates:
   - UI / app-level feature gates
   - Camera HAL / VendorTag capability
   - APS algorithm availability
   - sensor / ISP / encoder / thermal constraints
4. Keep kernel-side work separate from LSPosed/user-space hooks.
5. Verify final media files before calling an unlocked feature real.

## Safety rule

A visible camera option is **not** proof that the underlying sensor/ISP/video
pipeline supports it.

Every feature moves through:

`discovered -> advertised -> callable -> streaming -> stable -> validated`

## First target

- OnePlus 13
- Model IDs currently guarded: `PJZ110`, `CPH2653`, `CPH2655`
- Platform: SM8750
- Codename: `dodge`

## Clean-room / license boundary

Original CameraBoost code is MIT licensed. Third-party projects referenced by
the research notes retain their own licenses. No proprietary OPPO/OnePlus
camera blobs are redistributed.
