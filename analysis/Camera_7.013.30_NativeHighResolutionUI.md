# Camera 7.013.30 native High Resolution UI mapping

This note corrects the earlier conflation of three different concepts:

1. `HighPixelMode (im.e0)`: standalone high-pixel shooting mode.
2. `HighDefinitionMode (im.y)`: Hasselblad/AI "super definition" style mode.
3. `com.oplus.camera.feature.high_resolution`: native **Photo-mode High Resolution toggle**.

## Native High Resolution feature

From the supplied Camera 7.013.30 APK:

- Feature name: `com.oplus.camera.feature.high_resolution`
- FeatureFactory: `hb.y#c`
- Feature presenter: `rg.g`
- Model/helper: `rg.b`
- Presenter log name: `HighResolutionPresenter`
- Menu preference key: `pref_camera_high_resolution_key`
- State values: `standard` and `standard_high`
- Menu text resource: `camera_setting_menu_high_resolution_item`
- Submenu text resource: `camera_setting_submenu_high_res`

The runtime log from PJZ110 / Camera 7.013.30 confirms that normal Photo
(`modeName=common`, runtime class `im.j1`) already loads this feature:

`com.oplus.camera.feature.high_resolution`

Therefore the feature implementation is present even when the UI entry is absent.

## Menu-panel gate

`CameraSettingsConfig.parseMenuPanel()` contains this exact gating structure:

```
if (!CameraConfig.getConfigBooleanValue(
        "com.oplus.feature.high.definition.support")
    && CameraConfig.getConfigBooleanValue(
        "com.oplus.pre.high.resolution.support")) {
    mMenuPanelList.add("pref_camera_high_resolution_key");
}
```

So forcing `key_high_picture_size`, `f2()`, QBC state, output size, or zoom
is the wrong layer for restoring the button.

## hotfix12 policy

hotfix12 restores only:
- the native menu-panel entry `pref_camera_high_resolution_key`, if missing;
- the runtime `getSupportFunction("pref_camera_high_resolution_key")` UI support
  result for the real normal-photo runtime object `im.j1`.

It does **not** force:
- `standard_high`;
- `key_high_picture_size`;
- QBC/session state;
- output dimensions;
- SAT/lens/zoom state;
- HighPixelMode;
- HighDefinitionMode.

The native `HighResolutionPresenter` remains responsible for switching
`standard <-> standard_high`.
