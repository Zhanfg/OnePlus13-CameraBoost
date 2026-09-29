# 10-bit still photography workstream

## What "10-bit photo" means here

CameraBoost keeps four separate concepts apart:

1. **OPlus 10-bit HEIC/HEIF still photo** — the immediate target.
2. **Android public 10-bit P010 output** — an important HAL capability signal.
3. **Ultra HDR / JPEG_R / HEIC_ULTRAHDR** — gain-map HDR formats, not the same thing as a plain 10-bit HEIC.
4. **RAW10/RAW12/DNG** — sensor/raw-domain data, also not the same thing as processed 10-bit HEIC.

Android's public camera stack exposes the
`REQUEST_AVAILABLE_CAPABILITIES_DYNAMIC_RANGE_TEN_BIT` capability and P010
stream configurations for 10-bit output. OPlus Camera additionally has its own
product/config gate for 10-bit HEIC encoding.

## OPlus feature gates observed in public upstream research

The following keys are used as **compatibility probes**:

- `com.oplus.10bits.heic.encode.support`
- `com.oplus.camera.heif.support.livephoto`
- `com.oplus.livephoto.support.10bit`
- `com.oplus.feature.video.10bit.support` (observed only; not enabled by this workstream)

CameraBoost v0.1 only enables the first key in the experimental enable build.
10-bit Live Photo and 10-bit video remain disabled until normal 10-bit still
capture is proven stable and its output is verified.

## Dual compatibility path

OPlus Camera versions do not always resolve feature flags through the same code path.
CameraBoost therefore supports two independent compatibility paths:

1. observe/patch the resolved `oplus_camera_config` document;
2. observe/override the OPlus vendor-tag getter path when present:
   - `CameraAdapterUtils#getVendorTagConfig`
   - `ApsUtils#getVendorTagConfig`
   - legacy/current `CameraConfig#getConfigBooleanValue`

If any class is missing, the hook fails open and continues with the remaining paths.

## APK variants

The Android module produces two installable debug-signed artifacts:

### probe

- scopes only `com.oplus.camera`;
- observes OPlus camera config and direct vendor-tag getter paths;
- reports Camera2 10-bit/P010/HEIC/RAW/JPEG_R/HEIC_ULTRAHDR capabilities;
- does **not** modify the camera configuration.

### enable10bit

- same capability UI and logging;
- only mutates config on detected OnePlus 13 models/codename;
- sets `com.oplus.10bits.heic.encode.support` to enabled through whichever compatible OPlus path is present;
- leaves Live Photo and video 10-bit gates untouched;
- fails open: parse/hook errors leave unrelated camera behavior unchanged.

## Validation protocol

A successful menu toggle is insufficient.

For a candidate capture:

1. install the `probe` APK and enable its LSPosed scope for `com.oplus.camera`;
2. restart/force-stop the camera and open it once;
3. open the CameraBoost app and copy the Camera2 capability report;
4. check LSPosed logs for the original OPlus 10-bit HEIC gate value;
5. install/activate `enable10bit` only when ready for the experimental gate;
6. restart the camera process;
7. enable the OPlus 10-bit/HEIF option if it appears, then capture a normal still;
8. inspect the final file with:

   ```bash
   python tools/verify_10bit.py /path/to/photo.heic --json
   ```

9. require a 10-bit pixel format / >=10-bit sample report before calling the
   feature `validated`.

## Why P010 matters but is not sufficient

Android requires 10-bit dynamic-range capable devices to advertise supported
10-bit profiles, including HLG10, and the HAL must support dynamic-range profile
values for P010 or implementation-defined PRIVATE streams. This is strong
evidence that the sensor/ISP/HAL can move 10-bit samples through a public
pipeline. It does **not** guarantee that OPlus' proprietary HEIC still encoder
path is enabled for every lens/mode.

## Related enhancements queued after first successful capture

Once real 10-bit HEIC is verified on the target build:

- 10-bit Live Photo, gated separately;
- per-lens support matrix;
- Master/professional-mode 10-bit HEIC;
- metadata preservation and color-profile validation;
- DCI-P3 / BT.2020 transfer and primaries audit;
- 10-bit + ProXDR / Ultra HDR coexistence tests;
- Android 16 `HEIC_ULTRAHDR` comparison;
- RAW Max / Turbo RAW interoperability;
- AI HD / tele SDSR compatibility tests;
- HEIF gallery editing / blur compatibility;
- SDR export fallback without altering 10-bit originals.
