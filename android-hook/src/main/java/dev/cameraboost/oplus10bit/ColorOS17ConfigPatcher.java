package dev.cameraboost.oplus10bit;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;

final class ColorOS17ConfigPatcher {
    static final class Result {
        final boolean parsed;
        final boolean changed;
        final int enabledExisting;
        final int synthesized;
        final String output;
        final String error;

        Result(boolean parsed, boolean changed, int enabledExisting, int synthesized,
               String output, String error) {
            this.parsed = parsed;
            this.changed = changed;
            this.enabledExisting = enabledExisting;
            this.synthesized = synthesized;
            this.output = output;
            this.error = error;
        }
    }

    private ColorOS17ConfigPatcher() {}

    private static boolean isBooleanLikeType(String type) {
        return "Byte".equalsIgnoreCase(type)
                || "Int32".equalsIgnoreCase(type)
                || "Int".equalsIgnoreCase(type)
                || "Boolean".equalsIgnoreCase(type);
    }

    static Result patch(String original, OplusCapabilityResolver resolver) {
        if (original == null || original.trim().isEmpty()) {
            return new Result(false, false, 0, 0, original, "empty config");
        }

        try {
            boolean rootIsArray = original.trim().startsWith("[");
            JSONObject rootObject = rootIsArray ? null : new JSONObject(original);
            JSONArray rows = rootIsArray
                    ? new JSONArray(original)
                    : rootObject.optJSONArray("file_data");

            if (rows == null) {
                return new Result(false, false, 0, 0, original,
                        "not an OPlus VendorTag document");
            }

            Set<String> existing = new HashSet<>();
            int enabledExisting = 0;
            int synthesized = 0;
            boolean changed = false;

            for (int i = 0; i < rows.length(); i++) {
                JSONObject row = rows.optJSONObject(i);
                if (row == null) {
                    continue;
                }
                String key = row.optString("VendorTag", "");
                if (key.isEmpty()) {
                    continue;
                }
                existing.add(key);
                OplusFeatureGateRegistry.markHostAdvertised(key);

                OplusFeatureGateRegistry.GateSpec spec = OplusFeatureGateRegistry.get(key);
                if (spec == null) {
                    if (BuildConfig.ENABLE_EXPERIMENTAL_ALL
                            && resolver.shouldForceBoolean(key)
                            && isBooleanLikeType(row.optString("Type", ""))) {
                        String before = row.optString("Value", "");
                        if (!"1".equals(before)) {
                            row.put("Value", "1");
                            if (row.has("DefaultValue")) row.put("DefaultValue", "1");
                            changed = true;
                            enabledExisting++;
                        }
                    }
                    continue;
                }
                if (!OplusFeatureGateRegistry.shouldForce(key, resolver)) {
                    continue;
                }

                String declaredType = row.optString("Type", spec.type);
                String wanted = OplusFeatureGateRegistry.enabledValueForDeclaredType(spec, declaredType);
                String before = row.optString("Value", "");
                if (!wanted.equals(before)) {
                    row.put("Value", wanted);
                    if (row.has("DefaultValue")) {
                        row.put("DefaultValue", wanted);
                    }
                    changed = true;
                    enabledExisting++;
                }
            }

            for (OplusFeatureGateRegistry.GateSpec spec : OplusFeatureGateRegistry.specs()) {
                if (existing.contains(spec.key)
                        || !OplusFeatureGateRegistry.shouldSynthesize(spec, resolver)) {
                    continue;
                }
                JSONObject row = new JSONObject();
                row.put("VendorTag", spec.key);
                row.put("Type", spec.type);
                row.put("Count", spec.count);
                row.put("Value", spec.value);
                rows.put(row);
                OplusFeatureGateRegistry.markHostAdvertised(spec.key);
                existing.add(spec.key);
                synthesized++;
                changed = true;
            }

            if (BuildConfig.ENABLE_EXPERIMENTAL_ALL) {
                for (String key : resolver.experimentalSynthesizableGates()) {
                    if (existing.contains(key) || OplusFeatureGateRegistry.knows(key)) {
                        continue;
                    }
                    JSONObject row = new JSONObject();
                    row.put("VendorTag", key);
                    row.put("Type", "Byte");
                    row.put("Count", "1");
                    row.put("Value", "1");
                    rows.put(row);
                    existing.add(key);
                    synthesized++;
                    changed = true;
                }
            }

            String output;
            if (rootIsArray) {
                output = rows.toString();
            } else {
                rootObject.put("file_data", rows);
                output = rootObject.toString();
            }
            return new Result(true, changed, enabledExisting, synthesized, output, null);
        } catch (Throwable t) {
            return new Result(false, false, 0, 0, original,
                    t.getClass().getSimpleName() + ": " + t.getMessage());
        }
    }
}
