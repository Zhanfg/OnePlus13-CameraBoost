#!/system/bin/sh
# CameraBoost ColorOS 17 crash probe
# Standalone: no command-line arguments required.

SELF="$(readlink -f "$0" 2>/dev/null)"
[ -n "$SELF" ] || SELF="$0"

if [ "$(id -u 2>/dev/null)" != "0" ]; then
    echo "[*] Requesting root..."
    exec su -c "sh '$SELF'"
fi

TS="$(date +%Y%m%d_%H%M%S)"
OUT="/sdcard/Download/CameraBoost_CrashProbe_${TS}"
mkdir -p "$OUT" || exit 1
SUMMARY="$OUT/summary.txt"
LIVE="$OUT/logcat_live.txt"

say() { echo "$*" | tee -a "$SUMMARY"; }

cleanup() {
    if [ -n "$LOGCAT_PID" ]; then
        kill "$LOGCAT_PID" 2>/dev/null
        wait "$LOGCAT_PID" 2>/dev/null
    fi
}
trap cleanup EXIT INT TERM

say "============================================================"
say " CameraBoost ColorOS 17 Crash Probe"
say " Time: $(date)"
say " Output: $OUT"
say "============================================================"

{
    echo "=== Device ==="
    echo "model=$(getprop ro.product.model)"
    echo "device=$(getprop ro.product.device)"
    echo "android=$(getprop ro.build.version.release)"
    echo "sdk=$(getprop ro.build.version.sdk)"
    echo "display=$(getprop ro.build.display.id)"
    echo "fingerprint=$(getprop ro.build.fingerprint)"
    echo "kernel=$(uname -a)"
    echo "selinux=$(getenforce 2>/dev/null)"
    echo
    echo "=== Packages ==="
    for p in \
        com.oplus.camera \
        com.tlsu.opluscamerapro \
        dev.cameraboost.oplus10bit.coloros17all \
        dev.cameraboost.oplus10bit.coloros17 \
        com.coloros.gallery3d \
        com.coloros.ocrscanner
    do
        echo "--- $p ---"
        pm path "$p" 2>&1
        dumpsys package "$p" 2>/dev/null | grep -E \
          'versionName=|versionCode=|codePath=|firstInstallTime=|lastUpdateTime=|pkgFlags=|enabled=' | head -n 30
    done
} > "$OUT/device_and_packages.txt" 2>&1

say "[1/8] Clearing old logcat buffers"
logcat -c >/dev/null 2>&1

say "[2/8] Starting live log capture"
logcat -v threadtime -b main -b system -b crash -b events > "$LIVE" 2>&1 &
LOGCAT_PID=$!
sleep 1

say "[3/8] Force-stopping Camera and reproducing launch"
am force-stop com.oplus.camera >/dev/null 2>&1
sleep 1
{
    echo "=== Launch output ==="
    monkey -p com.oplus.camera -c android.intent.category.LAUNCHER 1 2>&1
    echo
    echo "=== Explicit camera intent fallback ==="
    am start -a android.media.action.STILL_IMAGE_CAMERA -p com.oplus.camera 2>&1
} > "$OUT/launch.txt"

sleep 8
CAM_PID="$(pidof com.oplus.camera 2>/dev/null)"
if [ -n "$CAM_PID" ]; then
    say "Camera process after 8s: ALIVE pid=$CAM_PID"
else
    say "Camera process after 8s: NOT RUNNING"
fi

say "[4/8] Finalizing log capture"
cleanup
LOGCAT_PID=""

logcat -d -v threadtime -b crash > "$OUT/logcat_crash.txt" 2>&1
logcat -d -v threadtime -b main -b system | grep -Ei \
  'CameraBoost17|CameraBoostCompat17|CameraBoost|OPCameraPro|opluscamerapro|LSPosed|Xposed|com\.oplus\.camera|AndroidRuntime|FATAL EXCEPTION|OutOfMemoryError|SIGABRT|SIGSEGV|Fatal signal|ANR|Watchdog|ClassNotFound|NoSuchMethod|NoSuchField|UnsatisfiedLink|dlopen|SecurityException|VerifyError|LinkageError' \
  > "$OUT/logcat_filtered.txt" 2>&1

say "[5/8] Collecting ActivityManager exit history"
dumpsys activity exit-info com.oplus.camera > "$OUT/activity_exit_info.txt" 2>&1
dumpsys activity processes > "$OUT/activity_processes.txt" 2>&1
dumpsys meminfo com.oplus.camera > "$OUT/camera_meminfo.txt" 2>&1

say "[6/8] Collecting LSPosed/Xposed logs"
mkdir -p "$OUT/lsposed"
for d in /data/adb/lspd/log /data/adb/lspd/log/verbose /data/adb/lsposed/log; do
    [ -d "$d" ] || continue
    for f in $(ls -t "$d"/* 2>/dev/null | head -n 8); do
        [ -f "$f" ] || continue
        tail -n 3000 "$f" > "$OUT/lsposed/$(basename "$f")" 2>/dev/null
    done
done

say "[7/8] Collecting recent tombstones and ANR traces"
mkdir -p "$OUT/tombstones" "$OUT/anr"
for f in $(ls -t /data/tombstones/tombstone_* 2>/dev/null | head -n 6); do
    [ -f "$f" ] && cp -p "$f" "$OUT/tombstones/" 2>/dev/null
done
for f in $(ls -t /data/anr/* 2>/dev/null | head -n 6); do
    [ -f "$f" ] && cp -p "$f" "$OUT/anr/" 2>/dev/null
done

say "[8/8] Building quick verdict"
ALL="$OUT/_all_relevant.txt"
cat "$OUT/logcat_crash.txt" "$OUT/logcat_filtered.txt" "$OUT/activity_exit_info.txt" "$OUT"/lsposed/* 2>/dev/null > "$ALL"

{
    echo
    echo "=== Quick verdict ==="
    grep -q 'OutOfMemoryError' "$ALL" && echo "[!] OutOfMemoryError detected."
    grep -Eq 'Fatal signal|SIGABRT|SIGSEGV' "$ALL" && echo "[!] Native fatal signal detected."
    grep -Eq 'FATAL EXCEPTION|AndroidRuntime' "$ALL" && echo "[!] Java fatal exception detected."
    grep -Eq 'ANR|Watchdog' "$ALL" && echo "[!] ANR/watchdog evidence detected."
    echo
    echo "--- CameraBoost stage markers ---"
    grep -E 'CameraBoost17.*stage=' "$ALL" | tail -n 80
    LAST_STAGE="$(grep -Eo 'stage=[A-Z0-9_]+' "$ALL" | tail -n 1)"
    echo
    if [ -n "$LAST_STAGE" ]; then
        echo "Last CameraBoost stage: $LAST_STAGE"
    else
        echo "No CameraBoost stage marker found."
    fi
    echo
    echo "--- Top crash lines ---"
    grep -Ei \
      'FATAL EXCEPTION|Fatal signal|OutOfMemoryError|SIGABRT|SIGSEGV|VerifyError|LinkageError|NoSuchMethod|NoSuchField|ClassNotFound|UnsatisfiedLink|SecurityException|ANR|Watchdog' \
      "$ALL" | tail -n 120
} >> "$SUMMARY"

rm -f "$ALL"
ARCHIVE="${OUT}.tar.gz"
tar -czf "$ARCHIVE" -C "$(dirname "$OUT")" "$(basename "$OUT")" 2>/dev/null

echo
echo "============================================================"
echo "Done"
echo "Summary : $SUMMARY"
echo "Archive : $ARCHIVE"
echo "============================================================"
cat "$SUMMARY"
