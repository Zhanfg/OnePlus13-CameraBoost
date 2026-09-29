#!/system/bin/sh
set -eu

OUT="${1:-/sdcard/CameraBoost-snapshot}"
mkdir -p "$OUT"

safe_run() {
  name="$1"
  shift
  {
    echo "# command: $*"
    "$@"
  } >"$OUT/$name.txt" 2>&1 || true
}

safe_run getprop getprop
safe_run uname uname -a
safe_run camera_service dumpsys media.camera
safe_run packages sh -c 'pm list packages -f | grep -Ei "camera|oplus|oneplus|gallery|photos"'
safe_run camera_modules sh -c 'cat /proc/modules | grep -Ei "camera|cam_|mmrm|video|ois|actuator"'
safe_run mounts sh -c 'mount | grep -Ei "vendor|odm|camera"'
safe_run proc_cmdline cat /proc/cmdline

{
  echo "# camera-related vendor files: path + sha256 when readable"
  for base in /vendor /odm /system_ext; do
    [ -d "$base" ] || continue
    find "$base" -type f \(       -iname '*camera*' -o -iname '*camx*' -o -iname '*chi*' -o       -iname '*chromatix*' -o -iname '*aps*' -o -iname '*ois*'     \) 2>/dev/null | head -n 400 | while read -r f; do
      if command -v sha256sum >/dev/null 2>&1 && [ -r "$f" ]; then
        sha256sum "$f" 2>/dev/null || echo "$f"
      else
        echo "$f"
      fi
    done
  done
} >"$OUT/vendor_camera_inventory.txt" 2>&1 || true

cat >"$OUT/README.txt" <<'EOF'
CameraBoost metadata snapshot.
This collector intentionally does NOT copy proprietary camera libraries,
calibration data, encrypted/decrypted APS configs, or user photos.
EOF

echo "CameraBoost snapshot written to: $OUT"
