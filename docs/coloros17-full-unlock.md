# ColorOS 17 full-unlock compatibility track

This branch is the first source-level refactor of CameraBoost from a single 10-bit
gate experiment into a runtime-aware OPlus camera capability layer.

## Evidence baseline

The compatibility work is grounded in three camera baselines supplied for the target
OnePlus 13 environment:

- Camera 6.070.228 — known-good legacy baseline with Morpho AI Capture Guide.
- Camera 7.006.92 — ColorOS 17 ROM baseline observed on the device.
- Camera 7.013.30 — current updated system camera and primary compatibility target.

The proprietary APKs, ML models and camera blobs are not committed to this repository.
Only derived compatibility facts are recorded here.

## Confirmed architecture migration

### AI photography

6.070.228 exposes the legacy stack:

- `com.morphoinc.anchortracking.sdk.MorphoInitParams`
- Morpho anchor-tracking native libraries
- `ai_capture_guide_composition.coz`
- `ai_capture_guide_scan.coz`

7.013.30 removes that dependency and exposes the modern stack:

- `com.oplus.camera.aicomposition.OplusAIComposition`
- `com.oplus.camera.feature.aicomposition.state.CompositionStateMachine`
- `com.oplus.camera.feature.aicomposition.view.AICompositionPreviewView`
- `com.oplus.camera.feature.aicomposition.inspiration.deepthink.DeepThinkManager`
- `ai_composition_rect_scale.coz`
- `ai_composition_screen_recognition.coz`

Therefore a missing `MorphoInitParams` is not a valid reason to disable AI guidance on
ColorOS 17. The module now detects the modern AI Composition runtime separately.

### Live Photo

Legacy fixed obfuscated anchors such as `nc.c`, `hj.l1` and `sj.i` are not stable
across the 6.x -> 7.x camera transition. 7.013.30 still exposes a full Live Photo
pipeline and feature vocabulary, including:

- `com.oplus.camera.livephoto.support`
- `com.oplus.camera.video.livephoto.support`
- `com.oplus.camera.heif.support.livephoto`
- `com.oplus.livephoto.support.10bit`
- `com.oplus.camera.livephoto.mastermode.support`
- `com.oplus.camera.livephoto.enable.eis`
- `com.oplus.camera.livephoto.enable.frc`

CameraBoost therefore treats semantic feature keys and runtime class presence as the
stable contract rather than obfuscated class names.

### Filters

7.013.30 keeps
`com.oplus.ocs.camera.ipusdk.processunit.filter.list.FilterGroupManager` but removes
legacy fields used by older modules. The compatibility hook now only depends on the
structural `sFilterGroup` / `sProFilterGroup` relationship and treats missing legacy
fields as non-fatal.

## Unlock model

The full-unlock build uses four layers:

1. runtime architecture detection;
2. serialized OPlus camera config patching;
3. direct VendorTag / CameraConfig getter overrides;
4. value-validation and filter-group compatibility hooks.

The generic policy enables existing scalar OPlus software gates that end in
`.support`, `.enable`, `.default.open` or `.status.on`, plus a reviewed set of
known cross-generation gates.

It intentionally does **not** override thermal, overheat, calibration, protection,
factory, low-memory or explicit negative/unsupported gates. Full feature exposure does
not mean disabling hardware safety interlocks.

## Current v1 coverage

The first pass includes:

- 10-bit HEIC
- 10-bit / HEIF Live Photo gates
- legacy AI Capture Guide + ColorOS 17 AI Composition detection
- Master/JPG MAX and high-resolution feature gates
- XPAN cross-camera gates
- telephoto and macro software gates
- multi-video / ultra-wide feature gates
- 120 FPS value legality
- ColorOS 17 FilterGroup structural compatibility

## Still to implement

The next pass must map pipeline-specific behavior that cannot be solved by boolean
feature gates alone:

- RAWMAX/JPGMAX capture pipeline and metadata path
- UnitConfig lifecycle / delayed resolution
- APS preview/capture decision config mutations
- ColorOS 17 LUT staging and runtime lookup
- thumbnail/MediaStore compatibility bridge
- per-lens validation for 120 FPS, 8K, HDR and MAX modes
- final-file validation for 10-bit, Ultra HDR, RAW and Live Photo outputs

A menu item becoming visible is treated only as **advertised**. It is not marked
validated until capture, encoding and final media verification succeed.
