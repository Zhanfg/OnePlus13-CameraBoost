# OPCameraPro 3.2.10 Test7 — ColorOS 17 native-camera baseline

Target:
- OnePlus 13 PJZ110
- Camera 7.013.30
- OPCameraPro 3.2.10

## Why Test6 is retired

The direct-25MP trace captured an ART verifier failure in the modified
OPCameraPro bootstrap class:

```
pre-onCreate bootstrap failed, camera continues unhooked:
Verifier rejected class ...
register v4 has type Undefined but expected Integer
```

That means Test6 is not a valid baseline even when the APK itself installs.

## Test7 changes

Test7 is rebuilt from the original supplied 3.2.10 APK and makes only
verifier-safe mutations:

1. `enable25MP = false`
   - disables OPCameraPro's legacy ColorOS16-era direct-25MP config/runtime
     path while we establish the Camera 7.013.30 native baseline;
   - no conditional branch bytecode is rewritten.
2. `enableMasterModeParamFix = false` (actual field 19317).
3. Skip the old Protobuf FeatureTable installer by NOP-ing its single
   **void call site**, not by changing the target method body.
4. No capability-snapshot branch bypasses.
5. PseudoUltraHdr and MAX/RAW implementations are not disabled by this patch.

This build is intentionally not the final direct-25MP implementation. Its
purpose is to recover and observe Camera 7.013.30's own:
- `com.oplus.camera.feature.high_resolution`
- `HighResolutionPresenter (rg.g)`
- SAT lens switching
- native High Resolution UI

before reimplementing 25MP using the native capture-time path.

## Native Camera 7.013.30 high-resolution architecture

Recovered from the supplied APK:

- `rg.a = HighResolutionKeys`
- `rg.b = HighResolutionModel`
- `rg.g = HighResolutionPresenter`
- feature name: `com.oplus.camera.feature.high_resolution`
- preference key: `pref_camera_high_resolution_key`
- state values: `standard`, `standard_high`

Preserved `AndroidTestAdapter` methods confirm the native API:

- `setPhotoHighResolution(boolean)`
- `getHighResolutionState()`
- `setTurboRawResolutionEnhanceOnOff(boolean)`
- `getSupportTurboRawResolutionEnhance()`
- `isTurboRawResolutionEnhanceCapture()`

Notably, `setTurboRawResolutionEnhanceOnOff` writes the native
`pref_capture_resolution_list` DataKey to `high` / `standard`.
This is the path to study for the final per-shot direct-25MP migration; it is
not equivalent to forcing the preview mode permanently into HighPixel state.
