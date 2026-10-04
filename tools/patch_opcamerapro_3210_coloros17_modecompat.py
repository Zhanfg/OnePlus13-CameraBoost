#!/usr/bin/env python3
"""
Reproducible binary compatibility patch for the exact OPCameraPro 3.2.10 APK
supplied during the ColorOS 17 / Camera 7.013.30 investigation.

This deliberately does NOT touch JPGMAX, RAWMAX, 25MP or their runtime hooks.
It only isolates three legacy mode/UI compatibility behaviors:
  1) enableMasterMode -> false (prevents master.mode.version=2.0 injection)
  2) enableMasterModeParamFix -> false
  3) ProtobufFeature hook installer -> no-op

The patch is intentionally pinned to the input SHA-256 and known DEX layout.
"""
from __future__ import annotations

import hashlib
import struct
import sys
import zipfile
import zlib
from pathlib import Path

EXPECTED_SHA256 = "51536e4dfe03057ee5df721a75194e26be337f3dd2cac1653c85277932636d03"

CONSTRUCTOR_CODE_OFF = 6212500
PROTOBUF_INIT_CODE_OFF = 5302048
PARAM_FIX_GETTER_CODE_OFF = 6209108

FIELD_ENABLE_MASTER_MODE = 19230
FIELD_ENABLE_MASTER_MODE_PARAM_FIX = 19318


def unit(data: bytearray, code_off: int, index: int) -> int:
    return struct.unpack_from("<H", data, code_off + 16 + index * 2)[0]


def set_unit(data: bytearray, code_off: int, index: int, value: int) -> None:
    struct.pack_into("<H", data, code_off + 16 + index * 2, value)


def patch_dex(data: bytes) -> bytes:
    dex = bytearray(data)

    # Validate the exact DEX layout before mutating anything.
    assert unit(dex, CONSTRUCTOR_CODE_OFF, 69) & 0xFF == 0x5C
    assert unit(dex, CONSTRUCTOR_CODE_OFF, 70) == FIELD_ENABLE_MASTER_MODE
    assert unit(dex, CONSTRUCTOR_CODE_OFF, 407) & 0xFF == 0x5C
    assert unit(dex, CONSTRUCTOR_CODE_OFF, 408) == FIELD_ENABLE_MASTER_MODE_PARAM_FIX
    assert unit(dex, PARAM_FIX_GETTER_CODE_OFF, 0) & 0xFF == 0x55
    assert unit(dex, PARAM_FIX_GETTER_CODE_OFF, 1) == FIELD_ENABLE_MASTER_MODE_PARAM_FIX

    # Do not assign enableMasterMode=true into VendorTagSettings.
    set_unit(dex, CONSTRUCTOR_CODE_OFF, 69, 0x0000)
    set_unit(dex, CONSTRUCTOR_CODE_OFF, 70, 0x0000)

    # Do not assign enableMasterModeParamFix=true.
    set_unit(dex, CONSTRUCTOR_CODE_OFF, 407, 0x0000)
    set_unit(dex, CONSTRUCTOR_CODE_OFF, 408, 0x0000)

    # Defensive getter override: const/4 v0,#0; return v0; nop
    set_unit(dex, PARAM_FIX_GETTER_CODE_OFF, 0, 0x0012)
    set_unit(dex, PARAM_FIX_GETTER_CODE_OFF, 1, 0x000F)
    set_unit(dex, PARAM_FIX_GETTER_CODE_OFF, 2, 0x0000)

    # ProtobufFeature.init(): return-void before installing parseFrom hooks.
    set_unit(dex, PROTOBUF_INIT_CODE_OFF, 0, 0x000E)

    # DEX signature/checksum.
    dex[12:32] = hashlib.sha1(dex[32:]).digest()
    struct.pack_into("<I", dex, 8, zlib.adler32(dex[12:]) & 0xFFFFFFFF)
    return bytes(dex)


def main() -> None:
    src = Path(sys.argv[1] if len(sys.argv) > 1 else "OPCameraPro_v3.1.20.apk")
    dst = Path(sys.argv[2] if len(sys.argv) > 2
               else "OPCameraPro-3.2.10-ColorOS17-ModeCompatTest-unsigned.apk")

    digest = hashlib.sha256(src.read_bytes()).hexdigest()
    if digest != EXPECTED_SHA256:
        raise SystemExit(f"Refusing unknown input APK: {digest}")

    with zipfile.ZipFile(src, "r") as zin:
        patched = patch_dex(zin.read("classes.dex"))
        with zipfile.ZipFile(dst, "w", allowZip64=True) as zout:
            for info in zin.infolist():
                upper = info.filename.upper()
                if upper.startswith("META-INF/") and (
                    upper.endswith(".RSA") or upper.endswith(".DSA")
                    or upper.endswith(".EC") or upper.endswith(".SF")
                    or upper == "META-INF/MANIFEST.MF"
                ):
                    continue

                payload = patched if info.filename == "classes.dex" else zin.read(info.filename)
                copy = zipfile.ZipInfo(info.filename, date_time=info.date_time)
                copy.compress_type = info.compress_type
                copy.comment = info.comment
                copy.extra = info.extra
                copy.internal_attr = info.internal_attr
                copy.external_attr = info.external_attr
                copy.create_system = info.create_system
                copy.flag_bits = info.flag_bits
                zout.writestr(copy, payload)

    print(dst)
    print("sha256", hashlib.sha256(dst.read_bytes()).hexdigest())


if __name__ == "__main__":
    main()
