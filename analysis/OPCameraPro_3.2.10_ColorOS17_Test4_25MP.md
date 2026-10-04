# OPCameraPro 3.2.10 ColorOS 17 — Test4 25MP capability bridge

Target:
- OnePlus 13 / PJZ110
- Android 17 / ColorOS 17
- Camera 7.013.30
- OPCameraPro 3.2.10

## Evidence from HighPixel APS Audit v2

The device's stock ColorOS 17 camera configuration shows:
- /odm/etc/camera/config/camera_unit_config exists.
- rear_main explicitly lists high_pixel_mode.
- high_pixel_mode has aiHighPixel.
- ai_high_pixel_case is present.
- APS_CAPMODE_AI_HIGH_PIXEL is present.
- CameraHWConfiguration.config contains QBC operation modes.
- runtime APS reports com.oplus.sensor.mode.list.result with curSensorMode=3.
- no pApsSensorMode NULL was observed in this run.

Therefore the hardware/APS path is not generically unsupported.

## Why OPCameraPro still refuses 25MP

OPCameraPro 3.2.10 builds Camera25MpCapability using legacy file-based evidence:
- config/oplus_camera_config
- CameraHWConfiguration.config
- config/oplus_camera_preview_decision_config.json
- old VendorTags such as com.oplus.turboraw.re.support and qbc output sizes
- old libarcsoft_turbo_raw.so paths

The stock ColorOS 17 oplus_camera_config does not contain the old injected
TurboRAW/QBC VendorTags before OPCameraPro's runtime config mutation, so the snapshot can
report unsupported even though CameraUnit/high_pixel/QBC/sensor-mode support exists.

Camera25MpDecision then emits:
  Skip runtime decision redirect: module capability snapshot

## Test4 patch

Test4 keeps Test3's two compatibility fixes:
1. legacy MasterModeParamFix disabled;
2. old Protobuf FeatureTable mutator not installed.

Additionally, it NOPs only the single Camera25MpDecision installer branch that rejects
installation when the legacy Camera25MpCapability.supported field is false.

It does NOT:
- force sensor mode;
- modify HAL;
- force remosaic results;
- force downstream Camera25MpDecision results;
- modify RAWMAX/JPGMAX;
- restore the old Protobuf mode-table mutator.

All downstream 25MP runtime logic remains unchanged.
