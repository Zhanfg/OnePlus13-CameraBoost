# 10-bit still photography workstream

## What "10-bit photo" means here

CameraBoost keeps four separate concepts apart:

1. **OPlus 10-bit HEIC/HEIF still photo** — the immediate target.
2. **Android public 10-bit P010 output** — an important HAL capability signal.
3. **Ultra HDR / JPEG_R** — HDR gain-map JPEG, not the same thing as a 10-bit HEIC.
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

CameraBoost v0.1 only enables the first key in the experimental enable build.
10-bit Live Photo remains disabled until normal 10-bit still capture is proven
stable and its output is verified.

## APK variants

The Android module produces two installable debug-signed artifacts:

### probe

- scopes only `com.oplus.camera`;
- hooks the OPlus camera config path;
- records the current values of relevant feature gates in LSPosed logs;
- does **not** modify the config;
- its launcher activity reports Camera2 10-bit/P010/HEIC/RAW/JPEG_R capabilities.

### enable10bit

- same capability UI and logging;
- only mutates config on detected OnePlus 13 models/codename;
- sets `com.oplus.10bits.heic.encode.support` to Byte/1/1;
- leaves Live Photo 10-bit gates untouched;
- fails open: parse/hook errors leave the original camera config unchanged.

## Validation protocol

A successful menu toggle is insufficient.

For a candidate capture:

1. install the `probe` APK and collect the capability report;
2. check LSPosed logs for the original OPlus 10-bit HEIC gate value;
3. install/activate `enable10bit` only if you want the experimental gate;
4. restart the camera process;
5. capture a normal still photo with HEIF/high-efficiency mode active;
6. inspect the final file with:

   ```bash
   python tools/verify_10bit.py /path/to/photo.heic --json
   ```

7. require a 10-bit pixel format / >=10-bit sample report before calling the
   feature `validated`.

## Why P010 matters but is not sufficient

Android requires 10-bit dynamic-range capable devices to advertise supported
10-bit profiles, and 10-bit surfaces use either PRIVATE or YCBCR_P010. This is
strong evidence that the camera HAL can move 10-bit samples through a public
pipeline. It does **not** guarantee that OPlus' proprietary HEIC encoder path is
enabled for every still mode or lens.

## Next enhancements after first successful capture

Once real 10-bit HEIC is verified on the target build:

- 10-bit Live Photo, gated separately;
- per-lens support matrix;
- Master/professional-mode 10-bit HEIC;
- metadata preservation and color-profile validation;
- DCI-P3/BT.2020 transfer/primaries audit;
- 10-bit + HDR/ProXDR coexistence tests;
- HEIF blur-edit/gallery compatibility;
- fallback export to SDR JPEG for compatibility without altering originals.
