# Direct 25MP on Camera 7.013.30: CommonCapMode architecture

## Correct semantic target

OPCameraPro's "direct 25MP" is **not** a request to expose the standalone
HighPixel mode in the mode panel.

The intended behavior is:

```
CommonCapMode (normal Photo)
  -> high-resolution/QBC capture decision
  -> TurboRAW Resolution Enhance
  -> 25MP output
```

The standalone `HighPixelMode (im.e0)` is therefore not required for the
feature and must not be synthesized merely to restore direct 25MP.

## Camera 7.013.30 evidence

The supplied Camera 7.013.30 APK's `CommonCapMode = gm.q2` already contains
the host-side feature vocabulary for this path:

- `com.oplus.turboraw.re.support`
- `com.oplus.main.full.qbc.output.sizes`
- `com.oplus.config.full.bining.qbc.enable`
- `com.oplus.feature.highpixel.merge.support`
- `pref_camera_high_resolution_key`
- `key_high_picture_size`
- `func_switch_capture_resolution_support`

This means the correct ColorOS17 migration should keep the normal Photo mode
and feed it the correct QBC/TurboRAW decision/configuration state.

## OPCameraPro 3.2.10 architecture

The supplied 3.2.10 APK includes:
- `Camera25MpDecisionNative.installDecisionRedirectNative`
- `Camera25MpDecisionNative.activateDecisionRedirectNative`
- redirection support for:
  - `/odm/etc/camera/config/oplus_camera_aps_config`
  - `/odm/etc/camera/config/oplus_camera_preview_decision_config.json`

This is consistent with a capture-decision redirect rather than a mode-menu
unlock.

## Correction after hotfix5

CameraBoost hotfix5 incorrectly reintroduced standalone HighPixel feature
gates. That direction is deprecated.

From hotfix6 forward CameraBoost remains observation-only in
`com.oplus.camera`. Direct 25MP belongs to the OPCameraPro CommonCapMode
runtime/config adaptation.

## Next migration work

1. Keep `CommonCapMode` native and do not add a HighPixel mode entry.
2. Preserve OPCameraPro's direct-25MP config tags and native decision redirect.
3. Replace the legacy file-based capability snapshot with ColorOS17 evidence
   from CameraUnit/QBC/sensor-mode.
4. Validate that the native redirect installs and produces prepared QBC plans.
5. Validate actual normal-Photo output dimensions and APS/TurboRAW state.
