package dev.cameraboost.oplus10bit;

import de.robv.android.xposed.XposedHelpers;

final class RuntimeArchitecture {
    final boolean modernAiComposition;
    final boolean modernAiCompositionHelper;
    final boolean legacyAiCaptureGuide;
    final boolean livePhotoPipeline;
    final boolean ipuFilterGroupManager;
    final boolean appFilterGroupManager;
    final boolean filterGroupManager;
    final boolean protobufFeatureTable;
    final boolean configFeatureImpl;
    final boolean modernCameraConfig;

    private RuntimeArchitecture(
            boolean modernAiComposition,
            boolean modernAiCompositionHelper,
            boolean legacyAiCaptureGuide,
            boolean livePhotoPipeline,
            boolean ipuFilterGroupManager,
            boolean appFilterGroupManager,
            boolean protobufFeatureTable,
            boolean configFeatureImpl,
            boolean modernCameraConfig
    ) {
        this.modernAiComposition = modernAiComposition;
        this.modernAiCompositionHelper = modernAiCompositionHelper;
        this.legacyAiCaptureGuide = legacyAiCaptureGuide;
        this.livePhotoPipeline = livePhotoPipeline;
        this.ipuFilterGroupManager = ipuFilterGroupManager;
        this.appFilterGroupManager = appFilterGroupManager;
        this.filterGroupManager = ipuFilterGroupManager || appFilterGroupManager;
        this.protobufFeatureTable = protobufFeatureTable;
        this.configFeatureImpl = configFeatureImpl;
        this.modernCameraConfig = modernCameraConfig;
    }

    static RuntimeArchitecture detect(ClassLoader loader) {
        boolean modernAi = classExists(
                loader,
                "com.oplus.camera.aicomposition.OplusAIComposition"
        ) && classExists(
                loader,
                "com.oplus.camera.feature.aicomposition.state.CompositionStateMachine"
        );

        boolean modernAiHelper = classExists(
                loader,
                "com.oplus.ocs.camera.OplusAICompositionHelper"
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

        boolean ipuFilters = classExists(
                loader,
                "com.oplus.ocs.camera.ipusdk.processunit.filter.list.FilterGroupManager"
        );

        boolean appFilters = classExists(
                loader,
                "com.oplus.camera.filter.FilterGroupManager"
        );

        boolean protobuf = classExists(
                loader,
                "com.oplus.ocs.camera.configure.ProtobufFeatureConfig$FeatureTable"
        );

        boolean configFeature = classExists(
                loader,
                "com.oplus.ocs.camera.configure.ConfigFeatureImpl"
        );

        boolean cameraConfig = classExists(
                loader,
                "com.oplus.camera.configure.CameraConfig"
        );

        return new RuntimeArchitecture(
                modernAi,
                modernAiHelper,
                legacyAi,
                livePhoto,
                ipuFilters,
                appFilters,
                protobuf,
                configFeature,
                cameraConfig
        );
    }

    String summary() {
        return "runtime{modernAI=" + modernAiComposition
                + ", modernAIHelper=" + modernAiCompositionHelper
                + ", legacyAI=" + legacyAiCaptureGuide
                + ", livePhoto=" + livePhotoPipeline
                + ", ipuFilterGroup=" + ipuFilterGroupManager
                + ", appFilterGroup=" + appFilterGroupManager
                + ", protobuf=" + protobufFeatureTable
                + ", legacyConfigFeature=" + configFeatureImpl
                + ", cameraConfig=" + modernCameraConfig
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
