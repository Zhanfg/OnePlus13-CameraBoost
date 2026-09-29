# Research plan

## Phase 1 — Target inventory

Run `tools/collect_device.sh` on stock OnePlus 13 and record:

- exact build fingerprint and OPlus Camera package version;
- CameraService characteristics;
- loaded camera/MMRM/video kernel modules;
- camera-related vendor file names and hashes.

Do not commit proprietary libraries, calibration files, user photos or decrypted vendor configuration.

## Phase 2 — Configuration evidence

Produce a local decrypted APS/config dump using an external tool if desired, then run:

```bash
cameraboost diff dumps/oneplus13 dumps/reference --out feature-diff.json
cameraboost matrix feature-diff.json --out feature-matrix.md
```

Only the derived diff/matrix should be considered for publication after manual review.

## Phase 3 — Feature gate probes

Map the following OPlus Camera decision points:

1. config legality gate;
2. protobuf feature table;
3. APS decision/config;
4. CameraCharacteristics / VendorTags;
5. sensor mode / encoder availability.

Initial priority:

- 4K120 / 1080p120;
- preview HDR / HLG;
- 10-bit / Log / Dolby-related capability advertisement;
- Master/professional mode feature table;
- live photo and portrait feature gates;
- SAT/zoom routing.

## Phase 4 — Runtime validation

For each feature, store evidence using `cameraboost.gates.FeatureEvidence`.

No feature becomes auto-enabled until it reaches `validated`.

## Phase 5 — Kernel branch

Only after a feature fails at streaming/stability rather than app gating, investigate:

- qcom camera-kernel;
- OPlus camera kernel extensions;
- MMRM;
- interconnect bandwidth votes;
- IOMMU/DMA-BUF;
- scheduler/uclamp;
- thermal constraints.
