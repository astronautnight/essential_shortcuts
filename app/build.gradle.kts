plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.androidx.navigation.safeargs) // Use this instead
}

android {
    namespace = "com.essential.shortcuts"
    compileSdk = 37
    defaultConfig {
        applicationId = "com.essential.shortcuts"
        minSdk = 24
        targetSdk = 37

        versionCode = 15
        versionName = "1.82"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters += listOf("arm64-v8a")
        }
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
    kotlinOptions {
        jvmTarget = "11"
    }

    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
        resources {
            pickFirsts += setOf(
                "META-INF/NOTICE.md",
                "META-INF/LICENSE.md",
                "META-INF/NOTICE",
                "META-INF/LICENSE",
                "META-INF/DEPENDENCIES"
            )
        }
    }
}

dependencies {
    implementation(libs.androidx.compose.material.icons.extended)
    
    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat) 
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // Navigation
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    
    // ViewModel and LiveData
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)

    // UI Components
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.cardview)
    implementation(libs.blurview)

    // QR Code
    implementation(libs.zxing.android.embedded)

    // Animation physics
    implementation(libs.androidx.dynamicanimation)

    // CameraX for live scanner
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.guava)

    // ML Kit Barcode Scanning (bundled – works offline)
    implementation(libs.google.mlkit.barcode.scanning)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // JavaMail — send email directly via SMTP (no external app needed)
    implementation("com.sun.mail:android-mail:1.6.7")
    implementation("com.sun.mail:android-activation:1.6.7")

    // PDFBox for PDF manipulation
    implementation(libs.pdfbox.android)

    // FFmpeg for video processing
    implementation("com.github.microshow:RxFFmpeg:4.9.0")
}
