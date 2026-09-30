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
        versionCode = 2
        versionName = "0.1.1"

        // Static evidence gates. These remain false until the corresponding
        // OnePlus 13 path is proven from official/target artifacts.
        buildConfigField("boolean", "STATIC_OPLUS_GATE_VERIFIED", "false")
        buildConfigField("boolean", "STATIC_HAL_TEN_BIT_PATH_VERIFIED", "false")
        buildConfigField("boolean", "STATIC_HEIF_ENCODER_10BIT_VERIFIED", "false")
        buildConfigField("boolean", "STATIC_COLOR_METADATA_VERIFIED", "false")
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        getByName("debug") {
            buildConfigField("boolean", "ENABLE_10BIT_HEIC", "false")
            buildConfigField("boolean", "ENABLE_10BIT_LIVE_PHOTO", "false")
        }

        create("probe") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".probe"
            versionNameSuffix = "-probe"
            buildConfigField("boolean", "ENABLE_10BIT_HEIC", "false")
            buildConfigField("boolean", "ENABLE_10BIT_LIVE_PHOTO", "false")
            signingConfig = signingConfigs.getByName("debug")
        }

        create("enable10bit") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".enable10bit"
            versionNameSuffix = "-enable10bit"
            buildConfigField("boolean", "ENABLE_10BIT_HEIC", "true")
            buildConfigField("boolean", "ENABLE_10BIT_LIVE_PHOTO", "false")
            signingConfig = signingConfigs.getByName("debug")
        }

        getByName("release") {
            buildConfigField("boolean", "ENABLE_10BIT_HEIC", "false")
            buildConfigField("boolean", "ENABLE_10BIT_LIVE_PHOTO", "false")
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
    testImplementation("junit:junit:4.13.2")
}
