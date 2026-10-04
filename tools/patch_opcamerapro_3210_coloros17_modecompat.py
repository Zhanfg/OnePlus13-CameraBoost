#!/usr/bin/env python3
"""
OPCameraPro 3.2.10 -> Camera 7.013.30 / ColorOS 17 ModeCompat Test3.

Pinned to the exact user-supplied APK SHA-256. This patch does NOT disable
25MP, MasterMode, RAWMAX or JPGMAX. It isolates only two legacy compatibility
layers proven unsafe on Camera 7.013.30:

  1. enableMasterModeParamFix is forced false.
  2. the unique call that installs OPCameraPro's legacy Protobuf FeatureTable
     mutation hook is replaced with NOPs. The Protobuf hook implementation
     itself is left byte-for-byte intact, avoiding ART verifier failures.

This supersedes Test1/Test2.
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
PARAM_FIX_GETTER_CODE_OFF = 6209108
PROTOBUF_CALLER_CODE_OFF = 1438928

FIELD_ENABLE_MASTER_MODE_PARAM_FIX = 19318
PROTOBUF_INIT_METHOD_IDX = 21964
PROTOBUF_CALL_UNIT = 1338


def unit(data: bytearray, code_off: int, index: int) -> int:
    return struct.unpack_from("<H", data, code_off + 16 + index * 2)[0]


def set_unit(data: bytearray, code_off: int, index: int, value: int) -> None:
    struct.pack_into("<H", data, code_off + 16 + index * 2, value)


def patch_dex(data: bytes) -> bytes:
    dex = bytearray(data)

    # Strict layout validation.
    assert unit(dex, CONSTRUCTOR_CODE_OFF, 407) & 0xFF == 0x5C  # iput-boolean
    assert unit(dex, CONSTRUCTOR_CODE_OFF, 408) == FIELD_ENABLE_MASTER_MODE_PARAM_FIX
    assert unit(dex, PARAM_FIX_GETTER_CODE_OFF, 0) & 0xFF == 0x55  # iget-boolean
    assert unit(dex, PARAM_FIX_GETTER_CODE_OFF, 1) == FIELD_ENABLE_MASTER_MODE_PARAM_FIX

    # Unique ProtobufFeature installer call:
    # invoke-virtual {...}, method@21964, occupying three 16-bit code units.
    assert unit(dex, PROTOBUF_CALLER_CODE_OFF, PROTOBUF_CALL_UNIT) & 0xFF == 0x6E
    assert unit(dex, PROTOBUF_CALLER_CODE_OFF, PROTOBUF_CALL_UNIT + 1) == PROTOBUF_INIT_METHOD_IDX

    # 1) Disable only the legacy Master parameter-bar fix.
    set_unit(dex, CONSTRUCTOR_CODE_OFF, 407, 0x0000)
    set_unit(dex, CONSTRUCTOR_CODE_OFF, 408, 0x0000)

    # Defensive getter override: const/4 v0,#0 ; return v0 ; nop.
    set_unit(dex, PARAM_FIX_GETTER_CODE_OFF, 0, 0x0012)
    set_unit(dex, PARAM_FIX_GETTER_CODE_OFF, 1, 0x000F)
    set_unit(dex, PARAM_FIX_GETTER_CODE_OFF, 2, 0x0000)

    # 2) Skip the caller-side Protobuf hook installation.
    # Do not mutate ProtobufFeature.init() itself; Test2 proved that replacing
    # its first code unit caused ART VerifyError ("unexpected opcode unused-41").
    set_unit(dex, PROTOBUF_CALLER_CODE_OFF, PROTOBUF_CALL_UNIT, 0x0000)
    set_unit(dex, PROTOBUF_CALLER_CODE_OFF, PROTOBUF_CALL_UNIT + 1, 0x0000)
    set_unit(dex, PROTOBUF_CALLER_CODE_OFF, PROTOBUF_CALL_UNIT + 2, 0x0000)

    # DEX signature/checksum.
    dex[12:32] = hashlib.sha1(dex[32:]).digest()
    struct.pack_into("<I", dex, 8, zlib.adler32(dex[12:]) & 0xFFFFFFFF)
    return bytes(dex)


def main() -> None:
    src = Path(sys.argv[1] if len(sys.argv) > 1 else "OPCameraPro_v3.1.20.apk")
    dst = Path(sys.argv[2] if len(sys.argv) > 2
               else "OPCameraPro-3.2.10-ColorOS17-ModeCompatTest3-unsigned.apk")

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
