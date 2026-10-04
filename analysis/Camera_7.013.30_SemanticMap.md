# Camera 7.013.30 Semantic Map

Generated from the user-supplied Camera 7.013.30 APK and cross-checked against Camera 6.070.228.

This is a semantic map, not original R8/ProGuard name recovery. Runtime class names are intentionally left unchanged because Camera uses reflection, JNI and plugin contracts.

## High-confidence mode mapping

| 7.013.30 class | Semantic name | 6.070.228 match |
|---|---|---|
| `com.oplus.camera.module.a` | CommonVideoMode | `com.oplus.camera.module.a` |
| `gm.e3` | MacroMode | `fj.n3` |
| `gm.q2` | CommonCapMode | `fj.a3` |
| `im.c1` | PortraitCapMode | `hj.e1` |
| `im.d3` | TimeLapseProMode | `hj.f3` |
| `im.e0` | HighPixelMode | `hj.z` |
| `im.h0` | LongExposureMode | `hj.d0` |
| `im.h1` | ProfessionalCapMode | `hj.l1` |
| `im.h3` | UnderWaterMode | `hj.j3` |
| `im.i2` | StickerMode | `hj.k2` |
| `im.j0` | MasterCapMode | `hj.l0` |
| `im.j1` | QuickCaptureMode | `hj.p1` |
| `im.k0` | MicroscopePhotoMode | `hj.m0` |
| `im.k` | AISceneryMode | `hj.e` |
| `im.l` | FineFoodMode | `hj.f` |
| `im.m3` | XPANMode | `hj.p3` |
| `im.p1` | RetroCameraMode | new/changed |
| `im.p` | GRCapMode | `hj.i` |
| `im.q0` | NightMode | `hj.q0` |
| `im.r0` | PanoramaCapMode | `hj.s0` |
| `im.s2` | StreetMode | `hj.s2` |
| `im.u2` | SuperTextMode | `hj.u2` |
| `im.w2` | TiltShiftPhotoMode | `hj.x2` |
| `im.x` | HSProfessionalCapMode | `hj.s` |
| `im.y` | HighDefinitionMode | `hj.t` |
| `rm.c` | FastVideoMode | `sj.c` |
| `rm.d` | MicroscopeVideoMode | `sj.d` |
| `rm.f` | FilmVideoMode | `sj.g` |
| `rm.h` | QuickVideoMode | `sj.i` |
| `rm.m` | SlowVideoMode | `sj.m` |
| `rm.p` | TiltShiftFastVideoMode | `sj.s` |
| `rm.q0` | VideoMode base | `sj.n0` |
| `rm.q` | UnderWaterVideoMode | `sj.t` |
| `sm.b` | MasterVideoMode | `tj.c` |

## CaptureParam UI mapping

| Class | Semantic role |
|---|---|
| `df.r1` | UProParamListBar |
| `df.s1` | UProParam item View created/reused by UProParamListBar |
| `if.k0` | Item View expected by Camera 7.013.30 ListModeBarAdapter |
| `com.oplus.camera.feature.captureparam.ui.adapter.ListModeBarAdapter` | CaptureParam list adapter |
| `com.oplus.camera.feature.captureparam.view.CaptureParamView` | Professional/Master parameter host |

## Important crash finding

The observed crash:

```
java.lang.ClassCastException:
df.s1 cannot be cast to if.k0
```

is a **View recycling / adapter-style mismatch**, not a RAW/JPG data-model cast.

Camera 7.013.30's `ListModeBarAdapter` performs check-casts to `if.k0`. The UPro parameter bar `df.r1` creates/reuses `df.s1` item views. If a hook causes the wrong Professional/Master UI style or adapter path to be active, Android can recycle a `df.s1` where the newer adapter expects `if.k0`, producing the exact crash.

Therefore:
- do not globally force Master UI/version/style predicates;
- do not mix old UPro and new CaptureParam adapter branches;
- isolate OPCameraPro's `enableMasterModeParamFix` and any Protobuf/feature-table changes affecting `professional_mode`;
- keep JPGMAX/RAWMAX capture-path work separate from CaptureParam UI selection.

## OPCameraPro 3.2.10 evidence

The supplied OPCameraPro 3.2.10 DEX contains an explicit `enableMasterModeParamFix` setting alongside `enableMasterMode` and `enableMasterModeLivePhoto`.

Its public-source Protobuf hook also modifies the `professional_mode` feature table to inject filter/live-photo entries. These paths need ColorOS 17 / Camera 7.013.30-specific validation before being enabled.
