package dev.cameraboost.oplus10bit;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

final class OplusUnlockCatalog {
    private static final Set<String> SAFE_GATES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "com.oplus.ai.capture.guide.support",
            "com.oplus.feature.aicomposition.realscene.support",
            "com.oplus.feature.aicomposition.inspiration.support",
            "com.oplus.camera.livephoto.support",
            "com.oplus.camera.feature.live_photo",
            "com.oplus.livephoto.support.heif",
            "com.oplus.camera.livephoto.enable.eis",
            "com.oplus.feature.high.pixel.support",
            "com.oplus.high.resolution.support",
            "com.oplus.highpicture.pro.support",
            "com.oplus.feature.master.mode.version",
            "com.oplus.feature.master.mode.guide",
            "com.oplus.feature.xpan.mode.support",
            "com.oplus.xpan.all.camera.support",
            "com.oplus.xpan.more.tele.zoom.support",
            "com.oplus.support.grand.tour.filter",
            "com.oplus.camera.feature.gr_photo",
            "com.oplus.camera.preview.hdr.support",
            "com.oplus.feature.ultra.hdr.enable",
            "com.oplus.feature.photo.10bit.enable",
            "com.oplus.10bits.heic.encode.support",
            "com.oplus.camera.feature.ten_bit",
            "com.oplus.portrait.rear.flash.support",
            "com.oplus.motion.capture.support",
            "com.oplus.motion.capture.sat.support",
            "com.oplus.feature.macro.mode.support",
            "com.oplus.camera.mf.tele.marco.support",
            "com.oplus.feature.super.text.support",
            "com.oplus.camera.docscan.mode.support",
            "com.oplus.feature.ai.high.pixel.enable",
            "com.oplus.ai.hd.gan.zoom.support",
            "com.oplus.feature.aihd.sdsr.enable",
            "com.oplus.more.tele.zoom.support",
            "com.oplus.super.tele.zoom.support",
            "com.oplus.feature.global.ev.enable",
            "com.oplus.feature.street.mode.support",
            "com.oplus.feature.street.raw.support",
            "com.oplus.video.lock.lens.support",
            "com.oplus.video.sound.focus.support",
            "com.oplus.feature.video.10bit.support",
            "com.oplus.feature.video.dv.support",
            "com.oplus.feature.video.4k.support"
    )));

    private static final String[] NEGATIVE_MARKERS = {
            ".not.support", ".disable", ".disabled", ".hide.", ".close.",
            ".old.", ".preversion", "fallback", "thermal", "unsupported"
    };

    private OplusUnlockCatalog() {}

    static boolean shouldForceBoolean(String key) {
        if (key == null || key.isEmpty()) return false;
        if (SAFE_GATES.contains(key)) return true;
        if (!BuildConfig.ENABLE_EXPERIMENTAL_ALL) return false;

        String k = key.toLowerCase(Locale.ROOT);
        if (!k.startsWith("com.oplus.")) return false;
        for (String marker : NEGATIVE_MARKERS) {
            if (k.contains(marker)) return false;
        }

        return k.contains(".support")
                || k.contains(".supported")
                || k.endsWith(".enable")
                || k.contains(".enable.")
                || k.startsWith("com.oplus.camera.feature.");
    }

    static Set<String> safeGates() {
        return SAFE_GATES;
    }
}
