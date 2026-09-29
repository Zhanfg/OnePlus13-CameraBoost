# OnePlus13-CameraBoost

Clean-room research toolkit for mapping and comparing OPlus camera capabilities on OnePlus 13-class devices.

## Goals

1. Collect a reproducible device/camera capability snapshot.
2. Diff decrypted OPlus APS JSON dumps without redistributing proprietary camera blobs.
3. Build a feature matrix that separates:
   - UI / app-level feature gates
   - Camera HAL / VendorTag capability
   - APS algorithm availability
   - sensor / ISP / encoder / thermal constraints
4. Keep kernel-side work separate from LSPosed/user-space hooks.

## Safety rule

A visible camera option is **not** proof that the underlying sensor/ISP/video pipeline supports it.

Every feature moves through:

`discovered -> advertised -> callable -> streaming -> stable -> validated`

## First target

- OnePlus 13
- Model IDs: `PJZ110`, `CPH2653`
- Platform: SM8750

## License

Original clean-room code is MIT licensed. Third-party references retain their own licenses.
