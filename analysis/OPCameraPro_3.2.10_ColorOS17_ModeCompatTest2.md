# OPCameraPro 3.2.10 ColorOS 17 ModeCompat Test2

## Test1 result

On Camera 7.013.30:
- Master / Professional and other native modes recovered substantially.
- mode switching still has residual issues.
- direct-output 25 MP disappeared.

## Important field-map correction

The initial Test1 analysis was off by one field around the beginning of
`VendorTagSettings`.

Correct sequence from the 3.2.10 DEX `toString()` implementation:

- 19227 = enableCameraPalette
- 19228 = retainCameraPaletteMetadata
- 19229 = enableCameraPaletteAppleStyle
- **19230 = enable25MP**
- 19231 = enableMasterMode
- 19232 = enableMasterRawMax
- 19233 = enableMasterJpgMax

Therefore Test1 accidentally disabled **enable25MP**, not enableMasterMode.

The private 3.2.10 config path guarded by field 19230 decrypts to:
- `com.oplus.turboraw.re.support = 1`
- `com.oplus.turboraw.re.open.bydefault = 1`
- `com.oplus.main.full.qbc.output.sizes = ...`
and then prepares additional 25MP/QBC evidence.

This directly explains the lost 25MP feature in Test1.

## Test2 delta

Start again from the original supplied 3.2.10 APK and change only:

1. `enableMasterModeParamFix` -> false.
2. Protobuf FeatureTable mutation installer -> no-op.

Test2 does **not** modify:
- enable25MP,
- enableMasterMode,
- RAWMAX,
- JPGMAX,
- Camera25MpDecisionNative,
- APS/Pseudo-UHDR runtime logic.

The goal is to retain the mode-table stability improvement from Test1 while restoring
the full 25MP configuration and runtime path.

## Next diagnostic target

Residual mode-switch problems should be captured with the dedicated
`OPCameraPro_ModeSwitchProbe_v1.sh`, then isolated from remaining global camera config
hooks (especially UpdateHelper/getValidConfigData) without blanket-disabling premium
features.
