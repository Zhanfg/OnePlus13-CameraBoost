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
        versionCode = 1
        versionName = "0.1.0"
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        getByName("debug") {
            buildConfigField("boolean", "ENABLE_10BIT_HEIC", "false")
            buildConfigField("boolean", "ENABLE_10BIT_LIVE_PHOTO", "false")
            buildConfigField("boolean", "ENABLE_FULL_UNLOCK", "false")
        }

        create("probe") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".probe"
            versionNameSuffix = "-probe"
            buildConfigField("boolean", "ENABLE_10BIT_HEIC", "false")
            buildConfigField("boolean", "ENABLE_10BIT_LIVE_PHOTO", "false")
            buildConfigField("boolean", "ENABLE_FULL_UNLOCK", "false")
            signingConfig = signingConfigs.getByName("debug")
        }

        create("enable10bit") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".enable10bit"
            versionNameSuffix = "-enable10bit"
            buildConfigField("boolean", "ENABLE_10BIT_HEIC", "true")
            buildConfigField("boolean", "ENABLE_10BIT_LIVE_PHOTO", "false")
            buildConfigField("boolean", "ENABLE_FULL_UNLOCK", "false")
            signingConfig = signingConfigs.getByName("debug")
        }

        create("fullunlock") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".fullunlock"
            versionNameSuffix = "-coloros17-fullunlock"
            buildConfigField("boolean", "ENABLE_10BIT_HEIC", "true")
            buildConfigField("boolean", "ENABLE_10BIT_LIVE_PHOTO", "true")
            buildConfigField("boolean", "ENABLE_FULL_UNLOCK", "true")
            signingConfig = signingConfigs.getByName("debug")
        }

        getByName("release") {
            buildConfigField("boolean", "ENABLE_10BIT_HEIC", "false")
            buildConfigField("boolean", "ENABLE_10BIT_LIVE_PHOTO", "false")
            buildConfigField("boolean", "ENABLE_FULL_UNLOCK", "false")
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
