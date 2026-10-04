#!/system/bin/sh
# Camera one-tap soft reload for LSPosed / OPCameraPro testing.
# No arguments. Preserves Camera app data/settings and avoids Android force-stop state.

SELF="$(readlink -f "$0" 2>/dev/null)"
[ -n "$SELF" ] || SELF="$0"

echo "========================================"
echo " Camera Soft Reload"
echo " $(date)"
echo "========================================"

if [ "$(id -u 2>/dev/null)" != "0" ]; then
    echo "[*] Requesting root..."
    exec su -c "sh '$SELF'"
fi

PKG="com.oplus.camera"
OLDPID="$(pidof "$PKG" 2>/dev/null)"

if [ -n "$OLDPID" ]; then
    echo "[*] Soft-stopping Camera pid=$OLDPID (SIGTERM)"
    kill -TERM $OLDPID 2>/dev/null

    i=0
    while [ "$i" -lt 25 ]; do
        NEWPID="$(pidof "$PKG" 2>/dev/null)"
        [ -z "$NEWPID" ] && break
        sleep 0.08
        i=$((i + 1))
    done

    if [ -n "$(pidof "$PKG" 2>/dev/null)" ]; then
        echo "[*] Camera did not exit after TERM; using SIGKILL fallback"
        kill -KILL $(pidof "$PKG" 2>/dev/null) 2>/dev/null
        sleep 0.15
    fi
else
    echo "[*] Camera process is not running"
fi

echo "[*] Relaunching Camera..."
monkey -p "$PKG" -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1
sleep 0.35

PID="$(pidof "$PKG" 2>/dev/null)"
if [ -n "$PID" ]; then
    echo "[OK] Camera reloaded. new pid=$PID"
else
    am start -a android.media.action.STILL_IMAGE_CAMERA -p "$PKG" >/dev/null 2>&1
    sleep 0.35
    PID="$(pidof "$PKG" 2>/dev/null)"
    if [ -n "$PID" ]; then
        echo "[OK] Camera reloaded via intent. new pid=$PID"
    else
        echo "[!] Camera did not relaunch. Open it manually once."
        exit 2
    fi
fi

echo "[*] No app data/cache was cleared."
echo "[*] LSPosed hooks will be loaded in this new Camera process."
