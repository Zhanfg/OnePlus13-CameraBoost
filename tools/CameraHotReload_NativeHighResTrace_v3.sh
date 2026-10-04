#!/system/bin/sh
# Camera 7.013.30 native High Resolution menu + zoom trace
# No args. Soft reload only; no app data/cache clear.

SELF="$(readlink -f "$0" 2>/dev/null)"
[ -n "$SELF" ] || SELF="$0"

echo "========================================"
echo " Native High Resolution + Zoom Trace v3"
echo " $(date)"
echo "========================================"

if [ "$(id -u 2>/dev/null)" != "0" ]; then
    echo "[*] Requesting root..."
    exec su -c "sh '$SELF'"
fi

TS="$(date +%Y%m%d_%H%M%S)"
OUT="/sdcard/Download/NativeHighRes_ZoomTrace_${TS}"
mkdir -p "$OUT" || exit 1

PKG="com.oplus.camera"

echo "[1/5] Clear logcat"
logcat -c >/dev/null 2>&1

echo "[2/5] Start capture"
logcat -v threadtime -b main -b system -b crash -b events > "$OUT/logcat_live.txt" 2>&1 &
LPID=$!

echo "[3/5] Soft reload Camera"
OLDPID="$(pidof "$PKG" 2>/dev/null)"
if [ -n "$OLDPID" ]; then
    kill -TERM $OLDPID 2>/dev/null
    i=0
    while [ "$i" -lt 25 ] && [ -n "$(pidof "$PKG" 2>/dev/null)" ]; do
        sleep 0.08
        i=$((i+1))
    done
    if [ -n "$(pidof "$PKG" 2>/dev/null)" ]; then
        kill -KILL $(pidof "$PKG" 2>/dev/null) 2>/dev/null
        sleep 0.15
    fi
fi

monkey -p "$PKG" -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1
sleep 0.6
echo "[OK] Camera pid=$(pidof "$PKG" 2>/dev/null)"

echo
echo "============================================================"
echo " 接下来 35 秒请按顺序操作："
echo "  1. 保持普通【拍照】模式"
echo "  2. 看顶部/菜单里【高清】是否恢复"
echo "  3. 如果有，打开一次，再关闭一次"
echo "  4. 测试 0.6× → 1× → 3× → 1×"
echo "  5. 最后 1× 普通拍一张"
echo "============================================================"
echo

i=1
while [ "$i" -le 35 ]; do
    PID="$(pidof "$PKG" 2>/dev/null)"
    if [ -n "$PID" ]; then
        STATE="$(grep '^State:' /proc/$PID/status 2>/dev/null | head -1)"
        RSS="$(grep '^VmRSS:' /proc/$PID/status 2>/dev/null | head -1)"
        echo "t=${i}s pid=$PID $STATE $RSS" >> "$OUT/pid_trace.txt"
    else
        echo "t=${i}s pid=NOT_RUNNING" >> "$OUT/pid_trace.txt"
    fi
    [ $((i % 5)) -eq 0 ] && echo "[*] ${i}/35 秒"
    sleep 1
    i=$((i+1))
done

kill "$LPID" 2>/dev/null
wait "$LPID" 2>/dev/null

echo "[4/5] Filter logs"
grep -aEi 'CameraBoost17|NativeHighResolution|NativeHighRes|HighResolutionPresenter|pref_camera_high_resolution_key|camera_setting_menu_high_resolution_item|camera_setting_submenu_high_res|standard_high|standard|com\.oplus\.feature\.high\.definition\.support|com\.oplus\.pre\.high\.resolution\.support|com\.oplus\.super\.resolution\.picturesize|com\.oplus\.high\.picturesize|Direct25MP TRACE|original\.zoomRatio|zoomRatio|zoom_value|targetCameraId|cameraId:|rear_sat|rear_main|rear_wide|rear_tele|SAT|QBC|TurboRaw|turboraw|is_high_pixel|common_50m_capture_cnt|FATAL EXCEPTION|Fatal signal|SIGABRT|SIGSEGV' "$OUT/logcat_live.txt" > "$OUT/filtered.txt" 2>/dev/null

mkdir -p "$OUT/lsposed"
for d in /data/adb/lspd/log /data/adb/lspd/log/verbose /data/adb/lsposed/log; do
    [ -d "$d" ] || continue
    ls -t "$d"/* 2>/dev/null | head -n 8 | while IFS= read -r f; do
        [ -f "$f" ] || continue
        dst="$OUT/lsposed/$(echo "$f" | tr '/' '_')"
        tail -n 7000 "$f" | grep -aEi         'CameraBoost17|NativeHighResolution|NativeHighRes|HighResolutionPresenter|pref_camera_high_resolution_key|Direct25MP|zoomRatio|QBC|TurboRaw|turboraw'         > "$dst" 2>/dev/null
    done
done

{
    echo "time=$(date)"
    echo "camera_pid=$(pidof "$PKG" 2>/dev/null)"
    echo
    echo "=== Native menu / gate evidence ==="
    grep -aEi       'HighResolution native gates|HighResolution native menu|NativeHighResolutionUI|HighResolutionPresenter|pref_camera_high_resolution_key|standard_high'       "$OUT/filtered.txt" "$OUT"/lsposed/* 2>/dev/null | tail -n 300
    echo
    echo "=== Zoom / physical camera evidence ==="
    grep -aEi       'original\.zoomRatio|zoomRatio|zoom_value|targetCameraId|cameraId:|rear_sat|rear_main|rear_wide|rear_tele'       "$OUT/filtered.txt" | tail -n 300
    echo
    echo "=== 25MP / capture evidence ==="
    grep -aEi       'Direct25MP|QBC|TurboRaw|turboraw|is_high_pixel|common_50m_capture_cnt|high\.picturesize'       "$OUT/filtered.txt" "$OUT"/lsposed/* 2>/dev/null | tail -n 300
} > "$OUT/summary.txt"

echo "[5/5] Pack"
ARCHIVE="${OUT}.tar.gz"
tar -czf "$ARCHIVE" -C "$(dirname "$OUT")" "$(basename "$OUT")" 2>/dev/null

echo
echo "完成：$ARCHIVE"
