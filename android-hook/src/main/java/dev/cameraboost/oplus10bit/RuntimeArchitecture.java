package dev.cameraboost.oplus10bit;

import de.robv.android.xposed.XposedHelpers;

final class RuntimeArchitecture {
    final boolean modernAiComposition;
    final boolean legacyAiCaptureGuide;
    final boolean livePhotoPipeline;
    final boolean filterGroupManager;
    final boolean protobufFeatureTable;
    final boolean configFeatureImpl;

    private RuntimeArchitecture(
            boolean modernAiComposition,
            boolean legacyAiCaptureGuide,
            boolean livePhotoPipeline,
            boolean filterGroupManager,
            boolean protobufFeatureTable,
            boolean configFeatureImpl
    ) {
        this.modernAiComposition = modernAiComposition;
        this.legacyAiCaptureGuide = legacyAiCaptureGuide;
        this.livePhotoPipeline = livePhotoPipeline;
        this.filterGroupManager = filterGroupManager;
        this.protobufFeatureTable = protobufFeatureTable;
        this.configFeatureImpl = configFeatureImpl;
    }

    static RuntimeArchitecture detect(ClassLoader loader) {
        boolean modernAi = classExists(
                loader,
                "com.oplus.camera.aicomposition.OplusAIComposition"
        ) && classExists(
                loader,
                "com.oplus.camera.feature.aicomposition.state.CompositionStateMachine"
        );

        boolean legacyAi = classExists(
                loader,
                "com.morphoinc.anchortracking.sdk.MorphoInitParams"
        );

        boolean livePhoto = classExists(
                loader,
                "com.oplus.camera.feature.livephoto.io.LivePhotoSavedParams"
        ) || classExists(
                loader,
                "com.oplus.ocs.camera.CameraPictureCallback$CameraPictureImage"
        );

        boolean filters = classExists(
                loader,
                "com.oplus.ocs.camera.ipusdk.processunit.filter.list.FilterGroupManager"
        );

        boolean protobuf = classExists(
                loader,
                "com.oplus.ocs.camera.configure.ProtobufFeatureConfig$FeatureTable"
        );

        boolean configFeature = classExists(
                loader,
                "com.oplus.ocs.camera.configure.ConfigFeatureImpl"
        );

        return new RuntimeArchitecture(
                modernAi,
                legacyAi,
                livePhoto,
                filters,
                protobuf,
                configFeature
        );
    }

    String summary() {
        return "runtime{modernAI=" + modernAiComposition
                + ", legacyAI=" + legacyAiCaptureGuide
                + ", livePhoto=" + livePhotoPipeline
                + ", filterGroup=" + filterGroupManager
                + ", protobuf=" + protobufFeatureTable
                + ", configFeature=" + configFeatureImpl
                + "}";
    }

    private static boolean classExists(ClassLoader loader, String className) {
        try {
            XposedHelpers.findClass(className, loader);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }
}
