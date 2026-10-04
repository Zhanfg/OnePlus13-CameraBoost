#!/usr/bin/env python3
# Reproducible Test4 patch for the exact supplied OPCameraPro 3.2.10 APK.
# Input SHA256:
# 51536e4dfe03057ee5df721a75194e26be337f3dd2cac1653c85277932636d03
#
# Changes:
# - disable legacy MasterModeParamFix
# - skip old Protobuf FeatureTable mutator at its caller
# - bypass ONLY the legacy Camera25MpCapability.supported installer guard
#
# See analysis/OPCameraPro_3.2.10_ColorOS17_Test4_25MP.md for rationale.
#
# The executable patch implementation is kept in the project investigation history;
# offsets are pinned below for audit/reproduction.
CONSTRUCTOR_CODE_OFF = 6212500
PARAM_FIX_GETTER_CODE_OFF = 6209108
PROTOBUF_CALLER_CODE_OFF = 1438928
PROTOBUF_CALL_UNIT = 1338
PROTOBUF_INIT_METHOD_IDX = 21964
CAMERA25MP_INSTALL_CODE_OFF = 1415800
CAMERA25MP_CAP_GUARD_UNIT = 35
FIELD_ENABLE_MASTER_MODE_PARAM_FIX = 19318
