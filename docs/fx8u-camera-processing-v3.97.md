# FX8U Camera Processing For OP13 v3.97 — derived static analysis

Source artifact SHA-256:

`6e6a733544261537af6c6ad2f2e2a1a32f41417129ff719c8b7ef93d39e11068`

This document contains **derived observations only**. The proprietary module ZIP, shared
libraries, ML models and full camera configuration files are not stored in this repository.

## What the module actually is

The module declares itself as:

- id: `um8s_op13_ocvm_proc_addon`
- name: `FX8U Camera Processing For OP13`
- version: `3.97`
- author: `UltraM8`
- description: `Find x8 ultra camera processing blobs port for OnePlus13.`

Its payload overlays `/odm` and is overwhelmingly camera-processing libraries, configs
and ML models. It is not primarily a Camera APK replacement.

## Strong OnePlus 13 target adaptation evidence

The package is not a blind donor dump.

- video LTM configuration is keyed by project `23821`;
- `libAlgoInterface.so` contains `ApsPreviewDecision23821` and target capture-decision
  classes for rear/front normal and bokeh modes;
- the target preview decision surface includes HQ RAW, TurboHDR, HDR, night,
  sensor-mode and quick-shot decision methods;
- segmentation configuration declares product `8750`, vendor `QCOM` and QNN;
- bokeh configuration also declares product `8750` with QNN/APS API 6.0.

This means the package mixes **OnePlus-13-specific APS decisions/runtime adaptation** with
**Find X8 Ultra donor algorithms/models**.

The library also contains decision code for other project IDs, so the safest description
is a multi-project APS library with explicit 23821 support rather than a library compiled
only for OnePlus 13.

## 10-bit / HDR / HEIC relevance

The uploaded binaries expose concrete pipeline signals:

- `P010` and `UBWCTP10` buffers in video LTM / AI-NR nodes;
- `ChiStreamIntentHeic` / HEIC buffers;
- UltraHDR / JPEG_R source-codec functions in `libAlgoInterface.so`;
- P010-specific upscale entry points;
- BasicTone parameters with separate 10-bit and 8-bit vignette/dither strengths;
- Dolby-specific video AI-NR configuration.

These observations are highly relevant to CameraBoost's existing 10-bit color workstream,
but they are not by themselves proof that stock OnePlus 13 enables 10-bit HEIF still
encoding.

## HybridRAW is the center of gravity

The archive expands to about **1.65 GiB**. The `hybridraw_models` directory alone is
about **1.34 GiB**.

HybridRAW includes:

- multi-frame main / ultrawide / telephoto paths;
- motion-mask and AI fusion;
- dehaze and scene/sky/person segmentation;
- AITM tone mapping;
- JDD enhancement;
- high-zoom super-resolution;
- Stable-Diffusion-style super-resolution.

### Stable-Diffusion SR is real, not inferred

`libOPAlgoCamHybridRaw.so` contains source-path strings under:

`stage2_postproc/stableDiffusionSR`

including components named:

- `Unet`
- `VaeEncoder`
- `VaeDecoder`
- `Sampler`
- `PostSRNet`

The tuning library references:

- `unet.bix`
- `vae_encoder.bix`
- `vae_encoder_vertical.bix`
- `vae_decoder.bix`
- `vae_decoder_vertical.bix`

Those five assets consume about **735 MiB uncompressed / 476 MiB compressed**.

Therefore they should be treated as an optional high-super-resolution capability pack,
not automatically as a baseline dependency for 10-bit/HDR photography.

## X8 Ultra donor contamination that must not be auto-enabled

The package still retains a five-camera donor model:

- `main`
- `front`
- `uwide`
- `tele`
- `utele` (camera ID 4)

It also contains explicit `Tele2` / 6x HybridRAW classes and models, plus an
`AIAEVideoModelUltraTele.bin` asset.

OPPO's official Find X8 Ultra specification exposes both 3x and 6x telephoto cameras,
whereas OnePlus 13 officially exposes a single 3x LYT-600 telephoto. Therefore the
`tele2 / 6x / utele / Camera4` branch must default to **blocked** on OnePlus 13.

Do **not** broadly delete every color/spectral asset: OnePlus 13 official specifications
also list spectral sensors. Exact calibration compatibility still needs mapping.

## Size architecture

A practical packaging direction is:

### Core Processing

Keep the target APS decision layer and the normal main/ultrawide/3x/front processing
families:

- AlgoInterface / AlgoProcess;
- HybridRAW core;
- HDR transform / TurboHDR surface;
- BasicTone;
- Video AI-NR / VideoLTM;
- segmentation/bokeh dependencies;
- main / ultrawide / tele1 / front model families.

### Optional AI-SR pack

Move the Stable-Diffusion SR family and high-magnification generative enhancement into a
separate optional pack after the runtime dependency graph is confirmed.

### X8 Ultra-only pack

Do not load by default:

- Tele2 / 6x-specific HybridRAW models;
- Camera4 / utele-only configuration;
- UltraTele video model.

Just externalizing the five Stable-Diffusion SR core files plus obvious Tele2/UltraTele
assets removes roughly **514 MiB of compressed payload**, leaving an estimated **568 MiB**
before further dependency-aware slimming.

## It is not standalone

The addon ships 28 shared libraries but has numerous external QTI/OPlus/APS dependencies,
including:

- `libapsjpeg.so`
- `libapsexif.so`
- `libexif-jpeg-aps.so`
- `libmpbase.so`
- `libsharebuffer.so`
- `libtrace.so`
- QTI offline-camera AIDL
- CamX node utilities
- OPlus osense client libraries
- OpenCL / CDSP RPC

The installer source contains an intended check for a main module named
`um8s_op13_ocvm`, but that line is commented out in v3.97. Thus the package *states* an
architectural dependency while not actually enforcing it during installation.

Before deriving a standalone CameraBoost package, the matching main module or the stock
OnePlus 13 userspace must be inventoried to close this dynamic dependency graph.

## Next engineering step

Do not copy this module wholesale.

The next implementation should:

1. ingest/analyze the matching `um8s_op13_ocvm` main module if available;
2. construct a OnePlus 13 lens map and block camera ID 4 / Tele2 / utele;
3. split Core / AI-SR / donor-only assets;
4. map the observed P010 / UltraHDR / JPEG_R / HEIC surfaces into CameraBoost's existing
   color-still capability graph;
5. keep all proprietary blobs outside the public repository.


## Second dependency layer: runtime-loaded plugins and models

ELF `DT_NEEDED` dependencies are only part of the closure.

The shipped orchestration libraries also contain explicit runtime path references to files
that are **not packaged in this addon**. Confirmed examples include:

| Referenced file | Referencing addon library | Likely surface |
|---|---|---|
| `/odm/lib64/libapsultrahdr.so` | `libAlgoProcess.so` | UltraHDR / JPEG_R |
| `/odm/lib64/libarcsoft_turbo_hdr_grf.so` | `libAlgoProcess.so` | TurboHDR |
| `/odm/lib64/libVDUpScale.so` | `libAlgoInterface.so` | upscale |
| `/odm/lib64/libSuperRaw.so` | `libAlgoInterface.so` | Super RAW |
| `/odm/lib64/libSuperSensor.so` | `libAlgoInterface.so` | super-sensor processing |
| `/odm/lib64/libarcsoft_quad_super_resolution_raw.so` | `libAlgoInterface.so` | RAW super-resolution |
| `/odm/lib64/libarcsoft_turbo_raw.so` | `libAlgoInterface.so` | TurboRAW |
| `/odm/lib64/libOplusSecurity.so` | `libAlgoProcess.so` | OPlus security/config helper |
| `/odm/etc/camera/simpleunet_4_3.dlc` | `libAlgoInterface.so` | ML model |
| `oplus_camera_algo_traversal_config.json` | `libAlgoInterface.so` | algorithm traversal |
| `oplus_camera_preview_decision_config.json` | `libAlgoInterface.so` | preview decision |
| `dehaze_tuning.txt` | `libAlgoInterface.so` | dehaze tuning |
| `highmagsol_20x.bin` | `libhybridraw_tuningparams.so` | high-zoom SR |
| `highmagsol_40x.bin` | `libhybridraw_tuningparams.so` | high-zoom SR |
| `highmagsol_80x.bin` | `libhybridraw_tuningparams.so` | high-zoom SR |

The three `highmagsol_*` files are referenced by tuning but are **not present anywhere
inside the v3.97 addon ZIP**.

This is strong evidence that the addon expects additional stock or main-module assets.
It does **not** prove that every referenced plugin is mandatory for every mode: large APS
libraries commonly contain optional algorithm loaders whose selection is controlled by
device/mode/config decisions.

Accordingly, dependency closure now has two independent axes:

1. **ELF link-time closure** — `DT_NEEDED` libraries;
2. **runtime feature closure** — plugin/config/model paths selected by APS decisions.

A slim CameraBoost derivative must satisfy both before a file can be declared safe to
remove.

## Refined architecture interpretation

The uploaded artifact is not simply “X8 Ultra blobs” and not a self-contained camera
stack. It is closer to:

```text
OnePlus-13-aware APS decision/orchestration
        +
selected Find X8 Ultra processing libraries/models
        +
stock/main-module runtime plugins and config
```

That is useful for CameraBoost because the 23821 decision code tells us which capability
surfaces the porter expected to run on OnePlus 13, while the missing runtime references
show exactly where the addon still relies on the surrounding OPlus camera stack.
