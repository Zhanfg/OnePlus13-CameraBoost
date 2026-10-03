package dev.cameraboost.oplus10bit;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedHashMap;
import java.util.Map;

final class OplusConfigPatcher {
    static final String TAG_10BIT_HEIC = "com.oplus.10bits.heic.encode.support";
    static final String TAG_HEIF_LIVE_PHOTO = "com.oplus.camera.heif.support.livephoto";
    static final String TAG_10BIT_LIVE_PHOTO = "com.oplus.livephoto.support.10bit";
    static final String TAG_VIDEO_10BIT = "com.oplus.feature.video.10bit.support";

    private OplusConfigPatcher() {}

    static PatchResult inspectAndPatch(
            String original,
            boolean enable10BitHeic,
            boolean enable10BitLivePhoto
    ) {
        if (original == null || original.trim().isEmpty()) {
            return PatchResult.failure(original, "empty-config");
        }

        try {
            Parsed parsed = Parsed.parse(original);
            Map<String, String> before = inspect(parsed.array);

            boolean changed = false;
            if (enable10BitHeic) {
                changed |= upsert(
                        parsed.array,
                        TAG_10BIT_HEIC,
                        new OplusFeatureCatalog.OverrideValue("Byte", "1", "1")
                );
            }

            if (enable10BitLivePhoto
                    && ColorOS17CompatResolver.get().supportsLivePhotoStack()) {
                changed |= upsert(
                        parsed.array,
                        TAG_HEIF_LIVE_PHOTO,
                        new OplusFeatureCatalog.OverrideValue("Byte", "1", "1")
                );
                changed |= upsert(
                        parsed.array,
                        TAG_10BIT_LIVE_PHOTO,
                        new OplusFeatureCatalog.OverrideValue("Byte", "1", "1")
                );
            }

            // Apply the same capability policy to the config document that is used by
            // the runtime getter hooks. This avoids UI/APS disagreement.
            ColorOS17CompatResolver resolver = ColorOS17CompatResolver.get();
            for (Map.Entry<String, OplusFeatureCatalog.OverrideValue> entry
                    : OplusFeatureCatalog.allOverrides().entrySet()) {
                OplusFeatureCatalog.OverrideValue override =
                        OplusFeatureCatalog.overrideFor(entry.getKey(), resolver);
                if (override != null) {
                    changed |= upsert(parsed.array, entry.getKey(), override);
                }
            }

            Map<String, String> after = inspect(parsed.array);
            return new PatchResult(
                    true,
                    changed,
                    parsed.render(),
                    before,
                    after,
                    null
            );
        } catch (Throwable t) {
            return PatchResult.failure(
                    original,
                    t.getClass().getSimpleName() + ": " + t.getMessage()
            );
        }
    }

    private static Map<String, String> inspect(JSONArray array) {
        Map<String, String> result = new LinkedHashMap<>();
        result.put(TAG_10BIT_HEIC, null);
        result.put(TAG_HEIF_LIVE_PHOTO, null);
        result.put(TAG_10BIT_LIVE_PHOTO, null);
        result.put(TAG_VIDEO_10BIT, null);
        result.put("com.oplus.ai.capture.guide.support", null);
        result.put("com.oplus.ai.composition.enable", null);
        result.put("com.oplus.feature.aicomposition.realscene.support", null);
        result.put("com.oplus.feature.aicomposition.inspiration.support", null);
        result.put("com.oplus.camera.livephoto.support", null);
        result.put("com.oplus.feature.master.mode.version", null);
        result.put("com.oplus.xpan.mode.version", null);

        for (int i = 0; i < array.length(); i++) {
            JSONObject obj = array.optJSONObject(i);
            if (obj == null) {
                continue;
            }

            String tag = obj.optString("VendorTag", "");
            if (result.containsKey(tag)) {
                result.put(tag, obj.optString("Value", ""));
            }
        }
        return result;
    }

    private static boolean upsert(
            JSONArray array,
            String vendorTag,
            OplusFeatureCatalog.OverrideValue override
    ) throws JSONException {
        for (int i = 0; i < array.length(); i++) {
            JSONObject obj = array.optJSONObject(i);
            if (obj == null) {
                continue;
            }

            if (vendorTag.equals(obj.optString("VendorTag", ""))) {
                String oldValue = obj.optString("Value", "");
                String oldType = obj.optString("Type", "");
                String oldCount = obj.optString("Count", "");

                obj.put("Type", override.type);
                obj.put("Count", override.count);
                obj.put("Value", override.value);

                return !override.value.equals(oldValue)
                        || !override.type.equals(oldType)
                        || !override.count.equals(oldCount);
            }
        }

        JSONObject added = new JSONObject();
        added.put("VendorTag", vendorTag);
        added.put("Type", override.type);
        added.put("Count", override.count);
        added.put("Value", override.value);
        array.put(added);
        return true;
    }

    static final class PatchResult {
        final boolean parsed;
        final boolean changed;
        final String output;
        final Map<String, String> before;
        final Map<String, String> after;
        final String error;

        PatchResult(
                boolean parsed,
                boolean changed,
                String output,
                Map<String, String> before,
                Map<String, String> after,
                String error
        ) {
            this.parsed = parsed;
            this.changed = changed;
            this.output = output;
            this.before = before;
            this.after = after;
            this.error = error;
        }

        static PatchResult failure(String original, String error) {
            return new PatchResult(
                    false,
                    false,
                    original,
                    new LinkedHashMap<>(),
                    new LinkedHashMap<>(),
                    error
            );
        }
    }

    private static final class Parsed {
        final JSONObject rootObject;
        final JSONArray array;

        private Parsed(JSONObject rootObject, JSONArray array) {
            this.rootObject = rootObject;
            this.array = array;
        }

        static Parsed parse(String text) throws JSONException {
            String trimmed = text.trim();
            if (trimmed.startsWith("[")) {
                return new Parsed(null, new JSONArray(trimmed));
            }

            JSONObject root = new JSONObject(trimmed);
            JSONArray fileData = root.optJSONArray("file_data");
            if (fileData == null) {
                throw new JSONException("missing file_data");
            }
            return new Parsed(root, fileData);
        }

        String render() throws JSONException {
            if (rootObject == null) {
                return array.toString();
            }
            rootObject.put("file_data", array);
            return rootObject.toString();
        }
    }
}
