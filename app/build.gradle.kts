plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.services)
}

android {
    namespace = "id.ac.binus.bounty"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "id.ac.binus.bounty"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        buildConfigField("boolean", "USE_FIREBASE_EMULATORS", "false")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        javaCompileOptions {
            annotationProcessorOptions {
                arguments["room.schemaLocation"] = "$projectDir/schemas"
            }
        }
    }

    buildTypes {
        debug {
            buildConfigField(
                "boolean",
                "USE_FIREBASE_EMULATORS",
                (providers.gradleProperty("bountyFirebaseEmulator").orNull == "true").toString()
            )
            // Disposable installation for instrumentation and the M12 restart check.
            if (providers.gradleProperty("bountyIsolatedTest").orNull == "true") {
                applicationIdSuffix = ".testing"
            }
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core)
    implementation(libs.material)
    implementation(libs.androidx.recyclerview)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.glide)
    implementation(libs.androidx.room.runtime)
    annotationProcessor(libs.androidx.room.compiler)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}
