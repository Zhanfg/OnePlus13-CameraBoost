plugins {
    id("com.android.application")
}

android {
    namespace = "dev.cameraboost.oplus10bit"
    compileSdk = 36

    defaultConfig {
        applicationId = "dev.cameraboost.oplus10bit"
        minSdk = 30
        targetSdk = 36
        versionCode = 4
        versionName = "0.4.0-hotfix2"
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        getByName("debug") {
            buildConfigField("boolean", "ENABLE_10BIT_HEIC", "false")
            buildConfigField("boolean", "ENABLE_10BIT_LIVE_PHOTO", "false")
            buildConfigField("boolean", "ENABLE_COLOROS17_COMPAT", "false")
            buildConfigField("boolean", "ENABLE_EXPERIMENTAL_ALL", "false")
        }

        create("probe") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".probe"
            versionNameSuffix = "-probe"
            buildConfigField("boolean", "ENABLE_10BIT_HEIC", "false")
            buildConfigField("boolean", "ENABLE_10BIT_LIVE_PHOTO", "false")
            buildConfigField("boolean", "ENABLE_COLOROS17_COMPAT", "false")
            buildConfigField("boolean", "ENABLE_EXPERIMENTAL_ALL", "false")
            signingConfig = signingConfigs.getByName("debug")
        }

        create("enable10bit") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".enable10bit"
            versionNameSuffix = "-enable10bit"
            buildConfigField("boolean", "ENABLE_10BIT_HEIC", "true")
            buildConfigField("boolean", "ENABLE_10BIT_LIVE_PHOTO", "false")
            buildConfigField("boolean", "ENABLE_COLOROS17_COMPAT", "false")
            buildConfigField("boolean", "ENABLE_EXPERIMENTAL_ALL", "false")
            signingConfig = signingConfigs.getByName("debug")
        }

        create("coloros17") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".coloros17"
            versionNameSuffix = "-coloros17-compat"
            buildConfigField("boolean", "ENABLE_10BIT_HEIC", "false")
            buildConfigField("boolean", "ENABLE_10BIT_LIVE_PHOTO", "false")
            buildConfigField("boolean", "ENABLE_COLOROS17_COMPAT", "true")
            buildConfigField("boolean", "ENABLE_EXPERIMENTAL_ALL", "false")
            signingConfig = signingConfigs.getByName("debug")
        }

        create("coloros17All") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".coloros17all"
            versionNameSuffix = "-coloros17-all"
            buildConfigField("boolean", "ENABLE_10BIT_HEIC", "true")
            buildConfigField("boolean", "ENABLE_10BIT_LIVE_PHOTO", "true")
            buildConfigField("boolean", "ENABLE_COLOROS17_COMPAT", "true")
            buildConfigField("boolean", "ENABLE_EXPERIMENTAL_ALL", "true")
            signingConfig = signingConfigs.getByName("debug")
        }

        getByName("release") {
            buildConfigField("boolean", "ENABLE_10BIT_HEIC", "false")
            buildConfigField("boolean", "ENABLE_10BIT_LIVE_PHOTO", "false")
            buildConfigField("boolean", "ENABLE_COLOROS17_COMPAT", "false")
            buildConfigField("boolean", "ENABLE_EXPERIMENTAL_ALL", "false")
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        abortOnError = true
    }
}

dependencies {
    compileOnly("de.robv.android.xposed:api:82")
}
