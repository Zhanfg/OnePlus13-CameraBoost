# OCVM v61.82 main-module clean-room analysis

Pinned upstream: `ObyeBoss/OP13-OCVM@b160412105564844dfab50a30635580962830e50`.

This is a **derived analysis only**. The upstream tree does not expose a LICENSE file at
the pinned commit, so CameraBoost must not copy its source/config/blob payloads unless a
separate license or permission is established.

## What the main module changes

OCVM's main module is the control and low-level patch layer. The uploaded FX8U processing
addon is complementary rather than standalone.

The main module changes all five OnePlus 13 sensor-config families and rewrites a large
part of `CameraHWConfiguration.config`:

| Config | Mod entries | Stock entries | Added | Changed |
|---|---:|---:|---:|---:|
| dodgemain | 153 | 138 | 15 | 22 |
| dodgetele | 127 | 110 | 17 | 26 |
| dodgetele2 | 130 | 117 | 13 | 23 |
| dodgeultrawide | 124 | 112 | 12 | 6 |
| dodgefront | 83 | 78 | 5 | 22 |

No keyed sensor entry was removed in this comparison.

The hardware config grows from 256 to 280 parsed sections, with 35 new sections,
11 removed sections and 158 changed sections.

## High-value capability surfaces

The useful part for CameraBoost is the map of capabilities that OCVM demonstrates are
reachable in this stack:

- expose all RAW sizes across the logical sensor set;
- broaden RAW snapshot feature types;
- add explicit Pro/Night RAW callback configuration;
- add HQRAW night/lux and DRC-gain gates;
- enable frameless TurboRAW policy surfaces;
- expand in-sensor zoom routing and video IZoom coverage;
- broaden VSR exposure;
- expose additional RAW2RAW and high-pixel/QCFA paths.

The front-camera branch is particularly experimental: TurboHDR RAW dimensions are changed
from 3280×2464 to 6560×4928 while MFNR is disabled.

## Image-quality policy: do not blindly inherit it

OCVM deliberately pushes toward maximum retained texture.

For the main sensor, several HDR/coupleHDR tables that contain non-zero stock luminance
denoise, chroma denoise and sharpening values are changed to zero or near-zero. AI-night
zoom tuning similarly sets sharpening/denoise to zero across most normal zoom buckets.

That can be desirable as a **detail-biased profile**, but it is not a safe universal
default. CameraBoost should split this into explicit quality profiles:

```text
Stock-compatible
Balanced
Detail
Experimental RAW-like
```

Capability enabling and aesthetic tuning must remain separate.

## Resource policy: reject the global-force approach

The main module also contains unconditional performance overrides:

- max real-time/non-real-time image buffers = `0xffffffff`;
- CSID/IFE clocks and camera NoC/external bandwidth = `0xffffffff`;
- OPP clock override = 15;
- high-bandwidth IFE frame count = 12;
- capture thermal/FPS policy moved from 20 FPS toward 60 FPS and higher thresholds;
- extra HAL buffers are expanded.

CameraBoost should **not** carry these over verbatim.

Use a mode-aware governor instead:

```text
normal photo      → stock resource envelope
HDR / HybridRAW   → bounded burst boost
night multi-frame → bounded sustained boost
4K60 / HDR video  → sustained bandwidth + thermal feedback
AI-SR             → NPU/DSP/memory boost only while the branch is active
```

Hard thermal limits and memory-pressure rollback remain authoritative.

## Camera-slot correction

OCVM's hardware config swaps the order of `dodgetele` and `dodgetele2` in the logical
SensorNameList, while the public stock extraction already demonstrates that
`dodgetele2` is used by real OnePlus 13 telephoto modes.

Therefore filename/slot labels such as `tele2`, `6x` and `utele` must never be used
alone to infer physical camera topology.

## Clean-room direction

The architecture is now:

```text
stock OP13 camera stack
      ↓
target capability map (23821)
      ↓
safe feature gates / RAW exposure
      ↓
quality profiles (separate)
      ↓
optional FX8U processing deltas
      ↓
mode-aware resource governor
      ↓
runtime validation + rollback
```

This is materially safer and smaller than cloning OCVM's main module plus the 1+ GiB
processing addon wholesale.

## Upstream-reported regressions to turn into tests

OCVM reports the following known issues, so CameraBoost should convert each into a
regression test rather than rediscover it on-device:

- ultrawide → main switching hang in photo modes;
- laggy portrait preview;
- merge bleed with angled sunlight in the FX8U processing addon;
- zoom above 10× failing to save the final image.

These belong in the eventual stress suite together with repeated open/close, lens switching,
thermal soak, RAW/HDR bursts, camera-service restart and memory-pressure testing.
