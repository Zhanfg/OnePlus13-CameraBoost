package dev.cameraboost.oplus10bit;

import java.io.File;
import java.util.Arrays;
import java.util.List;

final class OplusAssetResolver {
    private static final List<String> LUT_ROOTS = Arrays.asList(
            "/odm/etc/camera/meishe_lut",
            "/odm/etc/camera/filters_lut",
            "/vendor/etc/camera/meishe_lut",
            "/vendor/etc/camera/filters_lut",
            "/product/etc/camera/meishe_lut",
            "/product/etc/camera/filters_lut",
            "/system/etc/camera/filters_lut",
            "/etc/camera/filters_lut"
    );

    private static final String[] GR_LUTS = {
            "gr.posi.rgba.bin",
            "gr.nega.rgba.bin",
            "gr.bw.rgba.bin",
            "gr.hi.bw.rgba.bin"
    };

    private static final String[] POSITIVE_LUTS = {
            "positive_normal.bin",
            "positive_master.bin",
            "positive_sdr_gen_a_1.bin",
            "positive_sdr_gen_d_1.bin",
            "positive_hdr_normal_a_1.bin",
            "positive_hdr_normal_d_1.bin",
            "positive_hdr_master_a_1.bin",
            "positive_hdr_master_d_1.bin"
    };

    boolean assetsReady(String featureId) {
        return rootFor(featureId) != null
                || "custom_hasselblad_watermark".equals(featureId);
    }

    String rootFor(String featureId) {
        if ("gr_filters".equals(featureId)) {
            return rootContainingAll(GR_LUTS);
        }
        if ("positive_filters".equals(featureId)) {
            return rootContainingAll(POSITIVE_LUTS);
        }
        return null;
    }

    String firstReadableLutRoot() {
        for (String root : LUT_ROOTS) {
            File dir = new File(root);
            if (dir.isDirectory() && dir.canRead()) {
                return root;
            }
        }
        return null;
    }

    String describe(String featureId) {
        if ("gr_filters".equals(featureId)) {
            return describeFiles(GR_LUTS);
        }
        if ("positive_filters".equals(featureId)) {
            return describeFiles(POSITIVE_LUTS);
        }
        return "no-external-asset-check";
    }

    private String rootContainingAll(String[] names) {
        for (String root : LUT_ROOTS) {
            boolean all = true;
            for (String name : names) {
                if (!new File(root, name).isFile()) {
                    all = false;
                    break;
                }
            }
            if (all) return root;
        }
        return null;
    }

    private String describeFiles(String[] names) {
        StringBuilder out = new StringBuilder();
        for (String root : LUT_ROOTS) {
            int present = 0;
            for (String name : names) {
                if (new File(root, name).isFile()) present++;
            }
            if (present > 0) {
                if (out.length() > 0) out.append("; ");
                out.append(root).append('=').append(present).append('/').append(names.length);
            }
        }
        return out.length() == 0 ? "0/" + names.length : out.toString();
    }
}
