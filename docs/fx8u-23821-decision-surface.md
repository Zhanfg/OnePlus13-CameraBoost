
# OnePlus 13 project 23821 APS decision surface derived from FX8U v3.97

This file records **symbol/key-level observations** from the uploaded module. No donor
library or model is redistributed.

## Target-specific APS class

`libAlgoInterface.so` contains an explicit `ApsPreviewDecision23821` implementation.

Its exported method surface includes:

- `hqRawParameters`
- `turbohdrParameters`
- `hdrParameters`
- `nightParameters`
- `sensorModeParameters`
- `updateQuickShotParams`
- `switchToMFNR`
- `faceRectifyParameters`
- before/after capture and process hooks

It also contains a project-specific `mBurstThermalThreshold` object.

This is stronger evidence than a generic feature string: it shows that the APS decision
library carries explicit 23821 behavior for RAW/HDR/night/sensor-mode/quick-shot policy.

It still does **not** prove that stock OnePlus 13 enables every branch, because selection
depends on camera config, VendorTags, runtime plugins and sensor/HAL capabilities.

## Capture decision modes registered for 23821

The target-specific capture-decision class exposes:

- rear normal;
- front normal;
- rear bokeh;
- front bokeh;
- AI high-pixel.

Rear/front normal and bokeh classes each expose target-specific pipeline update methods.

Do not infer that Master or Night is unsupported just because there is no identically named
23821 capture class: this multi-device APS library also uses generic/base implementations,
and the target preview decision explicitly contains Master/RAW/night-related parameter
surfaces.

## Important OPlus parameter families

### RAW / HDR

The binary directly references OPlus parameters for:

- HybridRAW version/thread/ORMS/skin-mask;
- TurboRAW moving-object, shutter-wait, thermal, tripod and version policy;
- TurboHDR memory, CPU binding, RAW-to-RAW, sensor name and limits;
- RAW HDR sensor exposure/gain and ISP gain/lux;
- front UHDR support;
- HDR v4 / merge-HDR / HDR-transform controls.

### Zoom / SAT

Observed decision keys include:

- optical-zoom APS metadata;
- SAT logical camera ID;
- preview/snapshot sensor masks;
- SAT master pipelines;
- multi-camera count;
- tele zoom gate;
- in-sensor zoom;
- tele and ultrawide upscale;
- tele calibration;
- **ultratele calibration**.

The last item is donor contamination for OnePlus 13 unless an explicit target mapping is
proven. It aligns with the module's separate `utele / Camera4 / Tele2 / 6x` assets.

### High-pixel / super-photo

The decision layer includes:

- SuperPhoto;
- UltraHD face/no-face branches;
- SuperSensor;
- UltraHighDefinition;
- YUV-SR / in-sensor-zoom integration.

### Quick-shot and thermal policy

The target library has explicit policy keys for:

- Master quick-shot threshold;
- RAW Max quick-shot threshold;
- rear-night quick-shot threshold;
- SuperNight thermal level;
- TurboRAW thermal level;
- TurboRAW process time limit.

These are especially useful for CameraBoost because performance/thermal tuning can be
separated from the actual imaging algorithm.

## CameraBoost implementation consequence

The correct next clean-room architecture is now:

```text
OnePlus 13 lens map
      ↓
23821 APS decision/config map
      ↓
runtime plugin + model closure
      ↓
P0 feature families
  ├── 10-bit / UHDR / JPEG_R
  ├── RAW / RAW Max / HybridRAW
  ├── TurboHDR
  ├── high-pixel / SuperPhoto
  └── SAT / 3x tele / zoom
      ↓
performance + thermal policy as a separate layer
```

Do not import `Camera4 / utele / Tele2 / 6x` into the target lens map.
