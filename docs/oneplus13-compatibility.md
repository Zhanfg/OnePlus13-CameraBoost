# OnePlus 13 compatibility matrix

This profile maps the official OPPO + OnePlus camera capability universe onto OnePlus 13
without performing device-side validation.

## Status meanings

- **native** — explicitly present in OnePlus 13 official materials or direct target hardware.
- **high-confidence** — the required hardware/platform capability is strongly evidenced by
  first-party sources, but OPlus HAL/APS/app integration still has to be mapped.
- **portable-with-adaptation** — no hard blocker is established, but the feature may require
  substantial algorithm, model, HAL, UI or metadata work.
- **hardware-blocked** — the exact implementation needs dedicated hardware absent from
  OnePlus 13 official specifications.

This is an engineering compatibility assessment, not a claim that an untested feature
already works.

## Why Find X9 is the primary sensor donor

Official specifications establish the following pairing:

| Camera | OnePlus 13 | OPPO Find X9 | Match |
|---|---|---|---|
| Main | Sony LYT-808 | Sony LYT-808 | exact |
| 3x telephoto | Sony LYT-600 | Sony LYT-600 | exact |
| Ultra-wide | S5KJN5 | JN5 | same sensor family / likely exact |
| Front | Sony IMX615 | Sony IMX615 | exact |

The important limitation is SoC: Find X9 uses MediaTek Dimensity 9500, while OnePlus 13
uses Snapdragon 8 Elite. Therefore Find X9 is excellent for feature semantics, sensor modes,
camera-app behavior and algorithm reference, but its ISP binaries must not be transplanted
into Qualcomm CamX/CHI.

## Snapdragon 8 Elite ceiling

Qualcomm's official product brief lists:

- triple 18-bit ISPs;
- Rec.2020 photo/video capture;
- up to 10-bit photo/video;
- 10-bit HEIF/HEIC capture;
- HDR10/HDR10+/HLG/Dolby Vision;
- 4K120 capture;
- computational HDR with up to four exposures.

That makes the following P0 targets particularly important:

- 10-bit HEIF / full-path 10-bit / DCI-P3 or Rec.2020 color path;
- 4K120;
- Pro Video + LOG + LUT/ACES workflow;
- RAW / RAW+ / 12-bit RAW / RAW MAX;
- expanded Master controls;
- Hasselblad HI-RES;
- Real-Time Triple Exposure;
- TurboRAW HDR;
- AI Telescope Zoom / Stage Mode / gapless zoom.

The profile intentionally keeps exact OPPO O-Log separate from generic OnePlus LOG:
having 10-bit/Rec.2020 capability does not automatically reproduce OPPO's transfer curve.

## Hardware-blocked examples

Do not spend time trying to reproduce the exact implementation of:

- True Color / spectral camera;
- MariSilicon X;
- five-axis OIS;
- dedicated RGBW pipeline;
- dedicated Color Filter or monochrome camera;
- 150° optical field of view;
- dedicated ToF;
- exact 20-bit MariSilicon RAW path.

Some *effects* can be approximated in software, but that is a different feature and should
receive a separate catalog identity.

## Querying the matrix

```bash
cameraboost compat profiles/oneplus13-compatibility.json --summary
cameraboost compat profiles/oneplus13-compatibility.json --priority P0
cameraboost compat profiles/oneplus13-compatibility.json --status hardware-blocked
```
