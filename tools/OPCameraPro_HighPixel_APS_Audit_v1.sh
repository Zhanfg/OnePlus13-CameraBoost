#!/system/bin/sh
# OnePlus 13 / ColorOS 17 HighPixel + APS + CameraUnit audit
# No arguments required. Read-only collection except logcat clear and camera restart.

SELF="$(readlink -f "$0" 2>/dev/null)"
[ -n "$SELF" ] || SELF="$0"

if [ "$(id -u 2>/dev/null)" != "0" ]; then
    exec su -c "sh '$SELF'"
fi

TS="$(date +%Y%m%d_%H%M%S)"
OUT="/sdcard/Download/OPCameraPro_HighPixel_APS_Audit_${TS}"
mkdir -p "$OUT"/{configs,framework,logs} || exit 1
SUMMARY="$OUT/summary.txt"
LIVE="$OUT/logs/logcat_live.txt"
LOGCAT_PID=""

say() { echo "$*" | tee -a "$SUMMARY"; }
cleanup() {
    if [ -n "$LOGCAT_PID" ]; then
        kill "$LOGCAT_PID" 2>/dev/null
        wait "$LOGCAT_PID" 2>/dev/null
        LOGCAT_PID=""
    fi
}
trap cleanup EXIT INT TERM

say "OPCameraPro HighPixel / APS / CameraUnit Audit"
say "Time: $(date)"
say "Output: $OUT"

{
    echo "model=$(getprop ro.product.model)"
    echo "device=$(getprop ro.product.device)"
    echo "android=$(getprop ro.build.version.release)"
    echo "sdk=$(getprop ro.build.version.sdk)"
    echo "display=$(getprop ro.build.display.id)"
    echo "fingerprint=$(getprop ro.build.fingerprint)"
    echo "kernel=$(uname -a)"
    for p in com.oplus.camera com.tlsu.opluscamerapro; do
        echo "--- $p ---"
        pm path "$p" 2>&1
        dumpsys package "$p" 2>/dev/null | grep -E 'versionName=|versionCode=|codePath=|lastUpdateTime=|enabled=' | head -n 30
    done
} > "$OUT/device_packages.txt" 2>&1

{
    find /product/framework /system_ext/framework /vendor/framework -maxdepth 2 -type f 2>/dev/null |
      grep -Ei 'camera.*unit|ocs.*camera|ipu|aps' | sort
    find /odm/etc/camera /vendor/etc/camera /product/etc/camera -maxdepth 4 -type f 2>/dev/null |
      grep -Ei 'aps|algo|preview|camera.*config|sensor|qbc|turbo|remosaic|unit' | sort
} > "$OUT/file_inventory.txt"

for f in \
  /product/framework/com.oplus.camera.unit.sdk.jar \
  /product/framework/com.oplus.camera.unit.sdk.adapter.jar \
  /odm/etc/camera/config/oplus_camera_aps_config \
  /odm/etc/camera/config/oplus_camera_preview_decision_config.json \
  /odm/etc/camera/config/oplus_camera_algo_switch_config \
  /odm/etc/camera/config/oplus_camera_config \
  /vendor/etc/camera/config/oplus_camera_aps_config \
  /vendor/etc/camera/config/oplus_camera_preview_decision_config.json \
  /vendor/etc/camera/config/oplus_camera_algo_switch_config
do
    [ -f "$f" ] || continue
    case "$f" in
        *.jar) cp -p "$f" "$OUT/framework/" 2>/dev/null ;;
        *) cp -p "$f" "$OUT/configs/" 2>/dev/null ;;
    esac
done

{
    for d in /odm/etc/camera /vendor/etc/camera /product/etc/camera; do
        [ -d "$d" ] || continue
        find "$d" -maxdepth 4 -type f -size -16M 2>/dev/null | while IFS= read -r f; do
            hits="$(grep -aEin -m 80 'turboraw|qbc|remosaic|sensor[_ .-]*mode|high[_ .-]*pixel|25mp|full[_ .-]*bin|limited[_ .-]*qbc|capture_turboraw|apsSensorMode' "$f" 2>/dev/null)"
            if [ -n "$hits" ]; then
                echo "===== $f ====="
                echo "$hits"
            fi
        done
    done
} > "$OUT/config_keyword_hits.txt" 2>&1

{
    service list 2>/dev/null | grep -Ei 'camera|ipu|aps'
    getprop | grep -Ei 'camera|sensor|qbc|turbo|remosaic|aps' | head -n 800
    dumpsys media.camera 2>&1
} > "$OUT/camera_service_snapshot.txt"

logcat -c >/dev/null 2>&1
logcat -v threadtime -b main -b system -b crash -b events > "$LIVE" 2>&1 &
LOGCAT_PID=$!
sleep 1
am force-stop com.oplus.camera >/dev/null 2>&1
sleep 1
monkey -p com.oplus.camera -c android.intent.category.LAUNCHER 1 > "$OUT/logs/launch.txt" 2>&1

echo "进入高像素；如有25MP/直出选项切换一次并尝试拍摄，然后切回拍照。采集40秒。"
for i in $(seq 1 40); do
    PID="$(pidof com.oplus.camera 2>/dev/null)"
    if [ -n "$PID" ]; then
        RSS="$(grep '^VmRSS:' /proc/$PID/status 2>/dev/null | head -1)"
        STATE="$(grep '^State:' /proc/$PID/status 2>/dev/null | head -1)"
        echo "t=${i}s pid=$PID $STATE $RSS" >> "$OUT/logs/pid_trace.txt"
    else
        echo "t=${i}s pid=NOT_RUNNING" >> "$OUT/logs/pid_trace.txt"
    fi
    sleep 1
done
cleanup

logcat -d -v threadtime -b main -b system -b crash -b events > "$OUT/logs/logcat_after.txt" 2>&1
grep -Ei 'CameraUnit|CameraUnitClient|IPUFeatures|IPU|APS|sensor[_ .-]*mode|SensorMode|pApsSensorMode|remosaic|turboraw|QBC|limited_qbc|full.bining|HighPixel|25MP|Camera25Mp|SuperRaw|RAWMAX|JPGMAX|OPCameraPro|FeatureTable|Protobuf|FATAL EXCEPTION|Fatal signal|SIGABRT|SIGSEGV|ANR|Watchdog' "$OUT/logs/logcat_after.txt" > "$OUT/logs/runtime_filtered.txt" 2>&1
dumpsys activity exit-info com.oplus.camera > "$OUT/logs/activity_exit_info.txt" 2>&1

{
    echo "=== Automatic hints ==="
    grep -q 'pApsSensorMode is NULL' "$OUT/logs/runtime_filtered.txt" && echo "[!] APS remosaic sensor-mode is still NULL."
    grep -Eqi 'Camera25Mp|25MP|turboraw|QBC' "$OUT/logs/runtime_filtered.txt" && echo "[*] 25MP/TurboRAW/QBC path observed."
    grep -q 'camera_unit_config is null' "$OUT/logs/runtime_filtered.txt" && echo "[!] OPCameraPro still uses obsolete camera_unit_config lookup."
} >> "$SUMMARY"

ARCHIVE="${OUT}.tar.gz"
tar -czf "$ARCHIVE" -C "$(dirname "$OUT")" "$(basename "$OUT")" 2>/dev/null
echo "Archive: $ARCHIVE"
