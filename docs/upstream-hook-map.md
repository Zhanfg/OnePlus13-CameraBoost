# Upstream Camera Hook Map

This file records **observed public symbols and behavior**, not copied upstream implementation.

Pinned OPCameraPro commit: `1fe83082dfbe176f723437018ee9f89f3478c2b3`.

## APS/config interception

Observed target:

- class: `com.oplus.ocs.camera.consumer.apsAdapter.update.UpdateHelper`
- method: `getValidConfigData`
- behavior: the upstream module observes the resolved OPlus camera configuration and can substitute a modified configuration.

Research value:

- identify which OPlus camera JSON/config file is active on OnePlus 13;
- collect the original capability surface before applying any unlock;
- feed decrypted/exported JSON into CameraBoost's clean-room diff engine.

## 120 fps feature gate

Observed target:

- class: `com.oplus.ocs.camera.configure.ConfigFeatureImpl`
- method: `isFeatureValueLegal`
- feature key: `com.oplus.configure.video.fps`
- value: `video_120fps`

This is evidence of an **app/config legality gate only**. It is not evidence that sensor readout, ISP, encoder, thermal policy or bandwidth can sustain 120 fps.

## Protobuf feature table

Observed target:

- class: `com.oplus.ocs.camera.configure.ProtobufFeatureConfig$FeatureTable`
- method: `parseFrom`

Observed feature names include:

- `com.oplus.camera.feature.filter`
- `com.oplus.camera.feature.live_photo`
- `com.oplus.preview.flash.mode`

Observed mode/camera identifiers include:

- `professional_mode`
- `portrait_mode`
- `rear_main`
- `rear_wide`
- `rear_tele`
- `rear_sat`
- `rear_portrait`

These names are useful probes for mapping OPlus Camera's feature graph.

## Preview HDR

The current upstream implementation locates a class by the string `DATASPACE_DISPLAY_P3_HLG`, which suggests a useful signature for discovering the preview-HDR path across OPlus Camera versions.

## Research rule

Treat every LSPosed hook as a **probe into a decision layer**, not as proof of hardware support.

The target validation chain remains:

`discovered -> advertised -> callable -> streaming -> stable -> validated`.
