plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.emotikey"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.emotikey"
        minSdk = 29
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
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
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // TensorFlow Lite
    implementation ("org.tensorflow:tensorflow-lite:2.14.0")
    implementation ("org.tensorflow:tensorflow-lite-support:0.4.4")

    // ML Kit Face Detection
    implementation ("com.google.mlkit:face-detection:16.1.7")

    // CameraX
    implementation ("androidx.camera:camera-core:1.3.1")
    implementation ("androidx.camera:camera-camera2:1.3.1")
    implementation ("androidx.camera:camera-lifecycle:1.3.1")
    implementation ("androidx.camera:camera-view:1.3.1")

    // Permission handling
    implementation ("pub.devrel:easypermissions:3.0.0")
}