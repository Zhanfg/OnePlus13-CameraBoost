# OnePlus 13 official OSS camera surface map

This document maps the P0 CameraBoost workstreams onto the official OnePlus 13 SM8750
kernel/module source release.

Official source:

- repository: `OnePlusOSS/android_kernel_modules_and_devicetree_oneplus_sm8750`
- branch: `oneplus/sm8750_b_16.0.0_oneplus_13`
- base Qualcomm target: `sun`
- device codename/overlay: `dodge`

## What the official source actually exposes

### `camera.ko`

Defined by:

`vendor/qcom/opensource/camera-kernel/camera_modules.bzl`

The module includes:

- camera request and memory managers;
- SMMU and synchronization;
- CPAS plus interconnect voting;
- Spectra ISP (IFE/SFE/VFE);
- ICP/IPE/BPS/OFE;
- JPEG encoder/DMA;
- sensor, CCI, CSIPHY, actuator, EEPROM, OIS and flash;
- OPlus-specific camera link extensions.

For the `sun` target its build dependencies explicitly include Qualcomm's MMRM driver,
Synx, SMMU proxy and secure invocation support.

### `camera_extension.ko`

Defined by:

`vendor/oplus/kernel/camera/camera_extension_modules.bzl`

It contains OPlus customizations for sensor I/O, sensor behavior, actuator, EEPROM, OIS
and OIS firmware, camera monitoring/debug and ToF extension drivers.

### Camera device tree

The OnePlus build script explicitly includes:

`vendor/qcom/opensource/camera-devicetree`

and the device build uses the `dodge-23821-sun-overlay.dtbo` family over the Qualcomm
`sun` platform.

## What this means for our P0 features

### 10-bit HEIF / full-path 10-bit

**Start above the kernel.**

The official camera kernel contains a JPEG block, but the 10-bit HEIF capability is a
Qualcomm platform/HAL/encoder capability rather than an obvious `camera.ko` switch.
Investigate OPlus Camera configuration, Camera HAL/VendorTags, HEIF encoder selection and
color metadata first.

Only descend into CPAS/ICC/SMMU if a real 10-bit stream fails because of resource or
buffering constraints.

### 4K120 / Pro Video / LOG

Start with sensor mode exposure, HAL/CamX/CHI and encoder configuration.

The kernel becomes relevant only when:

- a valid sensor mode cannot stream;
- CSI/ISP/ICP resources fail;
- CPAS/interconnect bandwidth is insufficient;
- MMRM denies or down-votes the multimedia request;
- sustained recording hits a resource/thermal stability limit.

### RAW / RAW Max

Start with public/vendor RAW stream exposure, APS RAW fusion and DNG metadata/output.
If a genuine RAW stream itself is missing, then inspect the sensor/RDI/ISP and memory path.

### Master/Hasselblad controls

These are primarily Camera app + HAL request + APS configuration features.
Changing MMRM, DT or OIS firmware is not justified unless a specific physical control path
is proven missing.

### Telephoto/HDR

Start with SAT/zoom routing, APS fusion, calibration and multi-exposure algorithms.
ISP/ICP, interconnect and MMRM become relevant when the algorithm is already selected but
cannot stream or sustain the requested workload.

## Architectural rule

A kernel patch must answer a diagnosed lower-layer failure.

Do **not** add a kernel patch merely because an OPlus Camera menu is missing.
