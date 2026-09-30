# OnePlus 13 10-bit still path — static evidence closure

The goal is **not merely HDR processing**. The target is a real stored 10-bit HEIF/HEIC
still path whose samples, encoder negotiation and color metadata are all coherent.

## What is now proven

The evidence forms a much stronger chain than a Camera UI switch:

```text
Snapdragon 8 Elite supports 10-bit HEIF
            ↓
OnePlus 13 stock uses RAW10 capture routes
            ↓
SM8750 vendor defines real 10-bit YCBCR_P010
            ↓
P010 has an HW_IMAGE_ENCODER allocation variant
            ↓
SM8750 vendor declares c2.qti.heic.encoder
            ↓
OnePlus 13 stock contains libapsultrahdr + 10-bit-aware BasicTone
```

The most important new target-common evidence is:

- `YCBCR_P010` is described with 10-bit Y/Cb/Cr components;
- the P010 format has an explicit `HW_IMAGE_ENCODER` alignment variant;
- `media_codecs_sun.xml` declares
  `c2.qti.heic.encoder : image/vnd.android.heic`;
- the encoder alias is `OMX.qcom.video.encoder.heic`;
- the declared size ceiling reaches 16384×16384.

The public common-vendor history shows the current HEIC codec XML was imported in the
OOS 11.F.74 update and has no later file-history change in that repository.

## What is still *not* proven

The HEIC codec XML does **not** publish an input bit-depth or Main10/P010 profile for the
HEIC encoder block.

Therefore we still cannot collapse:

```text
P010 exists + HEIC encoder exists
```

into:

```text
c2.qti.heic.encoder accepts P010 and emits the desired 10-bit HEIF path
```

That negotiation is now the narrowest encoder-layer gap.

Three other layers remain deliberately unresolved:

1. the project-23821 public Camera HAL ten-bit dynamic-range contract;
2. still-image CICP/ICC/transfer/matrix metadata;
3. the current OPlus Camera/APS runtime ownership/value of
   `com.oplus.10bits.heic.encode.support`.

## Why RAW10 and UltraHDR do not close the gap

OnePlus 13 stock `camera_unit_config` contains many explicit
`capture_raw10_dol` and `capture_raw10_fullsize` routes. This proves the acquisition
side is capable of carrying more than an 8-bit JPEG-style path.

Likewise, stock contains `libapsultrahdr`, and the uploaded FX8U processing module
contains UltraHDR/JPEG_R and P010 processing surfaces.

Neither is equivalent to a 10-bit HEIF final file:

- RAW10 is sensor/RAW-domain data;
- JPEG_R/UltraHDR is a gain-map HDR representation;
- 10-bit HEIF requires the HEIF codec path itself to preserve 10-bit samples and carry
  correct color metadata.

## Hook policy

The Android hook now fails closed.

Even the `enable10bit` build cannot mutate the OPlus gate until all four static evidence
flags are explicitly verified:

- OPlus Camera/APS gate;
- target HAL 10-bit stream path;
- 10-bit HEIF encoder path;
- color metadata path.

Until then the hook remains observation-only.

This prevents a false success where a UI option appears but the camera silently stores an
8-bit HEIC/JPEG-compatible result.

## Current static priority

The next public-file search order is:

```text
Codec2/OMX HEIC P010/Main10 negotiation
        ↓
project-23821 HAL dynamic-range metadata
        ↓
OPlus Camera/APS 10-bit runtime gate
        ↓
CICP/ICC/HDR still metadata
```

No device test is required for this phase yet.
