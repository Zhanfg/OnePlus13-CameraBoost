package dev.cameraboost.oplus10bit;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

final class OplusConfigPatcher {
    static final String TAG_10BIT_HEIC = "com.oplus.10bits.heic.encode.support";
    static final String TAG_HEIF_LIVE_PHOTO = "com.oplus.camera.heif.support.livephoto";
    static final String TAG_10BIT_LIVE_PHOTO = "com.oplus.livephoto.support.10bit";
    static final String TAG_VIDEO_10BIT = "com.oplus.feature.video.10bit.support";

    private static final String[] INSPECT_TAGS = {
            TAG_10BIT_HEIC,
            TAG_HEIF_LIVE_PHOTO,
            TAG_10BIT_LIVE_PHOTO,
            TAG_VIDEO_10BIT,
            "com.oplus.ai.capture.guide.support",
            "com.oplus.feature.aicomposition.realscene.support",
            "com.oplus.feature.aicomposition.inspiration.support",
            "com.oplus.camera.livephoto.support",
            "com.oplus.camera.video.livephoto.support",
            "com.oplus.feature.master.jpg.max.support",
            "com.oplus.feature.master.hq.raw.support",
            "com.oplus.feature.master.mode.version",
            "com.oplus.high.resolution.support",
            "com.oplus.xpan.all.camera.support",
            "com.oplus.xpan.mode.version"
    };

    private OplusConfigPatcher() {}

    static PatchResult inspectAndPatch(
            String original,
            boolean enable10BitHeic,
            boolean enable10BitLivePhoto,
            boolean enableFullUnlock,
            RuntimeArchitecture runtime
    ) {
        if (original == null || original.trim().isEmpty()) {
            return PatchResult.failure(original, "empty-config");
        }

        try {
            Parsed parsed = Parsed.parse(original);
            Map<String, String> before = inspect(parsed.array);
            Set<String> changedKeys = new LinkedHashSet<>();

            if (enableFullUnlock) {
                patchExistingSoftwareGates(parsed.array, runtime, changedKeys);
                applyScalarOverrides(parsed.array, runtime, changedKeys);
            }

            if (enable10BitHeic
                    && upsertByteFlag(parsed.array, TAG_10BIT_HEIC, "1")) {
                changedKeys.add(TAG_10BIT_HEIC);
            }

            if (enable10BitLivePhoto) {
                if (upsertByteFlag(parsed.array, TAG_HEIF_LIVE_PHOTO, "1")) {
                    changedKeys.add(TAG_HEIF_LIVE_PHOTO);
                }
                if (upsertByteFlag(parsed.array, TAG_10BIT_LIVE_PHOTO, "1")) {
                    changedKeys.add(TAG_10BIT_LIVE_PHOTO);
                }
            }

            Map<String, String> after = inspect(parsed.array);
            return new PatchResult(
                    true,
                    !changedKeys.isEmpty(),
                    parsed.render(),
                    before,
                    after,
                    changedKeys,
                    null
            );
        } catch (Throwable t) {
            return PatchResult.failure(
                    original,
                    t.getClass().getSimpleName() + ": " + t.getMessage()
            );
        }
    }

    private static void patchExistingSoftwareGates(
            JSONArray array,
            RuntimeArchitecture runtime,
            Set<String> changedKeys
    ) throws JSONException {
        for (int i = 0; i < array.length(); i++) {
            JSONObject obj = array.optJSONObject(i);
            if (obj == null) {
                continue;
            }

            String key = obj.optString("VendorTag", "");
            if (!CapabilityKeyPolicy.shouldForceBoolean(key, runtime)) {
                continue;
            }

            String type = obj.optString("Type", "");
            String count = obj.optString("Count", "");
            String oldValue = obj.optString("Value", "");

            // Do not rewrite numeric thresholds/ranges merely because their names
            // contain words such as "support". Only scalar boolean-like records
            // are changed in the serialized config path.
            if (!CapabilityKeyPolicy.isBooleanLikeConfigEntry(type, count, oldValue)) {
                continue;
            }

            if (!"1".equals(oldValue)) {
                obj.put("Value", "1");
                changedKeys.add(key);
            }
        }
    }

    private static void applyScalarOverrides(
            JSONArray array,
            RuntimeArchitecture runtime,
            Set<String> changedKeys
    ) throws JSONException {
        for (Map.Entry<String, CapabilityValuePolicy.OverrideSpec> entry
                : CapabilityValuePolicy.all().entrySet()) {
            String key = entry.getKey();
            CapabilityValuePolicy.OverrideSpec spec =
                    CapabilityValuePolicy.find(key, runtime);
            if (spec == null) {
                continue;
            }

            if (upsertValue(array, key, spec)) {
                changedKeys.add(key);
            }
        }
    }

    private static boolean upsertValue(
            JSONArray array,
            String vendorTag,
            CapabilityValuePolicy.OverrideSpec spec
    ) throws JSONException {
        for (int i = 0; i < array.length(); i++) {
            JSONObject obj = array.optJSONObject(i);
            if (obj == null) {
                continue;
            }

            if (vendorTag.equals(obj.optString("VendorTag", ""))) {
                String oldType = obj.optString("Type", "");
                String oldCount = obj.optString("Count", "");
                String oldValue = obj.optString("Value", "");

                obj.put("Type", spec.type);
                obj.put("Count", spec.count);
                obj.put("Value", spec.value);

                return !spec.type.equalsIgnoreCase(oldType)
                        || !spec.count.equals(oldCount)
                        || !spec.value.equals(oldValue);
            }
        }

        JSONObject added = new JSONObject();
        added.put("VendorTag", vendorTag);
        added.put("Type", spec.type);
        added.put("Count", spec.count);
        added.put("Value", spec.value);
        array.put(added);
        return true;
    }

    private static Map<String, String> inspect(JSONArray array) {
        Map<String, String> result = new LinkedHashMap<>();
        for (String tag : INSPECT_TAGS) {
            result.put(tag, null);
        }

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

    private static boolean upsertByteFlag(JSONArray array, String vendorTag, String value)
            throws JSONException {
        for (int i = 0; i < array.length(); i++) {
            JSONObject obj = array.optJSONObject(i);
            if (obj == null) {
                continue;
            }

            if (vendorTag.equals(obj.optString("VendorTag", ""))) {
                String oldValue = obj.optString("Value", "");
                String oldType = obj.optString("Type", "");
                String oldCount = obj.optString("Count", "");

                obj.put("Type", "Byte");
                obj.put("Count", "1");
                obj.put("Value", value);

                return !value.equals(oldValue)
                        || !"Byte".equals(oldType)
                        || !"1".equals(oldCount);
            }
        }

        JSONObject added = new JSONObject();
        added.put("VendorTag", vendorTag);
        added.put("Type", "Byte");
        added.put("Count", "1");
        added.put("Value", value);
        array.put(added);
        return true;
    }

    static final class PatchResult {
        final boolean parsed;
        final boolean changed;
        final String output;
        final Map<String, String> before;
        final Map<String, String> after;
        final Set<String> changedKeys;
        final String error;

        PatchResult(
                boolean parsed,
                boolean changed,
                String output,
                Map<String, String> before,
                Map<String, String> after,
                Set<String> changedKeys,
                String error
        ) {
            this.parsed = parsed;
            this.changed = changed;
            this.output = output;
            this.before = before;
            this.after = after;
            this.changedKeys = changedKeys;
            this.error = error;
        }

        static PatchResult failure(String original, String error) {
            return new PatchResult(
                    false,
                    false,
                    original,
                    new LinkedHashMap<>(),
                    new LinkedHashMap<>(),
                    new LinkedHashSet<>(),
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
