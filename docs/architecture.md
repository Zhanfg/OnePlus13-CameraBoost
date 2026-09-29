# Architecture

## Layer model

```text
OPlus Camera APK / LSPosed hooks
        ↓
Camera2 / OPlus framework
        ↓
Camera HAL / VendorTags / CamX / CHI
        ↓
APS decision/configuration layer
        ↓
ISP / DSP / encoder
        ↓
camera-kernel / MMRM / ICC / IOMMU
        ↓
sensor / OIS / actuator / EEPROM
```

CameraBoost deliberately keeps these layers separate.

## Safety gate

Every feature must move through:

1. **discovered** — found in app/config/reference device
2. **advertised** — exposed by framework/HAL
3. **callable** — app can enter the mode without immediate crash
4. **streaming** — preview/capture/video pipeline starts
5. **stable** — sustained test passes
6. **validated** — output metadata and quality are consistent

Never jump from `discovered` directly to `validated`.

## Initial workstreams

### A. APS diff
Compare decrypted JSON from target and donor devices and classify HDR/MFNR/RAW/SR/night/fusion, SAT/zoom routing, frame counts, thresholds, video mode gates and sensor-dependent blocks.

### B. Capability snapshot
Collect build/model/platform, camera service diagnostics, package versions, kernel modules and selected readable kernel metadata.

### C. LSPosed compatibility
Implement only after the capability model is stable. Hooks should enable a feature only if runtime probes and the device profile allow it.

### D. Kernel work
Keep qcom camera-kernel, OPlus camera kernel extensions, MMRM, interconnect, scheduler and thermal work on a separate branch.
