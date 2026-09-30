plugins {
    id("com.android.application")
}

android {
    namespace = "dev.cameraboost.folderchunker"
    compileSdk = 36

    defaultConfig {
        applicationId = "dev.cameraboost.folderchunker"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        getByName("release") {
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
    testImplementation("junit:junit:4.13.2")
}
