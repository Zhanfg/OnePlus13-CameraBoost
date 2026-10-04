# OPCameraPro 3.2.10 → ColorOS 17 mode compatibility test

Target:
- OnePlus 13 / PJZ110
- ColorOS 17 / Android 17
- Camera 7.013.30
- OPCameraPro package com.tlsu.opluscamerapro, versionName 3.2.10

## Why this test exists

Camera 7.013.30 keeps its own native mode registry and CaptureParam UI generations.
OPCameraPro 3.2.10 contains compatibility code that predates this Camera generation and
can modify the camera mode FeatureTable, force Master mode version 2.0, and apply an
older Master parameter-bar fix.

Observed failures include:
- most non-photo/video mode tables becoming unusable,
- Master/Professional UI recycling a df.s1 view where ListModeBarAdapter expects if.k0,
- ModeSwitcher exposing a mode UI while its BaseMode instance is unavailable.

## Test1 binary patch

The patch intentionally changes only these behaviors:

1. VendorTagSettings.enableMasterMode is held false.
   This prevents the legacy com.oplus.feature.master.mode.version = 2.0 override.
2. VendorTagSettings.enableMasterModeParamFix is held false.
3. ProtobufFeature's hook installer is made a no-op, so it cannot hook
   FeatureTable.parseFrom(byte[]) / parseFrom(InputStream), rebuild mode tables,
   addNewMode, or addGroupFeatureTable.

Not changed:
- MasterJpgMaxCompat
- MasterRawMax
- Camera25MpDecision
- MasterUhdrApsDiag / PseudoUltraHdr
- other per-feature runtime hooks

Input APK SHA-256:
`51536e4dfe03057ee5df721a75194e26be337f3dd2cac1653c85277932636d03`

This is a diagnostic compatibility build, not a claim that all ColorOS 17 migration
work is complete. If native modes recover, the next step is to reimplement the needed
Protobuf additions through Camera-7.x-aware, mode-specific adapters instead of using the
legacy generic mode-table mutation.
