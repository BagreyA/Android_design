import com.android.tools.build.bundletool.flags.Flag.path

plugins {
    id("org.jetbrains.kotlin.plugin.serialization") version "1.7.20"
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    id("com.chaquo.python")
}

android {
    namespace = "com.example.login"
    compileSdk = 34

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }

    defaultConfig {
        applicationId = "com.example.login"
        minSdk = 24
        targetSdk = 34
        versionCode = 23
        versionName = "2.3.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        ndk {
            abiFilters += listOf("arm64-v8a", "x86_64", "armeabi-v7a")
        }

        externalNativeBuild {
            cmake {
                version = "3.18.1"
                path ("src/main/jni/CMakeLists.txt")
            }
        }
    }

    buildFeatures{
        dataBinding = true
        viewBinding = true
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
        dataBinding = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    sourceSets {
        getByName("main") {
            jniLibs.srcDirs("src/main/jniLibs")
        }
    }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-reflect:2.0.0")
    implementation("com.github.maddevsio:mad-location-manager:0.1.16")
    implementation ("fr.bmartel:jspeedtest:1.32.1")
    implementation("androidx.test.ext:junit-ktx:1.2.1")
    implementation("com.google.ar:core:1.44.0")
    implementation("androidx.test:runner:1.5.2")
    implementation("androidx.palette:palette-ktx:1.0.0")
    implementation("org.osmdroid:osmdroid-android:6.1.13")
    implementation("com.google.maps.android:maps-compose:2.11.4")
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("androidx.compose.material:material:1.6.7")
    implementation("com.google.android.gms:play-services-location:21.2.0")
    implementation("com.google.maps.android:android-maps-utils:3.8.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.compose.ui:ui-tooling:1.6.7")
    implementation("androidx.compose.material:material:1.6.7")
    implementation("androidx.compose.foundation:foundation:1.6.7")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.0")
    implementation(platform("androidx.compose:compose-bom:2024.05.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.graphics:graphics-core:1.0.0")
    implementation("androidx.work:work-runtime-ktx:2.9.1")
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.work:work-runtime-ktx:2.8.1")
    implementation ("com.jakewharton.timber:timber:5.0.1")
    implementation ("com.google.guava:guava:32.1.3-android")

    val libsuVersion = "6.0.0"
    implementation("com.github.topjohnwu.libsu:core:$libsuVersion")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:4.8.0")
    testImplementation("io.mockk:mockk:1.13.12")
    testImplementation("org.robolectric:robolectric:4.13")

    androidTestImplementation (platform("androidx.compose:compose-bom:2024.05.00"))
    androidTestImplementation ("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation ("androidx.test.ext:junit:1.1.5")
    debugImplementation ("androidx.compose.ui:ui-test-manifest")

    testImplementation("org.robolectric:robolectric:4.9")

}