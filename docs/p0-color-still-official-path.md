# P0 color-still: official 10-bit HEIF path

This phase deliberately does **not** use device-side testing and does not accept
third-party hooks as official evidence.

## 1. Platform ceiling is already high enough

Qualcomm's Snapdragon 8 Elite product brief explicitly lists:

- Rec. 2020 photo and video capture;
- up to 10-bit photo and video;
- 10-bit HEIF / HEIC photo capture;
- Google Ultra HDR photo capture.

Therefore OnePlus 13 is not constrained to 8-bit still capture by the SoC.

## 2. OPlus historically implemented 10-bit HEIF as a system imaging feature family

Official OnePlus and OPPO source releases both expose the following enabled build symbols:

- `OPLUS_FEATURE_10BIT_HEIF`
- `OPLUS_FEATRUE_HEIF_OPTIMIZE` (official spelling)
- `OPLUS_FEATURE_HEIF_CONVERTER`
- `OPLUS_FEATURE_IMAGE_PROCESSING`
- `OPLUS_FEATURE_ROI_ENCODE_QCOM`

This is important because it shows that the OPlus 10-bit HEIF path historically crossed
system image-processing / conversion / encoder integration boundaries. Restoring only a
Camera-app preference cannot be considered a complete implementation.

## 3. Android requires a real HAL path

AOSP's 10-bit camera-output contract requires a manufacturer implementation to expose:

- `ANDROID_REQUEST_AVAILABLE_CAPABILITIES_DYNAMIC_RANGE_TEN_BIT`;
- supported dynamic-range profiles, including HLG10;
- a recommended ten-bit dynamic-range profile;
- 10-bit stream support through P010 or implementation-defined PRIVATE output;
- appropriate Gralloc 4 HDR metadata.

These signals belong to Camera HAL / stream configuration, not the LSPosed UI layer.

## 4. OnePlus 13 public kernel boundary

The official SM8750 release exposes the Qualcomm/OPlus camera kernel surface
(`camera.ko`, `camera_extension.ko`, camera DT and MMRM), but its public camera module
sources do not expose a OnePlus-13-specific "enable 10-bit HEIF" switch.

The correct investigation order is therefore:

```text
OPlus system feature/config
        ↓
Camera HAL / dynamic-range metadata
        ↓
P010 or PRIVATE 10-bit stream
        ↓
HEIF / HEIC encoder bit depth
        ↓
CICP / ICC / primaries / transfer metadata
        ↓
OPlus Camera / APS feature exposure
        ↓
final stored file
```

Kernel work is only justified if this upper pipeline selects a valid 10-bit path but fails
because of buffer, bandwidth or resource constraints.

## 5. Official OnePlus 13 OTA/userspace artifacts still needed

The next official-source ingestion targets are:

- OPlus native-feature/build property material;
- Camera provider/HAL metadata and VendorTag declarations;
- media codec / HEIF / HEIC encoder declarations;
- OPlus Camera / APS configuration;
- color-space and HDR metadata configuration;
- Gallery/Photos HEIF/HDR decode and edit capability.

Their absence from the public **kernel** repository is not evidence that they are absent
from the shipping device.

## 6. Scanner support

`tools/extract_official_camera_features.py` now also detects:

- OPlus 10-bit/HEIF system imaging symbols;
- Android 10-bit Camera HAL tokens;
- HEIF / HEIC / P010 tokens;
- Rec.2020 / BT.2020 / DCI-P3 / HLG / HDR / Dolby Vision / ProXDR tokens.

The scanner is intentionally evidence-only. It does not patch an artifact and does not
turn a token match into a support claim.

## Non-equivalences

Keep these separate during all later implementation work:

- 10-bit HEIF != RAW10 / RAW12;
- 10-bit HEIF != JPEG_R / Ultra HDR;
- ProXDR != proof of 10-bit stored samples;
- P010 support != proof that OPlus' HEIF encoder is configured for 10-bit.


## CLI: find the next missing layer

After scanning one or more plaintext official artifacts:

```bash
python tools/extract_official_camera_features.py \
  official/oplus_native_features.mk \
  official/camera_metadata.txt \
  official/media_codecs.xml \
  -o official-camera-scan.json

cameraboost color-path official-camera-scan.json
```

The result is deliberately conservative. For example, finding `HEIC`, P010, Rec.2020
and the OPlus 10-bit HEIF build symbol still reports `heif-encoder` as the next layer
until there is explicit official evidence that the selected still encoder path is configured
for 10-bit output.

The CLI also restores the previously intended compatibility-profile command:

```bash
cameraboost compat profiles/oneplus13-compatibility.json --summary
cameraboost compat profiles/oneplus13-compatibility.json --priority P0
```
