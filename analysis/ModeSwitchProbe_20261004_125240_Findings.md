# ModeSwitchProbe 2026-10-04 12:52 — findings

Target:
- OnePlus 13 / PJZ110
- ColorOS 17 / Android 17
- Camera 7.013.30
- OPCameraPro 3.2.10 ModeCompat Test2

## 1. Current run did not crash the Camera process

The 45-second PID trace kept com.oplus.camera alive for the whole run. The current
logcat crash buffer contains no new Java or native fatal crash.

This is important: remaining symptoms are mode-state / pipeline integration problems,
not the previous process-level crashes.

## 2. Test2 Protobuf isolation is functionally useful but bytecode-invalid

ART rejects the patched ProtobufFeature class:

`Verifier rejected class ... unexpected opcode unused-41`

Test2 replaced the first code unit of the target method with return-void. That mutates
a verifier-sensitive method body and is not acceptable as a final patch.

Test3 fixes this by leaving the target method unchanged and replacing the *unique caller*
invoke-virtual (method index 21964) with three NOP code units.

## 3. 25MP runtime redirect is intentionally gated off

Repeated OPCameraPro log:

`Camera25MpDecision: Skip runtime decision redirect: module capability snapshot`

This existed before Test2 as well. It is not a new regression from the Protobuf patch.
The native redirect is guarded by Camera25MpCapability.supported.

Do not force this boolean yet.

## 4. CameraUnit/APS integration is incomplete

Repeated OPCameraPro log:

`UnitConfig: camera_unit_config is null`

During HighPixel/25MP-related switching the vendor camera pipeline repeatedly reports:

`OnUpdateRemosaicInfo(): pApsSensorMode is NULL!`

The Camera process remains alive, but the remosaic pipeline does not have a valid APS
sensor-mode description. This is the highest-priority remaining 25MP integration issue.

## 5. Camera 7.013.30 already contains the native HighPixel/TurboRAW/QBC path

The APK includes stable anchors such as:
- com.oplus.turboraw.re.support
- com.oplus.main.full.qbc.output.sizes
- com.oplus.full.bining.qbc.enable
- com.oplus.preview.capture.turboraw.cnt
- KEY_FULL_BINING_QBC_ENABLE
- KEY_LIMITED_QBC
- KEY_IS_TURBORAW_CAPTURE
- getSupportTurboRawResolutionEnhance
- isTurboRawResolutionEnhanceCapture
- setTurborawCaptureInfo

Therefore the correct migration is to connect OPCameraPro to Camera 7.013.30's existing
HighPixel/APS path, not to fabricate a parallel 25MP mode.

## Next implementation order

1. Test3: clean caller-side Protobuf isolation + legacy MasterParamFix off.
2. Observe all CameraUnit/IPU config names and CameraUnit supported mode/type inventory
   while switching into HighPixel.
3. Resolve the ColorOS 17 replacement for the old `camera_unit_config` lookup.
4. Map the APS sensor-mode object feeding remosaic.
5. Only then re-enable/redirect Camera25MpDecision when capability evidence is complete.
